package com.keopsfenks.arcana.skills.execution

/** Set only while the synchronous server damage call is on this region's thread. */
object SkillDamageScope {
    private val depth = ThreadLocal.withInitial { 0 }
    fun isActive() = depth.get() > 0
    fun <T> during(action: () -> T): T {
        val previous = depth.get()
        depth.set(previous + 1)
        try { return action() }
        finally { if (previous == 0) depth.remove() else depth.set(previous) }
    }
}
