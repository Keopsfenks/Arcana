package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.platform.*
import java.time.Duration
import java.util.Optional
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal data class VariableEntry(val value: SkillValue, val deadline: Long?, val expiresAtMillis: Long?, val persistent: Boolean, val version: Long)
class VariableTargetException : IllegalStateException("Variable target is unavailable")

/** Pure immutable values and atomic replacements; no live Bukkit reads on this path. */
class VariableStore(
    private val clock: () -> Long = System::nanoTime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val persistence: PersistentVariableStore? = null
) : AutoCloseable {
    internal class SubjectState(val subject: VariableSubject) {
        val active = AtomicBoolean(true)
        val entries = ConcurrentHashMap<SkillKey, VariableEntry>()
        val loads = ConcurrentHashMap<SkillKey, CompletableFuture<Void>>()
        val loaded = ConcurrentHashMap.newKeySet<SkillKey>()
    }
    private val closed = AtomicBoolean(false)
    private val actors = ConcurrentHashMap<UUID, SubjectState>()
    private val server = SubjectState(VariableSubject(null, server = true))
    private val sequence = AtomicLong()
    private val writer = UUID.randomUUID().toString()

    fun access(frame: InvocationFrame, persistent: Map<SkillVariableScope, Set<SkillKey>> = emptyMap()): VariableAccess {
        check(!closed.get()) { "Variable store is closed" }
        val actor = actors.computeIfAbsent(frame.invocation.actor().id()) {
            SubjectState(VariableSubject(it, frame.invocation.actor().player().isPresent))
        }
        val target = frame.invocation.target().orElse(null)?.let { reference -> actors.computeIfAbsent(reference.id()) {
            SubjectState(VariableSubject(it, reference.player().isPresent))
        } }
        return VariableAccess(frame, actor, target, persistent.mapValues { java.util.Set.copyOf(it.value) })
    }

    inner class VariableAccess internal constructor(
        private val frame: InvocationFrame,
        private val actor: SubjectState,
        private val target: SubjectState?,
        private val persistent: Map<SkillVariableScope, Set<SkillKey>>
    ) : SkillVariables {
        private fun checkActive() {
            frame.checkActive()
            if (closed.get() || !actor.active.get()) throw CancellationException("Variable actor retired")
        }
        private fun state(scope: SkillVariableScope): SubjectState? = when (scope) {
            SkillVariableScope.INVOCATION -> null
            SkillVariableScope.ACTOR -> actor
            SkillVariableScope.TARGET -> target?.takeIf { it.active.get() } ?: throw VariableTargetException()
            SkillVariableScope.SERVER -> server
        }
        fun prepare(): CompletableFuture<Void> {
            checkActive()
            val pending = mutableListOf<CompletableFuture<Void>>()
            for ((scope, keys) in persistent) for (key in keys) {
                val state = requireNotNull(state(scope)) { "Invocation variables cannot persist" }
                val port = requireNotNull(persistence) { "Variable persistence is unavailable" }
                require(state.subject.player || state.subject.server) { "Entity variables cannot persist" }
                val future = state.loads.computeIfAbsent(key) {
                    port.load(state.subject, key).thenAccept { record ->
                        if (closed.get() || !state.active.get()) throw CancellationException("Variable subject retired")
                        state.loaded.add(key)
                        val remaining = record?.expiresAtMillis?.let { (it - wallClock()).coerceAtMost(86_400_000).coerceAtLeast(0) }
                        var write: VariableEntry? = null
                        state.entries.compute(key) { _, local ->
                            if (local != null) local.copy(persistent = true, version = sequence.incrementAndGet()).also { write = it }
                            else if (record?.value != null && (remaining == null || remaining > 0)) {
                                VariableEntry(record.value, remaining?.let { clock() + it * 1_000_000 }, record.expiresAtMillis, true, sequence.incrementAndGet())
                            } else null
                        }
                        write?.let { persist(state, key, it.value, it.expiresAtMillis, it.version) }
                    }
                }
                future.whenComplete { _, error -> if (error != null) state.loads.remove(key, future) }
                pending.add(future)
            }
            return CompletableFuture.allOf(*pending.toTypedArray()).thenApply { checkActive(); null }
        }

        override fun get(scope: SkillVariableScope, key: SkillKey): Optional<SkillValue> {
            checkActive()
            val state = try { state(scope) } catch (_: VariableTargetException) { return Optional.empty() }
            requirePrepared(scope, key, state)
            val map = state?.entries ?: frame.locals
            var expiredEntry: VariableEntry? = null
            var expiredVersion = 0L
            val entry = map.computeIfPresent(key) { _, current ->
                if (expired(current)) { expiredEntry = current; expiredVersion = sequence.incrementAndGet(); null } else current
            }
            expiredEntry?.takeIf { it.persistent }?.let { save(requireNotNull(state), key, null, null, expiredVersion) }
            return Optional.ofNullable(entry?.value)
        }

        override fun set(scope: SkillVariableScope, key: SkillKey, value: SkillValue, ttl: Duration): CompletionStage<Void> {
            checkActive()
            require(!ttl.isNegative && ttl <= Duration.ofHours(24)) { "Variable TTL must be between zero and 24 hours" }
            val state = state(scope)
            requirePrepared(scope, key, state)
            val nanos = ttl.toNanos()
            val map = state?.entries ?: frame.locals
            val result = map.compute(key) { _, old -> VariableEntry(value, if (nanos == 0L) null else clock() + nanos,
                if (nanos == 0L) null else Math.addExact(wallClock(), (nanos + 999_999) / 1_000_000),
                key in persistent[scope].orEmpty() || old?.persistent == true, sequence.incrementAndGet()) }!!
            if (result.persistent) save(requireNotNull(state), key, value, result.expiresAtMillis, result.version)
            return CompletableFuture.completedFuture(null)
        }

        override fun add(scope: SkillVariableScope, key: SkillKey, delta: Double): CompletionStage<Double> {
            checkActive()
            require(delta.isFinite()) { "Variable delta must be finite" }
            val state = state(scope)
            requirePrepared(scope, key, state)
            val map = state?.entries ?: frame.locals
            val result = map.compute(key) { _, previous ->
                val old = previous?.takeUnless(::expired)
                val amount = (old?.value?.number() ?: 0.0) + delta
                require(amount.isFinite()) { "Variable result must be finite" }
                VariableEntry(SkillValue.of(amount), old?.deadline, old?.expiresAtMillis,
                    key in persistent[scope].orEmpty() || previous?.persistent == true, sequence.incrementAndGet())
            }!!
            if (result.persistent) save(requireNotNull(state), key, result.value, result.expiresAtMillis, result.version)
            return CompletableFuture.completedFuture(result.value.number())
        }

        override fun remove(scope: SkillVariableScope, key: SkillKey): CompletionStage<Boolean> {
            checkActive()
            val state = state(scope)
            requirePrepared(scope, key, state)
            val map = state?.entries ?: frame.locals
            var removed: VariableEntry? = null
            var version = 0L
            map.compute(key) { _, current -> removed = current; version = sequence.incrementAndGet(); null }
            if (removed?.persistent == true || key in persistent[scope].orEmpty()) save(requireNotNull(state), key, null, null, version)
            return CompletableFuture.completedFuture(removed?.let { !expired(it) } ?: false)
        }

        private fun requirePrepared(scope: SkillVariableScope, key: SkillKey, state: SubjectState?) {
            if (key in persistent[scope].orEmpty() || state?.entries?.get(key)?.persistent == true) {
                check(state != null && key in state.loaded) { "Persistent variable requires prefetch: $key" }
            }
        }
        private fun save(state: SubjectState, key: SkillKey, value: SkillValue?, expires: Long?, version: Long) {
            checkActive()
            persist(state, key, value, expires, version)
        }
    }

    private fun persist(state: SubjectState, key: SkillKey, value: SkillValue?, expires: Long?, version: Long) {
        if (closed.get() || !state.active.get()) throw CancellationException("Variable subject retired")
        requireNotNull(persistence).write(state.subject, key, PersistedVariable(value, expires, writer, version))
    }

    private fun expired(entry: VariableEntry) = entry.deadline?.let { clock() - it >= 0 } ?: false
    fun entryCount(id: UUID): Int = actors[id]?.entries?.size ?: 0
    fun evictActor(id: UUID) {
        actors.remove(id)?.let { state -> state.active.set(false); state.entries.clear(); state.loads.values.forEach { it.cancel(false) }; state.loads.clear(); state.loaded.clear() }
        persistence?.evictActor(id)
    }
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            actors.keys.toList().forEach(::evictActor)
            server.active.set(false); server.entries.clear(); server.loads.values.forEach { it.cancel(false) }
            persistence?.close()
        }
    }
}
