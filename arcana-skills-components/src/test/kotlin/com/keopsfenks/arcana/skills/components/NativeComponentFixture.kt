package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture

class NativeComponentFixture(directory: Path, expressionEnvironmentFactory: ((Array<String>) -> com.keopsfenks.arcana.core.math.ExpressionEnvironment)? = null) {
    val root = EngineFixture(directory, expressionEnvironmentFactory = expressionEnvironmentFactory)
    val engine get() = root.engine
    val platform get() = engine.platform()
    val scheduler get() = root.scheduler
    val marks get() = root.marks
    val worldId = UUID.randomUUID()
    init {
        every { root.plugin.name } returns "Arcana"
        every { root.world.uid } returns worldId
        actor(root.caster, EntityType.PLAYER); actor(root.victim, EntityType.ZOMBIE)
        register("mark") { args, _ -> val name = args.string("name", "mark"); CompiledSkillMechanic { marks.add(name); success() } }
    }
    private fun actor(entity: LivingEntity, type: EntityType) {
        every { entity.uniqueId } returns if (entity === root.caster) root.playerId else UUID.randomUUID()
        every { entity.type } answers { scheduler.assertOwner(entity); type }
        every { entity.health } answers { scheduler.assertOwner(entity); 20.0 }
        every { entity.maxHealth } answers { scheduler.assertOwner(entity); 20.0 }
        every { entity.velocity } answers { scheduler.assertOwner(entity); Vector(0.0, 0.0, 1.0) }
        every { entity.world } answers { scheduler.assertOwner(entity); root.world }
        every { entity.boundingBox } answers { scheduler.assertOwner(entity); val point = entity.location; BoundingBox(point.x - .3, point.y, point.z - .3, point.x + .3, point.y + 1.8, point.z + .3) }
    }
    fun register(id: String, factory: (SkillArguments, SkillCompilation) -> CompiledSkillMechanic) = platform.register(root.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", id), object : SkillMechanic(SkillDescriptor.of(id, "test")) {
        override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = factory(arguments, compilation)
    })
    fun install(mechanics: List<SkillMechanic>) { mechanics.forEach { platform.register(root.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", it.descriptor().id()), it) } }
    fun write(id: String = "one", options: String = "", effects: String) { Files.writeString(root.directory.resolve("$id.yml"), "schema: 2\nid: $id\n$options\neffects:\n$effects\n") }
    fun load() { val report = engine.reload(); check(report.success()) { report.errors().toString() } }
    fun invocation(mob: Boolean = false) = SkillInvocation.builder(SkillActor.of(if (mob) root.victim else root.caster)).build()
    fun cast(id: String = "one", mob: Boolean = false) = platform.cast(SkillKey("arcana", id), invocation(mob)).toCompletableFuture()
    fun success() = CompletableFuture.completedFuture(SkillResult.success())
}
