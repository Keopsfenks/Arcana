package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class SoundEffect : SkillEffect("sound") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "sound", "volume", "pitch", "target")
        val key = arguments.string("sound")
        require(key.matches(Regex("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+"))) { "${arguments.path}.sound: expected a resource key such as minecraft:entity.player.levelup" }
        val sound = if (':' in key) key else "minecraft:$key"
        val volume = ComponentArguments.number(arguments, "volume", compiler, 0.0, 100.0, 1.0)
        val pitch = ComponentArguments.number(arguments, "pitch", compiler, 0.5, 2.0, 1.0)
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.atTarget(target) { location ->
            location.world?.playSound(location, sound, volume.evaluate(execution.context).toFloat(), pitch.evaluate(execution.context).toFloat())
            SkillResult.success()
        } }
    }
}
