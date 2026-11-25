package com.keopsfenks.arcana.skills.components.effects.presentation
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class ActionBarMechanic(private val audience: (Player) -> Audience = PlayerUtils::getAudience, private val parser: (String) -> Component = StringUtils::formatToRichComponent) : SkillMechanic(SkillDescriptor.of("action_bar", "Player-only action bar with literal snapshot replacements")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "text", "target"); val recipient = NativeActions.recipient(arguments); val text = NativeText.template(arguments, "text")
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Action bar requires a player") else { audience(entity).sendActionBar(NativeText.render(text, context.invocation(), parser)); SkillResult.success() }
        } }
    }
}
