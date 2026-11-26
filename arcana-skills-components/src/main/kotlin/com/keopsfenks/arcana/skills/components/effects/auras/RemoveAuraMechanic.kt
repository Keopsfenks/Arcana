package com.keopsfenks.arcana.skills.components.effects.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.auras.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import java.util.concurrent.CompletableFuture
class RemoveAuraMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("remove_aura", "Dispel matching owned aura instances", "name", "aura-type", "tag", "caster", "current")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val matcher = AuraMatcher(arguments, compilation)
        return CompiledSkillMechanic { context -> if (context !is PlatformExecutionContext) AuraSupport.unsupported() else {
            val selected = matcher.select(context)
            selected.forEach { context.state.step(); it.removeFrom(SkillRemovalReason.DISPELLED, context.takeIf { current -> current.state.managedState === it.managed }?.frame) }
            CompletableFuture.allOf(*selected.map { it.finished }.toTypedArray()).thenApply { SkillResult.success() }
        } }
    }
}
