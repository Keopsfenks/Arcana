package com.keopsfenks.arcana.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.compile.*
import com.keopsfenks.arcana.skills.config.SkillLoadResult
import com.keopsfenks.arcana.skills.config.SkillYamlLoader
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.filters.SkillFilters
import com.keopsfenks.arcana.skills.registry.ComponentRegistry
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.core.skills.platform.SkillDefinition
import com.keopsfenks.arcana.skills.triggers.SkillTriggerDispatch
import org.bukkit.plugin.Plugin
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DefaultSkillEngine(
    private val plugin: ArcanaPlugin,
    private val directory: Path,
    private val environment: ExpressionEnvironment,
    val scheduler: SkillScheduler = ArcanaSkillScheduler(plugin),
    clock: () -> Long = System::nanoTime,
    val random: () -> Double = { ThreadLocalRandom.current().nextDouble() }
) : SkillEngine {
    val cooldowns = CooldownStore(clock)
    internal val lifecycle = SkillLifecycleGate()
    private val generalPlatform = DefaultSkillPlatform(this, lifecycle, plugin, directory, environment)
    private val diagnostics = SkillRuntimeDiagnostics({ plugin.logger }, clock)
    val effects = ComponentRegistry<SkillEffect> { invalidate(it) }
    val conditions = ComponentRegistry<SkillCondition> { invalidate(it) }
    val triggers = ComponentRegistry<SkillTrigger> { handle ->
        invalidate(handle)
        listeners.remove(handle)?.let { safelyClose(it) }
    }
    private val listeners = ConcurrentHashMap<SkillRegistration, AutoCloseable>()
    private val current = AtomicReference(SkillSnapshot.EMPTY)
    private val executions = ConcurrentHashMap.newKeySet<ExecutionState>()
    private val closed = AtomicBoolean(false)
    private val runner = SkillRunner(this)
    private val reloadLock = Any()
    var filterCompiler: (SkillArguments) -> CompiledSkillCondition = SkillFilters::compile

    override fun registerEffect(owner: Plugin, effect: SkillEffect): SkillRegistration = lifecycle.admit(owner) {
        effects.register(owner, effect)
    }

    override fun registerCondition(owner: Plugin, condition: SkillCondition): SkillRegistration = lifecycle.admit(owner) {
        conditions.register(owner, condition)
    }

    override fun registerTrigger(owner: Plugin, trigger: SkillTrigger): SkillRegistration {
        val handle = lifecycle.admit(owner) { triggers.register(owner, trigger) }
        try {
            val listener = trigger.listen(object : SkillTriggerDispatch {
                override fun prepare(context: SkillContext): Runnable? {
                    val snapshot = current.get()
                    if (!handle.isActive || closed.get() || snapshot.byTrigger[trigger.id].isNullOrEmpty()) return null
                    return Runnable {
                        if (handle.isActive && !closed.get()) triggerInSnapshot(trigger.id, context, snapshot)
                    }
                }
            })
            listeners[handle] = listener
            if (!handle.isActive) listeners.remove(handle)?.let { safelyClose(it) }
        } catch (error: Throwable) { handle.close(); throw error }
        return handle
    }

    override fun cast(skillId: String, context: SkillContext): CompletionStage<SkillResult> = castInSnapshot(skillId, context, current.get())
    override fun platform() = generalPlatform

    private fun castInSnapshot(skillId: String, context: SkillContext, snapshot: SkillSnapshot): CompletableFuture<SkillResult> {
        if (closed.get()) return completed(SkillResult.Status.CANCELLED, "Skill engine is stopped")
        val skill = snapshot.byId[skillId] ?: return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $skillId")
        if (current.get().byId[skillId] !== skill) return completed(SkillResult.Status.CANCELLED, "Skill definition changed before dispatch")
        if (!skill.available()) return completed(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "A component dependency is unavailable")
        if (!skill.enabled) return completed(SkillResult.Status.DISABLED, "Skill is disabled")
        val playerId = context.casterId
        val state = synchronized(reloadLock) {
            if (closed.get() || current.get().byId[skill.id] !== skill) return completed(SkillResult.Status.CANCELLED, "Skills changed before the cast started")
            ExecutionState(playerId, snapshot).also {
                it.dependencies.addAll(skill.dependencies)
                executions.add(it)
            }
        }
        state.result.whenComplete { _, _ -> executions.remove(state); state.release() }
        try {
            InvocationSnapshots.capture(context, state, scheduler)
                .thenCompose { runner.run(skill, it, state, 1) }
                .whenComplete { result, error -> state.result.complete(if (error == null) result else failure(error)) }
        } catch (error: Throwable) { state.result.complete(failure(error)) }
        return state.result
    }

    fun runNested(skillId: String, context: SkillContext, state: ExecutionState, depth: Int): CompletionStage<SkillResult> {
        state.checkActive()
        val skill = state.snapshot.byId[skillId] ?: return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $skillId")
        return runner.run(skill, context, state, depth)
    }

    override fun trigger(triggerId: String, context: SkillContext): CompletionStage<List<SkillResult>> {
        return triggerInSnapshot(triggerId, context, current.get())
    }

    private fun triggerInSnapshot(triggerId: String, context: SkillContext, snapshot: SkillSnapshot): CompletionStage<List<SkillResult>> {
        val results = snapshot.byTrigger[triggerId].orEmpty().map { skill ->
            castInSnapshot(skill.id, context, snapshot).thenApply { result ->
                diagnostics.report(skill.id, result)
                result
            }
        }
        return CompletableFuture.allOf(*results.toTypedArray()).thenApply { java.util.List.copyOf(results.map { it.join() }) }
    }

    // Keep staging separate so the core can validate before cancelling any tasks.
    fun prepareReload(): SkillLoadResult = if (closed.get()) SkillLoadResult(null, SkillReloadResult(false, 0, 0, listOf("Skill engine is stopped"))) else generalPlatform.prepareReload()

    fun commitReload(candidate: SkillLoadResult): SkillReloadResult {
        candidate.platformCandidate?.let { return generalPlatform.repository.commit(it) }
        var cancelPrevious: () -> Unit = {}
        val result = synchronized(reloadLock) {
            if (!candidate.report.success()) return@synchronized candidate.report
            if (closed.get()) return@synchronized SkillReloadResult(false, 0, 0, listOf("Skill engine is stopped"))
            val snapshot = requireNotNull(candidate.snapshot)
            val missing = snapshot.byId.values.filterNot { it.available() }
            if (missing.isNotEmpty()) return@synchronized SkillReloadResult(false, 0, 0, missing.map { "${it.source}: component was removed during reload" })
            cancelPrevious = publishDefinitions(snapshot)
            candidate.report
        }
        if (result.success()) cancelPrevious()
        return result
    }

    internal fun publishDefinitions(snapshot: SkillSnapshot): () -> Unit = synchronized(reloadLock) {
        val previousJobs = executions.toList()
        current.set(snapshot)
        diagnostics.retain(snapshot.byId.keys)
        return@synchronized { previousJobs.forEach { it.cancel("Skills reloaded") } }
    }

    internal fun compileSourceLegacy(definition: SkillDefinition): CompiledSkill {
        val values = LinkedHashMap(definition.configuration())
        values["id"]?.let { require(it is String && it.matches(Regex("[a-z0-9_]{1,100}"))) { "${definition.origin()}.id: invalid schema 1 skill ID" } }
        values["id"] = definition.key().value()
        values.remove("schema")
        return SkillCompilerImpl(environment, effects, conditions, triggers, filterCompiler).compile(SkillArguments(values, definition.origin()))
    }

    override fun reload(): SkillReloadResult = commitReload(prepareReload())
    override fun skills(): List<SkillInfo> = java.util.List.copyOf(current.get().byId.values.map { it.info() })

    override fun cancel(playerId: UUID) {
        executions.filter { it.playerId == playerId }.forEach { it.cancel("Player left") }
        cooldowns.remove(playerId)
    }

    fun cancelPending(reason: String) = synchronized(reloadLock) {
        executions.toList().forEach { it.cancel(reason) }
    }

    fun closeOwner(owner: Plugin) {
        val handles = lifecycle.disable(owner) {
            allHandles().filter { it.owner === owner }
        }
        handles.forEach { it.close() }
    }

    fun enableOwner(owner: Plugin) = lifecycle.enable(owner)

    override fun shutdown() {
        val handles = lifecycle.stop {
            closed.set(true)
            allHandles()
        } ?: return
        executions.toList().forEach { it.cancel("Skill engine stopped") }
        handles.forEach { it.close() }
        listeners.values.toList().forEach { safelyClose(it) }
        listeners.clear()
        cooldowns.clear()
        diagnostics.clear()
    }

    private fun allHandles(): List<SkillRegistration> = effects.snapshot() + conditions.snapshot() + triggers.snapshot() + generalPlatform.registrations()

    private fun invalidate(handle: SkillRegistration) {
        executions.filter { handle in it.dependencies }.forEach { it.cancel("Component ${handle.id} was removed") }
    }

    internal fun invalidateRegistration(handle: SkillRegistration) = invalidate(handle)

    private fun safelyClose(handle: AutoCloseable) {
        try { handle.close() } catch (error: Exception) { plugin.logger.warning("Could not close skill listener: ${error.message}") }
    }

    private fun completed(status: SkillResult.Status, message: String) = CompletableFuture.completedFuture(SkillResult.of(status, message))

    private fun failure(error: Throwable): SkillResult {
        var cause = error
        while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
        return SkillResult.of(if (cause is CancellationException) SkillResult.Status.CANCELLED else SkillResult.Status.ERROR, cause.message ?: cause.javaClass.simpleName)
    }
}
