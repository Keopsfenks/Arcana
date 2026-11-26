package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlayerCommandEffect : SkillEffect("player_command") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "command", "target")
        val command = EffectValues.command(arguments.string("command"))
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.NO_TARGET, "Command target must be a player")
            else if (Bukkit.dispatchCommand(entity, EffectValues.command(execution.format(command)))) SkillResult.success()
            else SkillResult.of(SkillResult.Status.ERROR, "Server rejected the player command")
        } }
    }
}
