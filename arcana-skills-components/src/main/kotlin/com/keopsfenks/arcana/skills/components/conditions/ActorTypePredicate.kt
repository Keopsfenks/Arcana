package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.Locale
class ActorTypePredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("actor_type", "Match actor/target/trigger player, mob or living snapshot kind", "subject", "kind")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val role = ConditionSupport.subject(arguments); val kind = arguments.string("kind").lowercase(Locale.ROOT)
        require(kind in setOf("player", "mob", "living")) { "${arguments.path}.kind: expected player, mob or living" }
        return SkillPredicate { input -> val player = ConditionSupport.data(input, role, "player")?.takeIf { it.kind() == SkillValue.Kind.BOOLEAN }?.bool(); player != null && (kind == "living" || player == (kind == "player")) }
    }
}
