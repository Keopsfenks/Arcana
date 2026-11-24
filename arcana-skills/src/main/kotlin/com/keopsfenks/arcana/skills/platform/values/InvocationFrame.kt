package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.platform.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import java.util.concurrent.ConcurrentLinkedQueue

/** Mutable local variables are isolated from the immutable public invocation snapshot. */
class InvocationFrame(val invocation: SkillInvocation, private val parent: InvocationFrame? = null, private val shared: InvocationFrame? = null) : AutoCloseable {
    internal val locals: ConcurrentHashMap<SkillKey, VariableEntry> = shared?.locals ?: ConcurrentHashMap()
    private val closed = AtomicBoolean(false)
    private val children = ConcurrentHashMap.newKeySet<InvocationFrame>()
    private val work = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    private val callbacks = ConcurrentLinkedQueue<() -> Unit>()
    fun checkActive() {
        if (closed.get()) throw CancellationException("Invocation frame is closed")
        parent?.checkActive()
    }
    fun fork(invocation: SkillInvocation = this.invocation): InvocationFrame {
        checkActive()
        return child(invocation, false).also { it.locals.putAll(locals) }
    }
    /** Sequential nodes copy invocation data while retaining this sequence's local variable scope. */
    fun view(invocation: SkillInvocation): InvocationFrame = child(invocation, true)
    private fun child(invocation: SkillInvocation, share: Boolean): InvocationFrame {
        checkActive()
        val child = InvocationFrame(invocation, this, if (share) this else null)
        children.add(child)
        child.onClose { children.remove(child) }
        if (closed.get()) child.close()
        return child
    }
    fun onClose(callback: () -> Unit) {
        callbacks.add(callback)
        if (closed.get() && callbacks.remove(callback)) callback()
    }
    fun <T> track(task: ScheduledWork<T>): ScheduledWork<T> {
        work.add(task)
        task.future.whenComplete { _, _ -> work.remove(task) }
        if (closed.get()) task.cancel()
        return task
    }
    fun snapshot(bindings: List<VariableBinding>, variables: SkillVariables): SkillInvocation {
        checkActive()
        val values = LinkedHashMap(invocation.data())
        for (binding in bindings) {
            values.remove(binding.alias)
            val value = variables.get(binding.scope, binding.key).orElse(binding.defaultValue)
            if (value != null) {
                require(value.kind() == binding.type) { "bindings.${binding.alias}: expected ${binding.type}" }
                values[binding.alias] = value
            }
        }
        return invocation.derive().replaceData(values).build()
    }
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            children.toList().forEach { it.close() }; children.clear()
            work.toList().forEach { it.cancel() }; work.clear()
            if (shared == null) locals.clear()
            while (true) (callbacks.poll() ?: break).invoke()
        }
    }
}

data class VariableBinding(
    val alias: String,
    val scope: SkillVariableScope,
    val key: SkillKey,
    val type: SkillValue.Kind,
    val defaultValue: SkillValue?,
    val persistent: Boolean
) {
    companion object {
        fun declarations(arguments: com.keopsfenks.arcana.core.skills.SkillArguments, namespace: String): List<VariableBinding> {
            if (!arguments.has("bindings")) return emptyList()
            val section = arguments.section("bindings")
            return java.util.List.copyOf(section.values.map { (alias, raw) ->
                val args = section.child(alias, raw)
                require(alias.matches(Regex("var_[a-z0-9_]{1,100}"))) { "${args.path}: variable aliases must begin var_" }
                args.values.keys.firstOrNull { it !in setOf("scope", "key", "type", "default", "persistent") }?.let { throw args.error(it, "unknown binding option") }
                val scope = try { SkillVariableScope.valueOf(args.string("scope", "invocation").uppercase(java.util.Locale.ROOT)) }
                catch (_: IllegalArgumentException) { throw args.error("scope", "unknown variable scope") }
                val persistent = args.bool("persistent", false)
                require(!persistent || scope != SkillVariableScope.INVOCATION) { "${args.path}.persistent: invocation variables cannot persist" }
                val type = ParameterBindings.kind(args.string("type", "number"), "${args.path}.type")
                VariableBinding(alias, scope, SkillKey.parse(args.string("key", alias.removePrefix("var_")), namespace), type,
                    if (args.has("default")) ParameterBindings.literal(type, args.raw("default"), namespace, "${args.path}.default") else null, persistent)
            })
        }
    }
}
