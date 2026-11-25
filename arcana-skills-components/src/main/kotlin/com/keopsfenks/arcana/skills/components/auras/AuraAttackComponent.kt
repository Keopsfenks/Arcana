package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
class AuraAttackComponent : SkillAuraComponent(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("attack", "Owned attack event hook with optional synchronous interception", "effects", "interceptors", "conditions", "consume-charge")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = AuraEventComponentSupport.compile(SkillKey("arcana", "attack"), arguments, compilation, descriptor())
}
