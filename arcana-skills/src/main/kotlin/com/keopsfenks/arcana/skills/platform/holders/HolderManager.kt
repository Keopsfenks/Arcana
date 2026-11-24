package com.keopsfenks.arcana.skills.platform.holders

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.providers.StatBindings
import com.keopsfenks.arcana.skills.platform.values.*
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/** Coalesced provider reads and diff-only passive reconciliation, only for explicitly tracked actors. */
class HolderManager(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform, private val lifecycle: SkillLifecycleGate) : AutoCloseable {
    private class Session(val actor: SkillActor) {
        var active = true
        var pending: CompletableFuture<List<SkillHolder>>? = null
        var fallback: ScheduledWork<Unit>? = null
        val bindings = LinkedHashMap<HolderBinding, Runtime>()
    }
    private class Runtime(val binding: HolderBinding) : AutoCloseable {
        val alive = AtomicBoolean(true)
        val roots = ConcurrentHashMap.newKeySet<ExecutionState>()
        val leases = ConcurrentHashMap.newKeySet<AutoCloseable>()
        override fun close() {
            if (!alive.compareAndSet(true, false)) return
            roots.toList().forEach { it.cancel("Holder removed") }; roots.clear()
            leases.toList().forEach { it.close() }; leases.clear()
        }
        fun root(root: ExecutionState) { roots.add(root); if (!alive.get()) { roots.remove(root); root.cancel("Holder removed") } }
        fun lease(lease: AutoCloseable) { leases.add(lease); if (!alive.get() && leases.remove(lease)) lease.close() }
    }
    private val sessions = HashMap<UUID, Session>()
    private var stopped = false
    fun holders(actor: SkillActor): List<SkillHolder> = lifecycle.locked { java.util.List.copyOf(sessions[actor.id()]?.bindings?.keys?.filter { it.provider.isActive }?.map { it.holder } ?: emptyList()) }
    fun active(actor: SkillActor): List<HolderBinding> = lifecycle.locked { sessions[actor.id()]?.bindings?.keys?.filter { it.provider.isActive && it.revisions.all { revision -> revision.available() } }?.toList() ?: emptyList() }
    fun refresh(actor: SkillActor): CompletableFuture<List<SkillHolder>> {
        val providers = platform.registry(SkillComponentType.HOLDER).snapshot().filter { it.isActive }
        if (providers.isEmpty()) { removeActor(actor.id()); return CompletableFuture.completedFuture(emptyList()) }
        var started = false
        val pair = lifecycle.locked {
            if (stopped || engine.stopped) return@locked null
            val session = sessions[actor.id()] ?: run {
                if (sessions.size >= 4096) throw PlatformBudgetException("Tracked holder actor limit is 4096")
                Session(actor).also { sessions[actor.id()] = it }
            }
            val result = session.pending ?: CompletableFuture<List<SkillHolder>>().also { session.pending = it; started = true }
            session to result
        } ?: return CompletableFuture.completedFuture(emptyList())
        val (session, result) = pair
        if (!started) return result
        val timeout = engine.scheduler.on(actor.entity(), 100) {
            if (!result.isDone && currentSafe(session)) removeActor(actor.id(), "Holder provider refresh exceeded 100 ticks")
            Unit
        }
        result.whenComplete { _, _ -> timeout.cancel() }
        timeout.future.whenComplete { _, error -> if (error != null && !result.isDone && currentSafe(session)) removeActor(actor.id()) }
        if (result.isDone) return result
        val pending = providers.map { provider ->
            try { provider.component.holders(actor).thenApply { values ->
                val copied = java.util.List.copyOf(values)
                require(copied.size <= 64) { "Holder provider ${provider.key} returned more than 64 holders" }
                copied.forEach { require(it.key().namespace() == provider.key.namespace()) { "Holder ${it.key()} belongs to another provider namespace" } }
                provider to copied
            }.toCompletableFuture() } catch (error: Throwable) { CompletableFuture.failedFuture(error) }
        }
        CompletableFuture.allOf(*pending.toTypedArray()).whenComplete { _, failure ->
            if (failure != null) finish(session, result, failure)
            else try {
                val snapshot = platform.repository.snapshot()
                val bindings = pending.flatMap { future ->
                    val (provider, holders) = future.join()
                    if (!platform.componentActive(provider)) emptyList() else holders.map { holder ->
                        val revisions = holder.skills().map { key -> snapshot.entries[key]?.takeIf { it.available() && it.native != null } ?: throw PlatformDependencyException("Holder ${holder.key()} references unavailable native skill $key") }
                        revisions.forEach { ParameterBindings.bind(requireNotNull(it.native).parameters, holder.parameters()) }
                        HolderBinding(provider, holder, revisions)
                    }
                }
                require(bindings.sumOf { it.revisions.size } <= 256) { "An actor may bind at most 256 held skill definitions" }
                require(bindings.sumOf { binding -> binding.revisions.sumOf { requireNotNull(it.native).passives.size } } <= 64) { "An actor may activate at most 64 holder passive instances" }
                require(bindings.size <= 64) { "An actor may have at most 64 active holders" }
                require(bindings.map { Triple(it.provider, it.holder.key(), it.holder.slot()) }.distinct().size == bindings.size) { "Duplicate holder key and slot" }
                val removed = mutableListOf<Runtime>(); val added = mutableListOf<Runtime>()
                val accepted = lifecycle.locked {
                    if (!current(session) || result.isDone) false else {
                        val after = bindings.filter { platform.componentActive(it.provider) && it.revisions.all { revision -> revision.available() } }.toSet()
                        val change = HolderDiff.between(session.bindings.keys.toSet(), after)
                        change.removed.forEach { session.bindings.remove(it)?.let(removed::add) }
                        change.added.forEach { binding -> Runtime(binding).also { session.bindings[binding] = it; added.add(it) } }
                        true
                    }
                }
                removed.forEach { it.close() }
                if (!accepted) { finish(session, result, CancellationException("Holder actor retired")); return@whenComplete }
                val initializers = added.map { initialize(session, it) }
                CompletableFuture.allOf(*initializers.toTypedArray()).whenComplete { _, error ->
                    if (error != null) {
                        lifecycle.locked { added.forEach { session.bindings.remove(it.binding, it) } }
                        added.forEach { it.close() }
                    }
                    finish(session, result, error)
                }
            } catch (error: Throwable) { finish(session, result, error) }
        }
        return result
    }
    private fun initialize(session: Session, runtime: Runtime): CompletableFuture<Unit> {
        val binding = runtime.binding
        val work = binding.revisions.filter { requireNotNull(it.native).enabled && requireNotNull(it.native).passives.isNotEmpty() }.map { revision ->
            try {
                val definition = requireNotNull(revision.native)
                val root = ExecutionState(session.actor.id(), platform.repository.snapshot().legacy)
                root.dependencies.add(binding.provider); root.dependencies.add(revision); root.dependencies.addAll(revision.dependencies)
                val state = lifecycle.locked {
                    check(current(session) && runtime.alive.get() && revision.available()) { "Holder retired before passive initialization" }
                    check(engine.enroll(root)); platform.attach(root)
                }
                runtime.root(root)
                val input = SkillInvocation.builder(session.actor).parameters(ParameterBindings.bind(definition.parameters, binding.holder.parameters())).build()
                InvocationSnapshots.capture(input, engine.scheduler, state).thenCompose { captured ->
                    val frame = state.frame(InvocationFrame(captured))
                    val persistent = definition.bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { (_, values) -> values.map { it.key }.toSet() }
                    val variables = platform.variables.access(frame, persistent)
                    variables.prepare().thenCompose { StatBindings.capture(captured, definition.stats, state, frame, platform) }.thenCompose { enriched ->
                        val actual = if (enriched === captured) frame else state.frame(frame.view(enriched))
                        val context = PlatformExecutionContext(engine, platform, state, actual, definition, 1, variables)
                        context.track(engine.scheduler.on(session.actor.entity()) {
                            state.checkActive(); check(currentSafe(session) && runtime.alive.get()) { "Holder removed" }
                            definition.conditions.test(context.invocation()) && (definition.permission == null || session.actor.player().orElse(null)?.hasPermission(definition.permission) == true)
                        }).thenCompose { enabled ->
                            if (!enabled) CompletableFuture.completedFuture(Unit) else {
                                val leases = definition.passives.map { contribution ->
                                    state.step()
                                    platform.contributions.apply(binding.provider, context, contribution).thenApply { lease -> runtime.lease(lease); Unit }
                                }
                                CompletableFuture.allOf(*leases.toTypedArray()).thenApply { Unit }
                            }
                        }
                    }
                }.whenComplete { _, error -> if (error != null) root.cancel("Passive initialization failed") }
            } catch (error: Throwable) { CompletableFuture.failedFuture<Unit>(error) }
        }
        return CompletableFuture.allOf(*work.toTypedArray()).thenApply { Unit }
    }
    private fun current(session: Session) = !stopped && session.active && sessions[session.actor.id()] === session
    private fun currentSafe(session: Session) = lifecycle.locked { current(session) }
    private fun finish(session: Session, result: CompletableFuture<List<SkillHolder>>, error: Throwable?) {
        val needsTimer = lifecycle.locked {
            if (session.pending === result) session.pending = null
            current(session) && session.fallback == null && platform.registry(SkillComponentType.HOLDER).snapshot().isNotEmpty()
        }
        if (error == null) result.complete(holders(session.actor)) else result.completeExceptionally(error)
        if (needsTimer) schedule(session)
    }
    private fun schedule(session: Session) {
        val work = engine.scheduler.on(session.actor.entity(), 20) {
            lifecycle.locked { session.fallback = null }
            if (currentSafe(session)) refresh(session.actor)
            Unit
        }
        val accepted = lifecycle.locked { if (!current(session) || session.fallback != null) false else { session.fallback = work; true } }
        if (!accepted) work.cancel()
        work.future.whenComplete { _, error -> if (error != null && accepted && currentSafe(session)) removeActor(session.actor.id()) }
    }
    fun removeActor(id: UUID, reason: String = "Holder actor retired") {
        val session = lifecycle.locked { sessions.remove(id)?.also { it.active = false } } ?: return
        session.fallback?.cancel(); session.pending?.completeExceptionally(CancellationException(reason))
        session.bindings.values.toList().forEach { it.close() }; session.bindings.clear()
    }
    fun remove(handle: SkillRegistration) {
        val removed = mutableListOf<Runtime>()
        lifecycle.locked { sessions.values.forEach { session ->
            session.bindings.entries.removeIf { (binding, runtime) ->
                val match = binding.provider === handle || binding.revisions.any { it === handle || handle in it.dependencies }
                if (match) removed.add(runtime)
                match
            }
        } }
        removed.forEach { it.close() }
    }
    override fun close() { val ids = lifecycle.locked { stopped = true; sessions.keys.toList() }; ids.forEach(::removeActor) }
}
