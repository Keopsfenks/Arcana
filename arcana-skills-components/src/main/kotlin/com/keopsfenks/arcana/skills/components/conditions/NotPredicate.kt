package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class NotPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("not", "Invert exactly one nested snapshot condition", "condition")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val child = compilation.predicate(arguments.section("condition")); return SkillPredicate { !child.test(it) }
    }
}
