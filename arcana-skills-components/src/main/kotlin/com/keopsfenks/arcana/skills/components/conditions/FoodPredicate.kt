package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class FoodPredicate : SkillPredicateType(SkillDescriptor.of("food", "Inclusive requested player food snapshot bounds")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "min", "max"); val range = ConditionSupport.Range(arguments, compilation, 0.0, 20.0)
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.FOOD)
        return SkillPredicate { range.test(ConditionSupport.number(it.data()[alias]), it) }
    }
}
