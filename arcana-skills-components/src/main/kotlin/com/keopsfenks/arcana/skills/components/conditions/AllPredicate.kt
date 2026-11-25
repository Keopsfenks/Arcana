package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class AllPredicate : SkillPredicateType(SkillDescriptor.of("all", "All nonempty nested snapshot conditions must match")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "conditions"); val definitions = arguments.list("conditions"); require(definitions.isNotEmpty()) { "${arguments.path}.conditions: must not be empty" }
        return compilation.predicates(definitions, "${arguments.path}.conditions")
    }
}
