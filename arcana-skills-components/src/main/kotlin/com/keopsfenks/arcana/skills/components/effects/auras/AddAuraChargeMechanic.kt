package com.keopsfenks.arcana.skills.components.effects.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.auras.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import java.util.concurrent.CompletableFuture
class AddAuraChargeMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("add_aura_charge", "Change matching aura charges, removing exhausted instances", "name", "aura-type", "tag", "caster", "current", "amount")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val matcher = AuraMatcher(arguments, compilation)
        val amount = NodeOptions.number(arguments, compilation, "amount", 1, -1_000_000.0, 1_000_000.0, integer = true)
        return CompiledSkillMechanic { context -> if (context !is PlatformExecutionContext) AuraSupport.unsupported() else {
            val delta = amount.evaluate(context.invocation()).toInt()
            val work = matcher.select(context).map { context.state.step(); it.addCharges(delta, context) }
            CompletableFuture.allOf(*work.toTypedArray()).thenApply { work.map { it.join() }.firstOrNull { !it.isSuccess } ?: SkillResult.success() }
        } }
    }
}
