package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
class AuraDamagedComponent : SkillAuraComponent(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("damaged", "Owned damaged event hook with optional synchronous interception", "effects", "interceptors", "conditions", "consume-charge")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = AuraEventComponentSupport.compile(SkillKey("arcana", "damaged"), arguments, compilation, descriptor())
}
