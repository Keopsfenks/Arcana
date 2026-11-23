package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

class EngineFixture(val directory: Path, immediate: Boolean = true) {
    val scheduler = ControlledSkillScheduler(immediate)
    val clock = AtomicLong(0)
    var roll = 0.0
    val plugin = mockk<ArcanaPlugin>(relaxed = true)
    val caster = mockk<Player>()
    val victim = mockk<LivingEntity>()
    val world = mockk<World>()
    val playerId = UUID.randomUUID()
    val marks = mutableListOf<String>()
    val engine: DefaultSkillEngine

    init {
        every { plugin.logger } returns Logger.getLogger("SkillsTest")
        every { caster.uniqueId } returns playerId
        every { caster.name } answers { scheduler.assertOwner(caster); "Caster" }
        every { caster.isOnline } answers { scheduler.assertOwner(caster); true }
        every { caster.isValid } returns true
        every { caster.isDead } returns false
        every { caster.health } answers { scheduler.assertOwner(caster); 20.0 }
        every { caster.maxHealth } returns 20.0
        every { caster.foodLevel } returns 20
        every { caster.level } returns 3
        every { caster.isSneaking } returns false
        every { caster.isSprinting } returns false
        every { caster.hasPermission(any<String>()) } returns true
        every { world.name } returns "world"
        every { caster.world } returns world
        every { caster.location } answers { scheduler.assertOwner(caster); Location(world, 0.0, 60.0, 0.0) }
        val inventory = mockk<PlayerInventory>()
        val item = mockk<ItemStack>()
        every { item.type } returns Material.DIAMOND_SWORD
        every { inventory.itemInMainHand } returns item
        every { caster.inventory } returns inventory
        every { victim.name } answers { scheduler.assertOwner(victim); "Victim" }
        every { victim.type } answers { scheduler.assertOwner(victim); EntityType.ZOMBIE }
        every { victim.isValid } answers { scheduler.assertOwner(victim); true }
        every { victim.isDead } returns false
        every { victim.location } answers { scheduler.assertOwner(victim); Location(world, 3.0, 60.0, 0.0) }
        engine = DefaultSkillEngine(plugin, directory, CompilerFixture().environment, scheduler, clock::get, random = { roll })
        register("mark") { arguments, _ -> CompiledSkillEffect {
            marks.add(arguments.string("name", "mark"))
            CompletableFuture.completedFuture(SkillResult.success())
        } }
        register("wait") { _, _ -> CompiledSkillEffect { it.delay(1) } }
        engine.registerTrigger(plugin, object : SkillTrigger("attack") {
            override fun listen(fire: java.util.function.Consumer<SkillContext>) = AutoCloseable {}
        })
    }

    fun register(id: String, factory: (SkillArguments, SkillCompiler) -> CompiledSkillEffect) =
        engine.registerEffect(plugin, object : SkillEffect(id) {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) = factory(arguments, compiler)
        })

    fun write(id: String = "one", options: String = "", effects: String = "  - type: mark") {
        Files.writeString(directory.resolve("$id.yml"), "id: $id\n$options\neffects:\n$effects\n")
    }
    fun load() = check(engine.reload().success()) { engine.reload().errors().toString() }
    fun context(target: Boolean = false) = SkillContext.builder(caster).victim(if (target) victim else null).build()
    fun cast(id: String = "one", target: Boolean = false) = engine.cast(id, context(target)).toCompletableFuture()
}
