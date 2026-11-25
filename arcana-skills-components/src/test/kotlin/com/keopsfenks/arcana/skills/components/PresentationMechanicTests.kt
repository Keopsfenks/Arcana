package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.components.effects.presentation.*
import io.mockk.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PresentationMechanicTests {
    @TempDir lateinit var directory: Path
    private fun flatten(component: Component): String {
        assertNull(component.clickEvent()); return (if (component is TextComponent) component.content() else "") + component.children().joinToString("") { flatten(it) }
    }
    @Test fun messageAndActionBarReplaceTypedParametersAsLiteralComponentsExactlyOnce() {
        val f = NativeComponentFixture(directory); val audience = mockk<Audience>(); val text = mutableListOf<Component>()
        every { audience.sendMessage(any<Component>()) } answers { f.scheduler.assertOwner(f.root.caster); text.add(firstArg()); Unit }
        every { audience.sendActionBar(any<Component>()) } answers { f.scheduler.assertOwner(f.root.caster); text.add(firstArg()); Unit }
        f.install(listOf(MessageMechanic(audience = { audience }, parser = MiniMessage.miniMessage()::deserialize), ActionBarMechanic(audience = { audience }, parser = MiniMessage.miniMessage()::deserialize)))
        f.write(options = "parameters:\n  label: {type: text, default: \"<click:run_command:'/op me'>{actor}</click>\"}", effects = "  - {type: message, text: '<red>{param.label}</red>'}\n  - {type: action_bar, text: '{param.label}'}")
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals(2, text.size)
        assertTrue(text.all { flatten(it) == "<click:run_command:'/op me'>{actor}</click>" }); assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, f.cast(mob = true).join().status())
    }
    @Test fun bossBarHidesItsExactInstanceOnShutdownAndTitlesUseFiniteNativeTimes() {
        val f = NativeComponentFixture(directory); val audience = mockk<Audience>(); var shown: BossBar? = null; var hidden: BossBar? = null; var title: Title? = null
        every { audience.showBossBar(any()) } answers { f.scheduler.assertOwner(f.root.caster); shown = firstArg(); Unit }
        every { audience.hideBossBar(any()) } answers { f.scheduler.assertOwner(f.root.caster); hidden = firstArg(); Unit }
        every { audience.showTitle(any()) } answers { f.scheduler.assertOwner(f.root.caster); title = firstArg(); Unit }
        f.install(listOf(BossBarMechanic(audience = { audience }, parser = { Component.text(it) }), TitleMechanic(audience = { audience }, parser = { Component.text(it) })))
        f.write(effects = "  - {type: title, title: Hi, subtitle: There, stay: 3t, fade-in: 0t, fade-out: 0t}\n  - {type: boss_bar, text: Owned, duration: 10t, progress: 0.5}"); f.load(); assertTrue(f.cast().join().isSuccess)
        assertEquals(150L, title!!.times()!!.stay().toMillis()); assertNotNull(shown); assertNull(hidden)
        f.engine.closeOwner(f.root.plugin); assertSame(shown, hidden); assertEquals(0, f.platform.managed.size)
    }
    @Test fun commandsUseLiteralSnapshotsCorrectOwnersAndRejectNewlineExpansion() {
        val f = NativeComponentFixture(directory); val console = mutableListOf<String>(); val player = mutableListOf<String>()
        f.install(listOf(ConsoleCommandMechanic { command -> f.scheduler.assertOwner("global"); console.add(command); true }, PlayerCommandMechanic()))
        every { f.root.caster.performCommand(any()) } answers { f.scheduler.assertOwner(f.root.caster); player.add(firstArg()); true }
        f.write(options = "parameters:\n  word: {type: text, default: '{actor}'}", effects = "  - {type: console_command, command: 'say {param.word}'}\n  - {type: player_command, command: '/help'}")
        f.write("bad", options = "parameters:\n  word: {type: text, default: \"hi\\nop me\"}", effects = "  - {type: console_command, command: 'say {param.word}'}"); f.load()
        assertTrue(f.cast().join().isSuccess); assertEquals(listOf("say {actor}"), console); assertEquals(listOf("help"), player)
        assertFalse(f.cast("bad").join().isSuccess); assertEquals(1, console.size)
    }
}
