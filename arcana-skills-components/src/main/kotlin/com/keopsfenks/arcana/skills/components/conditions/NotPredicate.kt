package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class NotPredicate : SkillPredicateType(SkillDescriptor.of("not", "Invert exactly one nested snapshot condition")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "condition"); val child = compilation.predicate(arguments.section("condition")); return SkillPredicate { !child.test(it) }
    }
}
