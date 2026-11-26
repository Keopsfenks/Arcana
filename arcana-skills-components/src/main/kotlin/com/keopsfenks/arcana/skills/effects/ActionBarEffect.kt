package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.util.PlayerUtils
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player

class ActionBarEffect(private val audience: (Player) -> Audience = PlayerUtils::getAudience) : SkillEffect("action_bar") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "text", "target")
        val text = ComponentArguments.nonEmpty(arguments, "text")
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.NO_TARGET, "Action bar target must be a player")
            else { audience(entity).sendActionBar(SkillText.render(text, execution.context)); SkillResult.success() }
        } }
    }
}
