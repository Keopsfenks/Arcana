package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.util.PlayerUtils
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player

class MessageEffect(private val audience: (Player) -> Audience = PlayerUtils::getAudience) : SkillEffect("message") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "text", "target")
        val text = ComponentArguments.nonEmpty(arguments, "text")
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.NO_TARGET, "Message target must be a player")
            else { audience(entity).sendMessage(SkillText.render(text, execution.context)); SkillResult.success() }
        } }
    }
}
