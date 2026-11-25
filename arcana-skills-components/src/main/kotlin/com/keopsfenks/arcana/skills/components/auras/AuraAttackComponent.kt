package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
class AuraAttackComponent : SkillAuraComponent(SkillDescriptor.of("attack", "Owned attack event hook with optional synchronous interception")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = AuraEventComponentSupport.compile(SkillKey("arcana", "attack"), arguments, compilation)
}
