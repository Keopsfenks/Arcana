package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
class AuraDamagedComponent : SkillAuraComponent(SkillDescriptor.of("damaged", "Owned damaged event hook with optional synchronous interception")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = AuraEventComponentSupport.compile(SkillKey("arcana", "damaged"), arguments, compilation)
}
