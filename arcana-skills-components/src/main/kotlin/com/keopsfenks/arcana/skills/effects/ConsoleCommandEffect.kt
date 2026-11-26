package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.Bukkit

class ConsoleCommandEffect : SkillEffect("console_command") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "command")
        val command = EffectValues.command(arguments.string("command"))
        return CompiledSkillEffect { execution -> execution.global {
            check(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), EffectValues.command(execution.format(command)))) { "Server rejected the console command" }
        } }
    }
}
