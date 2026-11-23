package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.skills.SkillContext
import java.util.function.Consumer

/** Bind a Bukkit event to its configuration before the entity-scheduler handoff. */
interface SkillTriggerDispatch : Consumer<SkillContext> {
    fun prepare(context: SkillContext): Runnable?
    override fun accept(context: SkillContext) { prepare(context)?.run() }
}
