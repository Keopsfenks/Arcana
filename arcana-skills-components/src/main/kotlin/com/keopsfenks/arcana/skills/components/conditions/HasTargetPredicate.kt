package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class HasTargetPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("has_target", "An explicit target/trigger/actor survives owned snapshot capture", "subject")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val role = ConditionSupport.subject(arguments, fallback = "target"); return SkillPredicate { role.reference(it).isPresent }
    }
}
