package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class ExpressionPredicate : SkillPredicateType(SkillDescriptor.of("expression", "A compiled finite numeric snapshot expression is nonzero")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "expression"); val expression = compilation.number(arguments.raw("expression"), "${arguments.path}.expression")
        return SkillPredicate { input -> try { expression.evaluate(input).let { it.isFinite() && it != 0.0 } } catch (_: IllegalArgumentException) { false } }
    }
}
