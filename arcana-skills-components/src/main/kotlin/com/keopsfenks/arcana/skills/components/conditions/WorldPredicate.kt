package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class WorldPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("world", "Match an explicit role's captured world name", "subject", "worlds")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val role = ConditionSupport.subject(arguments); val worlds = ConditionSupport.strings(arguments, "worlds").toSet()
        return SkillPredicate { input -> ConditionSupport.data(input, role, "world")?.takeIf { it.kind() == SkillValue.Kind.TEXT }?.text() in worlds }
    }
}
