package com.keopsfenks.arcana.skills.components.effects.presentation
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import org.bukkit.entity.Player

class PlayerCommandMechanic : SkillMechanic(SkillDescriptor.of("player_command", "Player-only literal command on its owning scheduler")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "command"); val recipient = NativeActions.recipient(arguments); val template = EffectValues.command(NativeText.template(arguments, "command"))
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Player command requires a player") else if (entity.performCommand(NativeText.command(template, context.invocation()))) SkillResult.success() else SkillResult.of(SkillResult.Status.ERROR, "Server rejected player command")
        } }
    }
}
