package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class AnyPredicate : SkillPredicateType(SkillDescriptor.of("any", "At least one nonempty nested snapshot condition must match")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "conditions"); val definitions = arguments.list("conditions"); require(definitions.isNotEmpty()) { "${arguments.path}.conditions: must not be empty" }
        val children = definitions.mapIndexed { index, raw -> compilation.predicate(arguments.child("conditions[$index]", raw)) }
        return SkillPredicate { input -> children.any { it.test(input) } }
    }
}
