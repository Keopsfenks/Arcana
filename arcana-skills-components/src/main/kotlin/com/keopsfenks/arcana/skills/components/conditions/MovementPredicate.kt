package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.Locale
class MovementPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("movement", "Match one owner-captured movement state", "subject", "state", "value")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val state = arguments.string("state").lowercase(Locale.ROOT)
        require(state in setOf("sneaking", "sprinting", "swimming", "gliding", "on_ground")) { "${arguments.path}.state: unknown movement state" }
        val expected = arguments.bool("value", true); val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.MOVEMENT, state)
        return SkillPredicate { it.data()[alias]?.takeIf { value -> value.kind() == SkillValue.Kind.BOOLEAN }?.bool() == expected }
    }
}
