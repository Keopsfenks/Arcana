package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.platform.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import java.util.concurrent.ConcurrentLinkedQueue

/** Mutable local variables are isolated from the immutable public invocation snapshot. */
class InvocationFrame(val invocation: SkillInvocation, private val parent: InvocationFrame? = null, private val shared: InvocationFrame? = null) : AutoCloseable {
    internal val locals: ConcurrentHashMap<SkillKey, VariableEntry> = shared?.locals ?: ConcurrentHashMap()
    private val terminal = AtomicInteger() // active, completed, explicitly cancelled
    private val cancellationCallbacks = ConcurrentLinkedQueue<() -> Unit>()
    private val children = ConcurrentHashMap.newKeySet<InvocationFrame>()
    private val work = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    private val callbacks = ConcurrentLinkedQueue<() -> Unit>()
    fun descendsFrom(frame: InvocationFrame): Boolean = this === frame || parent?.descendsFrom(frame) == true
    fun checkActive() {
        if ((terminal.get() != 0)) throw CancellationException("Invocation frame is closed")
        parent?.checkActive()
    }
    fun fork(invocation: SkillInvocation = this.invocation): InvocationFrame {
        checkActive()
        return child(invocation, false).also { it.locals.putAll(locals) }
    }
    /** Copies local values without a parent lifetime link, for explicitly adopted managed work. */
    fun detachedCopy(invocation: SkillInvocation = this.invocation): InvocationFrame {
        checkActive()
        return InvocationFrame(invocation).also { it.locals.putAll(locals) }
    }
    /** Sequential nodes copy invocation data while retaining this sequence's local variable scope. */
    fun view(invocation: SkillInvocation): InvocationFrame = child(invocation, true)
    private fun child(invocation: SkillInvocation, share: Boolean): InvocationFrame {
        checkActive()
        val child = InvocationFrame(invocation, this, if (share) this else null)
        children.add(child)
        child.onClose { children.remove(child) }
        if ((terminal.get() != 0)) child.close()
        return child
    }
    fun onClose(callback: () -> Unit) {
        callbacks.add(callback)
        if ((terminal.get() != 0) && callbacks.remove(callback)) callback()
    }
    fun <T> track(task: ScheduledWork<T>): ScheduledWork<T> {
        work.add(task)
        task.future.whenComplete { _, _ -> work.remove(task) }
        if ((terminal.get() != 0)) task.cancel()
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
    fun onCancel(callback: () -> Unit): AutoCloseable {
        cancellationCallbacks.add(callback)
        if (terminal.get() == 2 && cancellationCallbacks.remove(callback)) callback()
        else if (terminal.get() == 1) cancellationCallbacks.remove(callback)
        return AutoCloseable { cancellationCallbacks.remove(callback) }
    }
    /** Normal scope completion retires unadopted work without cancelling detached managed children. */
    fun complete() = retire(1)
    override fun close() = retire(2)
    private fun retire(mode: Int) {
        if (terminal.compareAndSet(0, mode)) {
            if (mode == 2) while (true) (cancellationCallbacks.poll() ?: break).invoke()
            else cancellationCallbacks.clear()
            children.toList().forEach { if (mode == 2) it.close() else it.complete() }; children.clear()
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
            return java.util.List.copyOf(section.values.filterKeys { !it.startsWith("stat_") }.map { (alias, raw) ->
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
