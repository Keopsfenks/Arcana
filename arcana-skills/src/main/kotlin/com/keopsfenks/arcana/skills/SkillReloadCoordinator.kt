package com.keopsfenks.arcana.skills

import com.keopsfenks.arcana.core.skills.SkillReloadResult
import com.keopsfenks.arcana.skills.config.SkillLoadResult

/** Preflight never publishes a candidate; the core commits after its normal task cancellation. */
class SkillReloadCoordinator(private val engine: DefaultSkillEngine, private val report: (SkillReloadResult) -> Unit) {
    private var staged: SkillLoadResult? = null
    private var emptyStartup = false

    fun initialize(): SkillReloadResult = engine.reload().also(report)

    fun prepare(allowEmptyStartup: Boolean = false): Boolean {
        staged = null
        emptyStartup = false
        val candidate = engine.prepareReload()
        if (!candidate.report.success()) {
            report(candidate.report)
            emptyStartup = allowEmptyStartup && engine.skills().isEmpty()
            return emptyStartup
        }
        staged = candidate
        return true
    }

    fun commit(): SkillReloadResult? {
        val candidate = staged
        staged = null
        if (emptyStartup) {
            emptyStartup = false
            engine.cancelPending("Arcana startup reload")
        }
        if (candidate == null) return null
        return engine.commitReload(candidate).also {
            report(it)
            if (!it.success()) engine.cancelPending("Skills reload was invalidated")
        }
    }
}
