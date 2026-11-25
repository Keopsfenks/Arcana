package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class HealthPredicate : SkillPredicateType(SkillDescriptor.of("health", "Inclusive health or health-percent bounds on one explicit role")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "min", "max", "percent"); val role = ConditionSupport.subject(arguments); val percent = arguments.bool("percent", false)
        val range = ConditionSupport.Range(arguments, compilation, 0.0, if (percent) 100.0 else 1_000_000.0)
        return SkillPredicate { input -> val health = ConditionSupport.number(ConditionSupport.data(input, role, "health")); val max = ConditionSupport.number(ConditionSupport.data(input, role, "max_health"))
            range.test(if (!percent) health else if (health != null && max != null && max > 0) health*100/max else null, input)
        }
    }
}
