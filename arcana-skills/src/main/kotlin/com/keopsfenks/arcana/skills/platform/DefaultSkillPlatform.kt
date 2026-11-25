package com.keopsfenks.arcana.skills.platform

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.legacy.LegacyActorException
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistry
import com.keopsfenks.arcana.skills.platform.sources.*
import com.keopsfenks.arcana.skills.platform.compile.SourceSkillCompiler
import com.keopsfenks.arcana.skills.config.SkillLoadResult
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import java.nio.file.Path
import com.keopsfenks.arcana.skills.platform.values.PersistentVariableStore
import com.keopsfenks.arcana.skills.platform.values.VariableStore
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import com.keopsfenks.arcana.skills.execution.ExecutionState
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState
import com.keopsfenks.arcana.skills.platform.execution.PlatformRunner
import com.keopsfenks.arcana.skills.platform.execution.FlowCursorStore
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.providers.ProviderRegistry
import com.keopsfenks.arcana.skills.platform.admission.AdmissionCoordinator

class DefaultSkillPlatform(
    private val engine: DefaultSkillEngine,
    private val lifecycle: SkillLifecycleGate,
    owner: ArcanaPlugin,
    directory: Path,
    environment: ExpressionEnvironment,
    environmentFactory: ((Array<String>) -> ExpressionEnvironment)? = null,
    clock: () -> Long = System::nanoTime
) : SkillPlatform {
    internal val monotonicClock = clock
    val traces = com.keopsfenks.arcana.skills.platform.diagnostics.SkillTraceRecorder(clock)
    private val ownedResources = ConcurrentHashMap.newKeySet<AutoCloseable>()
    fun own(resource: AutoCloseable) { ownedResources.add(resource); if (engine.stopped && ownedResources.remove(resource)) resource.close() }
    private val registries = ConcurrentHashMap<SkillComponentType<*>, PlatformRegistry<*>>()
    val variables = VariableStore(clock, persistence = PersistentVariableStore())
    val flowCursors = FlowCursorStore()
    val providers = ProviderRegistry(this)
    val admissions = AdmissionCoordinator(engine, lifecycle, clock)
    val cleanup = com.keopsfenks.arcana.skills.platform.managed.DefaultSkillCleanup(engine.scheduler)
    val managed = com.keopsfenks.arcana.skills.platform.managed.ManagedWorkRegistry(lifecycle, engine::reportProviderFailure)
    val contributions = com.keopsfenks.arcana.skills.platform.holders.ContributionStore(managed, lifecycle, engine::reportProviderFailure)
    val auraEvents = com.keopsfenks.arcana.skills.platform.auras.AuraEventDispatcher(engine, this, lifecycle)
    val auras = com.keopsfenks.arcana.skills.platform.auras.AuraManager(engine, this, lifecycle, clock)
    val projectiles = com.keopsfenks.arcana.skills.platform.projectiles.ProjectileManager(engine, this)
    val holderManager = com.keopsfenks.arcana.skills.platform.holders.HolderManager(engine, this, lifecycle)
    val interceptors = com.keopsfenks.arcana.skills.platform.events.InterceptorDispatcher(engine, this, lifecycle)
    internal fun componentActive(handle: SkillRegistration) = handle.isActive && lifecycle.available(handle.owner)
    val interceptorListener = com.keopsfenks.arcana.skills.platform.events.InterceptorListener(engine, this)
    val generatedSources = com.keopsfenks.arcana.skills.platform.events.GeneratedDamageSources(clock)
    val triggerSubscriptions = com.keopsfenks.arcana.skills.platform.events.TriggerSubscriptions(engine, this, lifecycle)
    val actorLifecycleListener = com.keopsfenks.arcana.skills.platform.events.ActorLifecycleListener(engine)
    private val runner = PlatformRunner(engine, this)
    val repository = DefinitionRepository(lifecycle, engine::compileSourceLegacy,
        { definition -> SourceSkillCompiler(this, environment, definition.key().namespace(), environmentFactory).compile(definition) },
        engine::publishDefinitions, engine::invalidateRegistration)

    init { repository.register(owner, DirectoryDefinitionSource(SkillKey("arcana", "default"), directory), core = true) }

    @Suppress("UNCHECKED_CAST")
    fun <T : SkillPlatformComponent> registry(type: SkillComponentType<T>): PlatformRegistry<T> =
        registries.computeIfAbsent(type) { PlatformRegistry(type, lifecycle) { engine.invalidateRegistration(it) } } as PlatformRegistry<T>

    override fun <T : SkillPlatformComponent> register(owner: Plugin, type: SkillComponentType<T>, key: SkillKey, component: T): SkillRegistration {
        val handle = registry(type).register(owner, key, component)
        if (component is SkillPlatformTrigger) try { triggerSubscriptions.install(handle, key, component) } catch (error: Throwable) { handle.close(); throw error }
        return handle
    }
    override fun subscribeTimer(owner: Plugin, trigger: SkillKey, actor: SkillActor, intervalTicks: Long) = triggerSubscriptions.timer(owner, trigger, actor, intervalTicks)

    internal fun registrations(): List<SkillRegistration> = registries.values.flatMap { it.snapshot() } + repository.registrations()
    override fun registerSource(owner: Plugin, source: SkillDefinitionSource) = repository.register(owner, source)
    override fun reloadSource(source: SkillKey): SkillReloadResult = repository.commit(repository.prepare(setOf(source)))
    override fun definitions() = repository.definitions()
    override fun refreshHolders(actor: SkillActor) = holderManager.refresh(actor)
    override fun holders(actor: SkillActor) = holderManager.holders(actor)
    internal fun close() { actorLifecycleListener.close(); triggerSubscriptions.close(); generatedSources.close(); auraEvents.close(); auras.close(); projectiles.close(); holderManager.close(); managed.close(); interceptorListener.close(); variables.close(); flowCursors.close(); admissions.close(); ownedResources.toList().forEach { if (ownedResources.remove(it)) try { it.close() } catch (error: Exception) { engine.reportProviderFailure("Could not close catalog resource: ${error.message}") } }; traces.close() }
    internal fun prepareReload(): SkillLoadResult {
        val candidate = repository.prepare()
        return SkillLoadResult(candidate.snapshot?.legacy, candidate.report, candidate)
    }
    internal fun legacyTrigger(key: SkillKey): SkillRegistration? = if (key.namespace() == "arcana") engine.triggers.get(key.value()) else null

    override fun cast(key: SkillKey, invocation: SkillInvocation): CompletionStage<SkillResult> {
        return castInSnapshot(key, invocation, repository.snapshot())
    }

    internal fun castInSnapshot(key: SkillKey, invocation: SkillInvocation, snapshot: DefinitionSnapshot): CompletableFuture<SkillResult> {
        if (engine.stopped) return completed(SkillResult.Status.CANCELLED, "Skill engine is stopped")
        val revision = snapshot.entries[key] ?: return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $key")
        if (revision.legacy != null) {
            return try { engine.castInSnapshot(DefinitionRevision.legacyId(key), LegacyInvocationAdapter.toLegacy(invocation), snapshot.legacy) }
            catch (error: LegacyActorException) { CompletableFuture.completedFuture(error.result) }
        }
        val admission = lifecycle.locked {
            if (repository.snapshot().entries[key] !== revision || !revision.isActive) return@locked SkillResult.of(SkillResult.Status.CANCELLED, "Definition changed before dispatch")
            if (!revision.available() || !lifecycle.available(revision.owner)) return@locked SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Definition $key is unavailable")
            val root = ExecutionState(invocation.actor().id(), snapshot.legacy)
            root.dependencies.add(revision); root.dependencies.addAll(revision.dependencies)
            if (!engine.enroll(root)) return@locked SkillResult.of(SkillResult.Status.CANCELLED, "Skill engine is stopped")
            attach(root)
        }
        if (admission is SkillResult) return CompletableFuture.completedFuture(admission)
        val state = admission as PlatformExecutionState
        state.traceId = traces.begin(key, invocation.actor().id(), revision.revisionId)
        traces.event(state.traceId, "start", mapOf("schema" to "2", "selectedEntities" to invocation.selection().entities().size.toString()))
        state.root.result.whenComplete { result, error -> traces.event(state.traceId, "complete", mapOf("status" to (result?.status()?.name ?: "ERROR"), "steps" to state.root.appliedSteps.toString())) }
        try {
            runner.run(revision, invocation, state, 1).whenComplete { result, error ->
                state.root.result.complete(if (error == null) result else PlatformExecutionState.failure(error))
            }
        } catch (error: Throwable) { state.root.result.complete(PlatformExecutionState.failure(error)) }
        return state.root.result
    }

    internal fun attach(root: ExecutionState): PlatformExecutionState = synchronized(root) {
        root.platformState ?: PlatformExecutionState(root, root.snapshot.definitions ?: repository.snapshot(), ::componentActive).also { root.platformState = it }
    }
    internal fun runNested(key: SkillKey, invocation: SkillInvocation, state: PlatformExecutionState, depth: Int, parent: InvocationFrame? = null): CompletableFuture<SkillResult> {
        state.checkActive()
        val revision = state.snapshot.entries[key] ?: return completed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $key")
        return runner.run(revision, invocation, state, depth, parent)
    }
    private fun completed(status: SkillResult.Status, message: String) = CompletableFuture.completedFuture(SkillResult.of(status, message))

    override fun trigger(key: SkillKey, invocation: SkillInvocation): CompletionStage<List<SkillResult>> {
        auraEvents.emit(key, invocation.actor(), invocation)
        val snapshot = repository.snapshot()
        val native = prepareTrigger(key, invocation, snapshot)
        val legacy = snapshot.legacy.byTrigger[DefinitionRevision.legacyId(key)].orEmpty().mapNotNull { skill -> snapshot.entries.values.find { it.legacy === skill } }.map { it.key to invocation }
        val work = legacy.map { (skill, supplied) -> castInSnapshot(skill, supplied, snapshot) } + (native?.invoke() ?: emptyList())
        return CompletableFuture.allOf(*work.toTypedArray()).thenApply { java.util.List.copyOf(work.map { it.join() }) }
    }
    /** Captures revisions and holder identities at emission; never reselects after a handoff. */
    internal fun prepareTrigger(key: SkillKey, invocation: SkillInvocation, snapshot: DefinitionSnapshot = repository.snapshot()): (() -> List<CompletableFuture<SkillResult>>)? {
        val global = snapshot.normalTriggers[key].orEmpty().map { Triple(it.key, invocation, { true }) }
        val held = holderManager.active(invocation.actor()).flatMap { binding -> binding.revisions.mapNotNull { revision ->
            val skill = revision.native
            if (skill != null && skill.enabled && skill.holderOnly && skill.interceptors.isEmpty() && key in skill.triggers && snapshot.entries[revision.key] === revision)
                Triple(revision.key, invocation.derive().parameters(binding.holder.parameters()).build(), { holderManager.active(invocation.actor()).any { it === binding } }) else null
        } }
        val work = (global + held).distinctBy { it.first to it.second.parameters() }
        if (work.isEmpty()) return null
        return { work.filter { it.third() }.map { (skill, supplied) -> castInSnapshot(skill, supplied, snapshot) } }
    }
}
