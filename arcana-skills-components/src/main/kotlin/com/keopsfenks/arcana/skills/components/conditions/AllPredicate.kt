package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class AllPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("all", "All nonempty nested snapshot conditions must match", "conditions")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val definitions = arguments.list("conditions"); require(definitions.isNotEmpty()) { "${arguments.path}.conditions: must not be empty" }
        return compilation.predicates(definitions, "${arguments.path}.conditions")
    }
}
