package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.platform.SkillInvocation
import java.util.function.Consumer

/** Selection happens in the callback, before any scheduler handoff. No Event belongs here. */
interface PlatformTriggerDispatch : Consumer<SkillInvocation> {
    fun prepare(input: SkillInvocation): Runnable?
    override fun accept(input: SkillInvocation) { prepare(input)?.run() }
}
