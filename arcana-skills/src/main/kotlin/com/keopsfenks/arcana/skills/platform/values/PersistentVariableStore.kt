package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.data.Profile
import com.keopsfenks.arcana.core.data.PlayerProfile
import com.keopsfenks.arcana.core.data.ServerProfile
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import com.keopsfenks.arcana.core.data.keys.PersistentDataKeyType
import com.keopsfenks.arcana.core.skills.platform.*
import org.bukkit.NamespacedKey
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

data class VariableSubject(val id: UUID?, val player: Boolean = false, val server: Boolean = false)
internal data class PersistedVariable(val value: SkillValue?, val expiresAtMillis: Long?, val writer: String, val version: Long)

/** Prefetch potentially blocking profile reads; subsequent writes update the already loaded cache. */
class PersistentVariableStore(
    private val profile: (VariableSubject) -> Profile = { if (it.server) ServerProfile.load() else PlayerProfile.load(requireNotNull(it.id)) },
    private val keys: (SkillKey, Boolean) -> PersistentDataKey<String> = { key, local ->
        PersistentDataKey(dataKey(key, local), PersistentDataKeyType.STRING, "", local)
    },
    executor: Executor? = null
) : AutoCloseable {
    companion object {
        internal fun dataKey(key: SkillKey, local: Boolean) = NamespacedKey("arcana", "skill_var/${if (local) "s" else "p"}/${key.namespace()}/${key.value()}")
    }
    private class Session {
        val active = AtomicBoolean(true)
        val pending = ConcurrentHashMap.newKeySet<CompletableFuture<*>>()
        @Volatile var profile: Profile? = null
    }
    private val closed = AtomicBoolean(false)
    private val sessions = ConcurrentHashMap<VariableSubject, Session>()
    private val dataKeys = ConcurrentHashMap<Pair<SkillKey, Boolean>, PersistentDataKey<String>>()
    private val owned = if (executor == null) ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1024),
        ThreadFactory { work -> Thread(work, "arcana-skills-data").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy()) else null
    private val io: Executor = executor ?: requireNotNull(owned)

    internal fun load(subject: VariableSubject, key: SkillKey): CompletableFuture<PersistedVariable?> {
        require(subject.player || subject.server) { "Entity variables cannot persist" }
        check(!closed.get()) { "Variable persistence is closed" }
        val session = sessions.computeIfAbsent(subject) { Session() }
        val result = CompletableFuture<PersistedVariable?>()
        session.pending.add(result)
        result.whenComplete { _, _ -> session.pending.remove(result) }
        try {
            io.execute {
                try {
                    if (closed.get() || !session.active.get()) throw CancellationException("Variable subject retired")
                    val loaded = synchronized(session) { session.profile ?: profile(subject).also { session.profile = it } }
                    val dataKey = dataKeys.computeIfAbsent(key to subject.server) { keys(key, subject.server) }
                    val record = VariableValueCodec.decode(loaded.read(dataKey))
                    if (closed.get() || !session.active.get()) throw CancellationException("Variable subject retired")
                    result.complete(record)
                } catch (error: Throwable) { result.completeExceptionally(error) }
                finally { if (closed.get() || !session.active.get()) session.profile = null }
            }
        } catch (error: Throwable) { result.completeExceptionally(error) }
        return result
    }

    internal fun write(subject: VariableSubject, key: SkillKey, record: PersistedVariable) {
        if (closed.get()) throw CancellationException("Variable persistence is closed")
        val session = sessions[subject] ?: throw CancellationException("Variable subject retired")
        if (!session.active.get()) throw CancellationException("Variable subject retired")
        val loaded = session.profile ?: error("Persistent variable requires prefetch: $key")
        val dataKey = dataKeys[key to subject.server] ?: error("Persistent variable requires prefetch: $key")
        val encoded = VariableValueCodec.encode(record)
        // Older completions from this writer cannot overwrite a newer value or removal tombstone.
        loaded.compute(dataKey) { previous ->
            val old = VariableValueCodec.decode(previous)
            if (old != null && old.writer == record.writer && old.version > record.version) previous else encoded
        }
    }

    fun evictActor(id: UUID) {
        sessions.entries.filter { it.key.id == id }.forEach { (key, session) -> if (sessions.remove(key, session)) stop(session) }
    }
    private fun stop(session: Session) {
        session.active.set(false)
        session.pending.toList().forEach { it.cancel(false) }
        session.pending.clear()
        session.profile = null
    }
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            sessions.values.toList().forEach(::stop)
            sessions.clear(); dataKeys.clear()
            owned?.shutdownNow()
        }
    }
}
