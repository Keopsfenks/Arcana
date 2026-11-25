package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.auras.AuraInstance
import java.util.concurrent.CompletableFuture
class AuraTickComponent : SkillAuraComponent(SkillDescriptor.of("tick", "Owned periodic aura hook using the existing lifetime budget")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "interval", "effects")
        val interval = AuraSupport.duration(arguments, compilation, "interval", "20t")
        val hooks = AuraSupport.hook(arguments, compilation, "effects")
        require(hooks.isNotEmpty()) { "${arguments.path}.effects: required" }
        return CompiledSkillMechanic { context ->
            val aura = context.services().require(AuraInstance::class.java)
            aura.subscribeTicks(interval.evaluate(context.invocation()).toLong(), hooks)
            CompletableFuture.completedFuture(SkillResult.success())
        }
    }
}
