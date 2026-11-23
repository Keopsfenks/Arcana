package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.SkillResult
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/** Automatic triggers have no command sender to receive execution failures. */
class SkillRuntimeDiagnostics(private val logger: () -> Logger, private val clock: () -> Long) {
    private val reported = ConcurrentHashMap<String, Long>()

    fun report(skillId: String, result: SkillResult) {
        if (result.status() != SkillResult.Status.ERROR && result.status() != SkillResult.Status.DEPENDENCY_UNAVAILABLE) return
        val now = clock()
        var emit = false
        reported.compute(skillId) { _, previous ->
            if (previous == null || now - previous >= 5_000_000_000L) { emit = true; now } else previous
        }
        if (emit) {
            try { logger().warning("Skill '$skillId' failed: ${result.message()}") }
            catch (_: Exception) { /* Logging must not alter the invocation result. */ }
        }
    }

    fun retain(ids: Set<String>) { reported.keys.retainAll(ids) }
    fun clear() = reported.clear()
}
