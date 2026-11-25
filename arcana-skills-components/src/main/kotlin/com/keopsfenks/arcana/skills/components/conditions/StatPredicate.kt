package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class StatPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("stat", "Inclusive requested finite stat snapshot bounds; missing keys do not match", "subject", "provider", "key", "min", "max")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val range = ConditionSupport.Range(arguments, compilation, -1_000_000.0, 1_000_000.0)
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.STAT, arguments.string("provider"), mapOf("key" to arguments.string("key")))
        return SkillPredicate { range.test(ConditionSupport.number(it.data()[alias]), it) }
    }
}
