package com.keopsfenks.arcana.skills.components.effects.combat
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import org.bukkit.entity.Player

class FeedMechanic : SkillMechanic(SkillDescriptor.of("feed", "Player-only bounded food and saturation addition")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "amount", "saturation", "target")
        val recipient = NativeActions.recipient(arguments); val amount = NativeActions.number(arguments, compilation, "amount", 1, 0.0, 20.0, true)
        val saturation = NativeActions.number(arguments, compilation, "saturation", 0, 0.0, 20.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Feed requires a player")
            else EffectValues.alive(entity) { val input = context.invocation(); entity.foodLevel = (entity.foodLevel+amount.evaluate(input).toInt()).coerceIn(0, 20); entity.saturation = (entity.saturation+saturation.evaluate(input)).coerceIn(0.0, entity.foodLevel.toDouble()).toFloat() }
        } }
    }
}
