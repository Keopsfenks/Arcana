package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
class TimePredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("time", "Inclusive day-time ticks, wrapping midnight when min exceeds max", "subject", "min", "max")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val min = NativeActions.number(arguments, compilation, "min", 0, 0.0, 23999.0, true); val max = NativeActions.number(arguments, compilation, "max", 23999, 0.0, 23999.0, true)
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.TIME)
        return SkillPredicate { input -> val time = ConditionSupport.number(input.data()[alias]); if (time == null) false else { val lo = min.evaluate(input); val hi = max.evaluate(input); if (lo <= hi) time in lo..hi else time >= lo || time <= hi } }
    }
}
