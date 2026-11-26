package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class HeightPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("height", "Inclusive height bounds on one explicit role", "subject", "min", "max")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val role = ConditionSupport.subject(arguments); val range = ConditionSupport.Range(arguments, compilation, -30_000_000.0, 30_000_000.0)
        return SkillPredicate { range.test(ConditionSupport.location(it, role)?.y(), it) }
    }
}
