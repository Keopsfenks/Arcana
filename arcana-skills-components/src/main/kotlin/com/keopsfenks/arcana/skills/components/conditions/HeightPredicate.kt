package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class HeightPredicate : SkillPredicateType(SkillDescriptor.of("height", "Inclusive height bounds on one explicit role")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "min", "max"); val role = ConditionSupport.subject(arguments); val range = ConditionSupport.Range(arguments, compilation, -30_000_000.0, 30_000_000.0)
        return SkillPredicate { range.test(ConditionSupport.location(it, role)?.y(), it) }
    }
}
