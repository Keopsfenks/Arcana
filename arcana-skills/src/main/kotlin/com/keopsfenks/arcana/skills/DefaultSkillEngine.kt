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
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
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
    val random: () -> Double = { ThreadLocalRandom.current().nextDouble() },
    expressionEnvironmentFactory: ((Array<String>) -> ExpressionEnvironment)? = null
) : SkillEngine {
    val cooldowns = CooldownStore(clock)
    internal val lifecycle = SkillLifecycleGate()
    private val generalPlatform = DefaultSkillPlatform(this, lifecycle, plugin, directory, environment, expressionEnvironmentFactory, clock)
    private val diagnostics = SkillRuntimeDiagnostics({ plugin.logger }, clock)
    val effects = ComponentRegistry<SkillEffect>(lifecycle) { invalidate(it) }
    val conditions = ComponentRegistry<SkillCondition>(lifecycle) { invalidate(it) }
    val triggers = ComponentRegistry<SkillTrigger>(lifecycle) { handle ->
        invalidate(handle)
        listeners.remove(handle)?.let { safelyClose(it) }
    }
    private val listeners = ConcurrentHashMap<SkillRegistration, AutoCloseable>()
    private val current = AtomicReference(SkillSnapshot.EMPTY)
    private val executions = ConcurrentHashMap.newKeySet<ExecutionState>()
    private val closed = AtomicBoolean(false)
    internal val stopped: Boolean get() = closed.get()
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
                    if (!handle.isActive || closed.get()) return null
                    if (generalPlatform.triggerSubscriptions.generatedId(context.values["damage_source_id"] as? String)) return null
                    val native = prepareNativeTrigger(trigger.id, context, snapshot)
                    if (snapshot.byTrigger[trigger.id].isNullOrEmpty() && native == null) return null
                    return Runnable {
                        if (handle.isActive && !closed.get()) { triggerInSnapshot(trigger.id, context, snapshot); native?.invoke() }
                    }
                }
            })
            listeners[handle] = listener
            if (!handle.isActive) listeners.remove(handle)?.let { safelyClose(it) }
        } catch (error: Throwable) { handle.close(); throw error }
        return handle
    }

    override fun cast(skillId: String, context: SkillContext): CompletionStage<SkillResult> {
        if (closed.get()) return completed(SkillResult.Status.CANCELLED, "Skill engine is stopped")
        val snapshot = current.get()
        if (skillId in snapshot.byId) return castInSnapshot(skillId, context, snapshot)
        val key = try { SkillKey.parse(skillId, "arcana") } catch (_: IllegalArgumentException) { return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $skillId") }
        return generalPlatform.castInSnapshot(key, LegacyInvocationAdapter.fromLegacy(context), snapshot.definitions ?: generalPlatform.repository.snapshot())
    }
    override fun platform() = generalPlatform

    internal fun castInSnapshot(skillId: String, context: SkillContext, snapshot: SkillSnapshot): CompletableFuture<SkillResult> {
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
        val traceKey = try { SkillKey.parse(skillId, "arcana") } catch (_: IllegalArgumentException) { null }
        val trace = traceKey?.let { generalPlatform.traces.begin(it, playerId, snapshot.definitions?.entries?.get(it)?.revisionId ?: UUID(0, 0)) }
        generalPlatform.traces.event(trace, "start", mapOf("schema" to "1"))
        state.result.whenComplete { result, _ -> generalPlatform.traces.event(trace, "complete", mapOf("status" to (result?.status()?.name ?: "ERROR"), "steps" to state.appliedSteps.toString())) }
        try {
            InvocationSnapshots.capture(context, state, scheduler)
                .thenCompose { runner.run(skill, it, state, 1) }
                .whenComplete { result, error -> state.result.complete(if (error == null) result else failure(error)) }
        } catch (error: Throwable) { state.result.complete(failure(error)) }
        return state.result
    }

    fun runNested(skillId: String, context: SkillContext, state: ExecutionState, depth: Int, platformFrame: com.keopsfenks.arcana.skills.platform.values.InvocationFrame? = null): CompletionStage<SkillResult> {
        state.checkActive()
        platformFrame?.checkActive()
        val skill = state.snapshot.byId[skillId]
        if (skill == null) {
            val key = try { SkillKey.parse(skillId, "arcana") } catch (_: IllegalArgumentException) { return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $skillId") }
            return generalPlatform.runNested(key, LegacyInvocationAdapter.fromLegacy(context), generalPlatform.attach(state), depth, platformFrame)
        }
        return runner.run(skill, context, state, depth, platformFrame)
    }

    internal fun runLegacyNested(skill: CompiledSkill, context: SkillContext, state: ExecutionState, depth: Int, platformFrame: com.keopsfenks.arcana.skills.platform.values.InvocationFrame? = null) = runner.run(skill, context, state, depth, platformFrame)
    internal fun enroll(state: ExecutionState): Boolean = synchronized(reloadLock) {
        if (closed.get()) false else {
            executions.add(state)
            state.result.whenComplete { _, _ -> executions.remove(state); state.release() }
            true
        }
    }

    override fun trigger(triggerId: String, context: SkillContext): CompletionStage<List<SkillResult>> {
        val snapshot = current.get()
        if (generalPlatform.triggerSubscriptions.generatedId(context.values["damage_source_id"] as? String)) return CompletableFuture.completedFuture(emptyList())
        val prepared = prepareNativeTrigger(triggerId, context, snapshot, includeAura = true)
        val legacy = triggerInSnapshot(triggerId, context, snapshot).toCompletableFuture()
        val native = prepared?.invoke().orEmpty()
        return legacy.thenCombine(CompletableFuture.allOf(*native.toTypedArray())) { results, _ -> java.util.List.copyOf(results + native.map { it.join() }) }
    }

    private fun prepareNativeTrigger(triggerId: String, context: SkillContext, snapshot: SkillSnapshot, includeAura: Boolean = false): (() -> List<CompletableFuture<SkillResult>>)? {
        val key = try { SkillKey.parse(triggerId, "arcana") } catch (_: IllegalArgumentException) { return null }
        val definitions = snapshot.definitions ?: generalPlatform.repository.snapshot()
        val actor = com.keopsfenks.arcana.core.skills.platform.SkillActor.of(context.caster)
        val aura = includeAura && generalPlatform.auraEvents.observes(key, actor)
        if (!aura && !generalPlatform.hasNativeTrigger(key, actor, definitions)) return null
        val input = try { LegacyInvocationAdapter.fromLegacy(context).let { it.derive().trigger(it.target().orElse(null)).build() } } catch (error: IllegalArgumentException) {
            val result = SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Legacy payload cannot be represented by schema 2: ${error.message}")
            return { diagnostics.report(triggerId, result); listOf(CompletableFuture.completedFuture(result)) }
        }
        val native = generalPlatform.prepareTrigger(key, input, definitions)
        return { if (aura) generalPlatform.auraEvents.emit(key, actor, input); native?.invoke().orEmpty() }
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
    override fun skills(): List<SkillInfo> = java.util.List.copyOf(current.get().byId.values.map { it.info() } + generalPlatform.repository.snapshot().entries.values.mapNotNull { revision -> revision.native?.let {
        SkillInfo(com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision.legacyId(it.key), it.enabled, it.triggers.map(SkillKey::toString), it.origin, if (revision.available()) "" else "A component dependency is unavailable")
    } })

    override fun cancel(playerId: UUID) {
        generalPlatform.triggerSubscriptions.removeActor(playerId)
        executions.filter { it.playerId == playerId }.forEach { it.cancel("Player left") }
        generalPlatform.holderManager.removeActor(playerId)
        generalPlatform.managed.removeActor(playerId)
        generalPlatform.admissions.removeActor(playerId)
        generalPlatform.variables.evictActor(playerId)
        generalPlatform.flowCursors.removeActor(playerId)
    }

    fun cancelPending(reason: String) = synchronized(reloadLock) {
        executions.toList().forEach { it.cancel(reason) }
    }

    fun closeOwner(owner: Plugin) {
        val handles = lifecycle.disable(owner) {
            allHandles().filter { it.owner === owner }
        }
        handles.forEach { it.close() }
        generalPlatform.triggerSubscriptions.closeOwner(owner)
    }

    fun enableOwner(owner: Plugin) = lifecycle.enable(owner)

    override fun shutdown() {
        val handles = lifecycle.stop {
            closed.set(true)
            allHandles()
        } ?: return
        generalPlatform.managed.close()
        executions.toList().forEach { it.cancel("Skill engine stopped") }
        handles.forEach { it.close() }
        listeners.values.toList().forEach { safelyClose(it) }
        listeners.clear()
        cooldowns.clear()
        diagnostics.clear()
        generalPlatform.close()
    }

    private fun allHandles(): List<SkillRegistration> = effects.snapshot() + conditions.snapshot() + triggers.snapshot() + generalPlatform.registrations()

    private fun invalidate(handle: SkillRegistration) {
        generalPlatform.triggerSubscriptions.remove(handle)
        generalPlatform.managed.remove(handle)
        generalPlatform.holderManager.remove(handle)
        generalPlatform.admissions.remove(handle)
        generalPlatform.flowCursors.remove(handle)
        executions.filter { handle in it.dependencies }.forEach { it.cancel("Component ${handle.id} was removed") }
    }

    internal fun invalidateRegistration(handle: SkillRegistration) = invalidate(handle)
    internal fun reportProviderFailure(message: String) { plugin.logger.warning(message) }

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
