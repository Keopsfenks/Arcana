package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlatformFixture(directory: Path, immediate: Boolean = true) {
    val legacy = EngineFixture(directory, immediate)
    val engine get() = legacy.engine
    val platform get() = engine.platform()
    val scheduler get() = legacy.scheduler
    val worldId = UUID.randomUUID()
    init {
        every { legacy.plugin.name } returns "Arcana"
        every { legacy.world.uid } returns worldId
        actor(legacy.caster, EntityType.PLAYER)
        actor(legacy.victim, EntityType.ZOMBIE)
    }
    fun actor(entity: LivingEntity, type: EntityType) {
        every { entity.uniqueId } returns if (entity === legacy.caster) legacy.playerId else UUID.randomUUID()
        every { entity.type } answers { scheduler.assertOwner(entity); type }
        every { entity.health } answers { scheduler.assertOwner(entity); 20.0 }
        every { entity.maxHealth } answers { scheduler.assertOwner(entity); 20.0 }
        every { entity.velocity } answers { scheduler.assertOwner(entity); Vector(0.0, 0.0, 1.0) }
        every { entity.world } answers { scheduler.assertOwner(entity); legacy.world }
    }
    fun register(id: String, factory: (SkillArguments, SkillCompilation) -> CompiledSkillMechanic) =
        platform.register(legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", id), object : SkillMechanic(SkillDescriptor.of(id, "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = factory(arguments, compilation)
        })
    fun write(id: String = "native", options: String = "", effects: String = "  - type: native_mark") {
        Files.writeString(legacy.directory.resolve("$id.yml"), "schema: 2\nid: $id\n$options\neffects:\n$effects\n")
    }
    fun invocation(mob: Boolean = false) = SkillInvocation.builder(SkillActor.of(if (mob) legacy.victim else legacy.caster)).build()
    fun cast(id: String = "native", input: SkillInvocation = invocation()) = platform.cast(SkillKey("arcana", id), input).toCompletableFuture()
    fun load() { val result = engine.reload(); check(result.success()) { result.errors().toString() } }
    fun success() = CompletableFuture.completedFuture(SkillResult.success())
}
