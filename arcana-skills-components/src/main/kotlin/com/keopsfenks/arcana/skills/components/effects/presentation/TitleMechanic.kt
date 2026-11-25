package com.keopsfenks.arcana.skills.components.effects.presentation
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

class TitleMechanic(private val audience: (Player) -> Audience = PlayerUtils::getAudience, private val parser: (String) -> Component = StringUtils::formatToRichComponent) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("title", "Player-only title with finite native client timings", "title", "subtitle", "target", "fade-in", "stay", "fade-out")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val recipient = NativeActions.recipient(arguments); val title = NativeText.template(arguments, "title"); val subtitle = arguments.string("subtitle", "").also { require(it.length <= 8192) { "${arguments.path}.subtitle: maximum 8192 characters" } }
        val fadeIn = NativeText.duration(arguments, compilation, "fade-in", "10t"); val stay = NativeText.duration(arguments, compilation, "stay", "70t"); val fadeOut = NativeText.duration(arguments, compilation, "fade-out", "20t")
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Title requires a player") else {
                val input = context.invocation(); val start = fadeIn.evaluate(input).toLong(); val show = stay.evaluate(input).toLong(); val end = fadeOut.evaluate(input).toLong()
                require(start+show+end <= 1_728_000) { "${arguments.path}.stay: total title time exceeds 24 hours" }
                audience(entity).showTitle(Title.title(NativeText.render(title, input, parser), NativeText.render(subtitle, input, parser), Title.Times.times(Duration.ofMillis(start*50), Duration.ofMillis(show*50), Duration.ofMillis(end*50))))
                SkillResult.success()
            }
        } }
    }
}
