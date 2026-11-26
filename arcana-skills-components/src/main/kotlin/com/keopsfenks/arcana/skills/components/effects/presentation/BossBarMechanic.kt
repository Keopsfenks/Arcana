package com.keopsfenks.arcana.skills.components.effects.presentation
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.*
import com.keopsfenks.arcana.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.CompletableFuture

class BossBarMechanic(private val audience: (Player) -> Audience = PlayerUtils::getAudience, private val parser: (String) -> Component = StringUtils::formatToRichComponent) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("boss_bar", "Show one uniquely owned finite player boss bar", "text", "target", "duration", "progress", "color", "overlay")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val recipient = NativeActions.recipient(arguments); val text = NativeText.template(arguments, "text"); val duration = AuraSupport.duration(arguments, compilation, "duration", "3s")
        val progress = NativeActions.number(arguments, compilation, "progress", 1, 0.0, 1.0)
        val color = try { BossBar.Color.valueOf(arguments.string("color", "PURPLE").uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { throw arguments.error("color", "unknown boss bar color") }
        val overlay = try { BossBar.Overlay.valueOf(arguments.string("overlay", "PROGRESS").uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { throw arguments.error("overlay", "unknown boss bar overlay") }
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Boss bar recipient missing"))
            else if (actor.player().isEmpty) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Boss bar requires a player"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Boss bar requires Arcana managed execution"))
            else {
                val bar = BossBar.bossBar(NativeText.render(text, input, parser), progress.evaluate(input).toFloat(), color, overlay)
                val cleanup = context.services().require(SkillCleanup::class.java)
                val show = CompiledSkillMechanic { current ->
                    current.services().require(ManagedExecutionState::class.java).own(AutoCloseable { cleanup.on(actor) { entity -> audience(entity as Player).hideBossBar(bar); SkillResult.success() } })
                    current.on(actor) { entity -> audience(entity as Player).showBossBar(bar); SkillResult.success() }
                }
                ManagedPulse.start(context, duration.evaluate(input).toLong(), 20, listOf(show), recipient = actor).thenApply { SkillResult.success() }
            }
        }
    }
}
