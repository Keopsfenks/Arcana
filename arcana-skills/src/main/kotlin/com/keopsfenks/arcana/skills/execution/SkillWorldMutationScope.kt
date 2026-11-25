package com.keopsfenks.arcana.skills.execution

/** Native protection events remain visible, while Arcana does not admit fresh roots from its own edits. */
object SkillWorldMutationScope {
    private val depth = ThreadLocal.withInitial { 0 }
    fun isActive() = depth.get() > 0
    fun <T> during(action: () -> T): T {
        val previous = depth.get(); depth.set(previous+1)
        try { return action() } finally { if (previous == 0) depth.remove() else depth.set(previous) }
    }
}
