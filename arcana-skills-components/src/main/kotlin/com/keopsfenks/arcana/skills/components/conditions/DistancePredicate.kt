package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import kotlin.math.sqrt
class DistancePredicate : SkillPredicateType(SkillDescriptor.of("distance", "Inclusive same-world distance bounds between two explicit roles")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "other", "min", "max"); val role = ConditionSupport.subject(arguments); val other = ConditionSupport.subject(arguments, "other", "target"); val range = ConditionSupport.Range(arguments, compilation, 0.0, 30_000_000.0)
        return SkillPredicate { input -> val from = ConditionSupport.location(input, role); val to = ConditionSupport.location(input, other); range.test(if (from == null || to == null) null else sqrt(TargetGeometry.distanceSquared(from, to)), input) }
    }
}
