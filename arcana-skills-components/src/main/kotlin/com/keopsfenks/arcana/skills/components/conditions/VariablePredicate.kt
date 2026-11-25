package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.flow.FlowSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
class VariablePredicate : SkillPredicateType(SkillDescriptor.of("variable", "Compare a declared typed variable binding or numeric inclusive range")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "binding", "equals", "min", "max"); val alias = arguments.string("binding")
        require(alias.matches(Regex("var_[a-z0-9_]{1,100}"))) { "${arguments.path}.binding: expected a declared var_ alias" }
        val equality = if (arguments.has("equals")) FlowSupport.parameter(arguments.raw("equals"), compilation, "${arguments.path}.equals") else null
        require(equality == null || !arguments.has("min") && !arguments.has("max")) { "${arguments.path}: equals and numeric range cannot be combined" }
        val range = if (equality == null) ConditionSupport.Range(arguments, compilation, -1_000_000.0, 1_000_000.0) else null
        return SkillPredicate { input -> val actual = input.data()[alias]; actual != null && (if (equality != null) actual == equality(input) else range!!.test(ConditionSupport.number(actual), input)) }
    }
}
