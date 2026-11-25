package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class LineOfSightPredicate : SkillPredicateType(SkillDescriptor.of("line_of_sight", "Match an owner-captured eye-to-eye loaded block segment")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "other", "max-distance"); val other = ConditionSupport.subject(arguments, "other", "target")
        val distance = arguments.number("max-distance", 256.0).also { require(it.isFinite() && it in .001..256.0) { "${arguments.path}.max-distance: expected .001..256" } }
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.LINE_OF_SIGHT, other.name, mapOf("max-distance" to distance))
        return SkillPredicate { it.data()[alias]?.takeIf { value -> value.kind() == SkillValue.Kind.BOOLEAN }?.bool() == true }
    }
}
