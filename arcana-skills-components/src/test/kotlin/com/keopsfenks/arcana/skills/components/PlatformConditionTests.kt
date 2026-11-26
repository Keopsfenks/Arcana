package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.conditions.*
import com.keopsfenks.arcana.skills.components.effects.variables.SetVariableMechanic
import com.keopsfenks.arcana.skills.components.effects.auras.ApplyAuraMechanic
import io.mockk.*
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.util.*
import org.bukkit.util.Vector
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class PlatformConditionTests {
    @TempDir lateinit var directory: Path
    private val fixtures = mutableListOf<NativeComponentFixture>()
    @BeforeEach fun worldResolver() { mockkStatic(Bukkit::class) }
    @AfterEach fun cleanup() { fixtures.forEach { it.engine.shutdown() }; unmockkStatic(Bukkit::class) }
    private fun fixture() = NativeComponentFixture(directory).also { f ->
        fixtures.add(f)
        val types = listOf(AllPredicate(), AnyPredicate(), NotPredicate(), ExpressionPredicate(), ActorTypePredicate(), PermissionPredicate(), HealthPredicate(), FoodPredicate(), MovementPredicate(), WorldPredicate(), BiomePredicate(), TimePredicate(), WeatherPredicate(), HeightPredicate(), DistancePredicate(), LineOfSightPredicate(), EntityTypePredicate(), HasTargetPredicate(), EquipmentPredicate(), VariablePredicate(), ResourcePredicate(), StatPredicate(), HasAuraPredicate())
        assertEquals(23, types.map { it.descriptor().id() }.toSet().size)
        types.forEach { f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", it.descriptor().id()), it) }
        every { Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.root.world }
        every { f.root.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); true }
    }
    @Test fun logicalTruthTablesAndEmptyNestedDefinitionsAreValidated() {
        val f = fixture()
        val cases = listOf("{type: all, conditions: [{type: expression, expression: 1}, {type: not, condition: {type: expression, expression: 0}}]}" to true,
            "{type: any, conditions: [{type: expression, expression: 0}, {type: expression, expression: 1}]}" to true,
            "{type: all, conditions: [{type: expression, expression: 1}, {type: expression, expression: 0}]}" to false)
        for ((condition, expected) in cases) {
            f.write(options = "conditions: [$condition]", effects = "  - type: mark"); f.load(); assertEquals(expected, f.cast().join().isSuccess)
        }
        for (condition in listOf("{type: all, conditions: []}", "{type: any, conditions: []}", "{type: not, conditions: []}", "{type: all, conditions: [{type: missing}]}")) {
            f.write(options = "conditions: [$condition]", effects = "  - type: mark"); val report = f.engine.reload(); assertFalse(report.success()); assertTrue(report.errors().joinToString().contains("conditions[0]"))
        }
    }
    @Test fun actorTargetAndTriggerNumericBoundsStayDistinctAndPredicatesReadNoLiveState() {
        val f = fixture(); val third = mockk<LivingEntity>()
        every { third.uniqueId } returns UUID.randomUUID(); every { third.isValid } answers { f.scheduler.assertOwner(third); true }
        every { third.name } answers { f.scheduler.assertOwner(third); "Third" }; every { third.type } answers { f.scheduler.assertOwner(third); EntityType.SKELETON }
        every { third.health } answers { f.scheduler.assertOwner(third); 5.0 }; every { third.maxHealth } answers { f.scheduler.assertOwner(third); 10.0 }
        every { third.velocity } answers { f.scheduler.assertOwner(third); Vector() }; every { third.world } answers { f.scheduler.assertOwner(third); f.root.world }
        every { third.location } answers { f.scheduler.assertOwner(third); Location(f.root.world, 0.0, 68.0, 0.0) }
        every { f.root.victim.health } answers { f.scheduler.assertOwner(f.root.victim); 7.0 }
        f.write(options = """
          conditions:
            - {type: health, subject: actor, min: 20, max: 20}
            - {type: health, subject: target, min: 7, max: 7}
            - {type: health, subject: trigger, min: 5, max: 5}
            - {type: height, subject: trigger, min: 68, max: 68}
            - {type: distance, subject: actor, other: target, min: 3, max: 3}
            - {type: actor_type, kind: player}
            - {type: entity_type, subject: trigger, types: [skeleton]}
            - {type: has_target}
        """.trimIndent(), effects = "  - type: mark")
        f.load(); val input = f.invocation().derive().target(SkillActor.of(f.root.victim)).trigger(SkillActor.of(third)).build()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
        f.register("pure_probe") { _, compiler -> val predicate = compiler.predicate(SkillArguments(mapOf("type" to "health", "min" to 20), "pure")); CompiledSkillMechanic { context ->
            val snapshot = context.invocation(); every { f.root.caster.health } throws IllegalStateException("Predicate performed a live read")
            assertTrue(predicate.test(snapshot)); f.success()
        } }
        f.write("pure", effects = "  - type: pure_probe"); f.load(); assertTrue(f.cast("pure").join().isSuccess)
    }
    @Test fun permissionReadsOnlyTheRequestedNodeAndNeverGrantsMobOrSpoofedSnapshots() {
        val f = fixture(); var reads = 0
        every { f.root.caster.hasPermission(any<String>()) } answers { f.scheduler.assertOwner(f.root.caster); assertEquals("arcana.use", firstArg<String>()); reads++; true }
        f.write(options = "conditions: [{type: permission, permission: arcana.use}]", effects = "  - type: mark"); f.load()
        assertTrue(f.cast().join().isSuccess); assertTrue(reads >= 1)
        val spoof = f.invocation(true).derive().data(mapOf("arcana_snapshot_0" to true)).build()
        assertEquals(SkillResult.Status.CONDITION_FAILED, f.platform.cast(SkillKey("arcana", "one"), spoof).toCompletableFuture().join().status())
        verify(exactly = 0) { f.root.caster.effectivePermissions }; verify(exactly = 0) { f.root.victim.hasPermission(any<String>()) }
    }
    @Test fun worldTimeAndWeatherAreCapturedOnlyWhenRequestedOnTheirOwners() {
        val f = fixture()
        every { f.root.world.time } answers { f.scheduler.assertOwner("global"); 6500L }
        every { f.root.world.hasStorm() } answers { f.scheduler.assertOwner("global"); true }
        every { f.root.world.isThundering } answers { f.scheduler.assertOwner("global"); true }
        f.write(options = "conditions: [{type: health, min: 1}]", effects = "  - type: mark"); f.load(); assertTrue(f.cast().join().isSuccess)
        verify(exactly = 0) { f.root.world.time }
        f.write(options = "conditions: [{type: world, worlds: [world]}, {type: time, min: 6500, max: 6500}, {type: weather, states: [thunder]}]", effects = "  - type: mark"); f.load(); assertTrue(f.cast().join().isSuccess)
    }
    @Test fun movementEquipmentAndEnumParsingRespectCapabilitiesAndLocaleRoot() {
        val f = fixture(); val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            every { f.root.caster.isSneaking } answers { f.scheduler.assertOwner(f.root.caster); true }
            val hand = f.root.caster.inventory.itemInMainHand; every { hand.amount } returns 1
            f.write(options = "conditions: [{type: movement, state: sneaking}, {type: equipment, slot: main_hand, materials: [diamond_sword]}, {type: food, min: 20, max: 20}]", effects = "  - type: mark"); f.load(); val outcome = f.cast().join(); assertTrue(outcome.isSuccess, outcome.toString())
            assertEquals(SkillResult.Status.CONDITION_FAILED, f.cast(mob = true).join().status())
        } finally { Locale.setDefault(before) }
    }
    @Test fun variableResourceAndStatValuesStayTypedAndMissingStatDataFailsTheCondition() {
        val f = fixture(); f.install(listOf(SetVariableMechanic())); var stats = mapOf(SkillKey("arcana", "power") to 7.0)
        f.platform.register(f.root.plugin, SkillComponentType.RESOURCE, SkillKey("arcana", "energy"), object : SkillResourceProvider {
            override fun descriptor() = SkillDescriptor.of("energy", "Read-only test resource")
            override fun reserve(actor: SkillActor, amount: Double) = CompletableFuture.completedFuture(SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Read only")))
            override fun balance(actor: SkillActor) = CompletableFuture.completedFuture(4.0).also { f.scheduler.assertOwner(actor.entity()) }
        })
        f.platform.register(f.root.plugin, SkillComponentType.STAT, SkillKey("arcana", "test_stats"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("test_stats", "Selected test stat")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(stats).also { f.scheduler.assertOwner(actor.entity()); assertEquals(setOf(SkillKey("arcana", "power")), keys) }
        })
        f.write("set", effects = "  - {type: set_variable, scope: actor, key: mode, value: ready}")
        f.write(options = "bindings:\n  var_mode: {scope: actor, key: mode, type: text, default: unset}\nconditions: [{type: variable, binding: var_mode, equals: ready}, {type: resource, resource: energy, min: 4, max: 4}, {type: stat, provider: test_stats, key: power, min: 7, max: 7}]", effects = "  - type: mark")
        f.load(); assertFalse(f.cast().join().isSuccess); f.cast("set").join(); assertTrue(f.cast().join().isSuccess)
        stats = emptyMap(); assertEquals(SkillResult.Status.CONDITION_FAILED, f.cast().join().status())
        f.write(options = "conditions: [{type: resource, resource: missing}]", effects = "  - type: mark"); assertFalse(f.engine.reload().success())
    }
    @Test fun lineOfSightCapturesEyesAndSweptBlocksWithoutForeignWorldReads() {
        val f = fixture(); var blocked = false; val chunk = mockk<Chunk>()
        every { chunk.entities } answers { assertTrue(f.scheduler.current is Location); emptyArray() }
        every { f.root.world.getChunkAt(any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); chunk }
        every { f.root.world.getNearbyEntities(any<BoundingBox>()) } answers { assertTrue(f.scheduler.current is Location); listOf(f.root.caster, f.root.victim) }
        every { f.root.world.minHeight } returns 0; every { f.root.world.maxHeight } returns 256
        every { f.root.caster.eyeLocation } answers { f.scheduler.assertOwner(f.root.caster); Location(f.root.world, 0.0, 61.62, 0.0) }
        every { f.root.victim.eyeLocation } answers { f.scheduler.assertOwner(f.root.victim); Location(f.root.world, 3.0, 61.62, 0.0) }
        val air = mockk<org.bukkit.block.Block>(); val wall = mockk<org.bukkit.block.Block>(); val empty = mockk<VoxelShape>(); val solid = mockk<VoxelShape>()
        every { empty.boundingBoxes } returns emptyList(); every { solid.boundingBoxes } returns listOf(BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        every { air.collisionShape } returns empty; every { wall.collisionShape } returns solid
        every { f.root.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); if (blocked && firstArg<Int>() == 1) wall else air }
        f.write(options = "conditions: [{type: line_of_sight}]", effects = "  - type: mark"); f.load(); val input = f.invocation().derive().target(SkillActor.of(f.root.victim)).build()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
        blocked = true; assertEquals(SkillResult.Status.CONDITION_FAILED, f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().status())
        verify(exactly = 0) { f.root.world.entities }
    }
    @Test fun auraMetadataFiltersByIdentityStacksAndChargesAndRetirementClearsIt() {
        val f = fixture(); f.install(listOf(ApplyAuraMechanic()))
        f.write("buff", effects = "  - {type: apply_aura, name: power, tags: [magic], duration: 100t, stack-limit: 2, charges: 3}")
        f.write(options = "conditions: [{type: has_aura, name: power, tag: magic, min-stacks: 2, min-charges: 3}]", effects = "  - type: mark")
        f.load(); assertFalse(f.cast().join().isSuccess); f.cast("buff").join(); assertFalse(f.cast().join().isSuccess)
        f.cast("buff").join(); assertTrue(f.cast().join().isSuccess); f.engine.cancel(f.root.playerId); assertFalse(f.cast().join().isSuccess)
    }
    @Test fun candidateFiltersCapturePermissionsForTheActualSelectedActor() {
        val f = fixture(); f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "entities_in_radius"), com.keopsfenks.arcana.skills.components.targeters.EntitiesInRadiusTargeter())
        val chunk = mockk<Chunk>(); every { chunk.entities } answers { assertTrue(f.scheduler.current is Location); arrayOf(f.root.caster, f.root.victim) }
        every { f.root.world.getChunkAt(any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); chunk }
        every { f.root.world.getNearbyEntities(any<BoundingBox>()) } answers { assertTrue(f.scheduler.current is Location); listOf(f.root.caster, f.root.victim) }
        val selected = mutableListOf<SkillActor>()
        f.register("record") { _, _ -> CompiledSkillMechanic { current -> selected.add(current.invocation().target().orElseThrow()); f.success() } }
        f.write(effects = "  - {type: record, targeter: {type: entities_in_radius, radius: 8, conditions: [{type: permission, subject: target, permission: arcana.use}]}}")
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals(listOf(SkillActor.of(f.root.caster)), selected)
    }
    @Test fun locationFiltersRemoveVictimMetadataButKeepActorPermissionMetadata() {
        val f = fixture(); f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "origin"), com.keopsfenks.arcana.skills.components.targeters.OriginTargeter())
        val input = f.invocation().derive().target(SkillActor.of(f.root.caster)).build()
        f.write(effects = "  - {type: mark, targeter: {type: origin, conditions: [{type: permission, subject: target, permission: arcana.use}]}}")
        f.load(); assertEquals(SkillResult.Status.NO_TARGET, f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().status())
        f.write(effects = "  - {type: mark, targeter: {type: origin, conditions: [{type: permission, subject: actor, permission: arcana.use}]}}")
        f.load(); assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
    }
    @Test fun localInterceptionCapturesItsPermissionWithoutFetchingNormalOnlyBiomeData() {
        val f = fixture(); f.platform.register(f.root.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "cancel_event"), com.keopsfenks.arcana.skills.components.interceptors.CancelEventInterceptor())
        f.write(options = "conditions: [{type: permission, permission: arcana.use}]\ninterceptors: [{type: cancel_event}]", effects = "  - {type: mark, conditions: [{type: biome, biomes: [plains]}]}")
        f.load(); val event = mockk<org.bukkit.event.entity.EntityDamageEvent>(); var cancelled = false
        every { event.isCancelled } answers { cancelled }; every { event.isCancelled = any() } answers { cancelled = firstArg(); Unit }; every { event.damage } returns 1.0
        val outcome = f.scheduler.on(f.root.caster) { f.platform.interceptors.intercept(SkillKey("arcana", "one"), f.invocation(), event) }.future.join()
        assertTrue(outcome.result.isSuccess, outcome.result.toString()); assertTrue(cancelled); outcome.continuation?.close()
        f.write(options = "conditions: [{type: biome, biomes: [plains]}]\ninterceptors: [{type: cancel_event}]", effects = "  - type: mark"); assertFalse(f.engine.reload().success())
    }
    @Test fun unusedTemplatesValidateButDoNotFetchTheirRuntimeSnapshotRequirements() {
        val f = fixture()
        every { f.root.caster.hasPermission(any<String>()) } throws IllegalStateException("Unused template snapshot was read")
        f.write(options = "templates:\n  examples:\n    unused:\n      type: mark\n      conditions: [{type: permission, permission: never.read}]", effects = "  - type: mark")
        f.load(); val result = f.cast().join(); assertTrue(result.isSuccess, result.toString())
    }
    @Test fun candidateAndLocationFiltersRebindTargetVariablesInsteadOfInheritingTheOldVictim() {
        val f = fixture(); f.install(listOf(SetVariableMechanic()))
        f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "entities_in_radius"), com.keopsfenks.arcana.skills.components.targeters.EntitiesInRadiusTargeter())
        f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "origin"), com.keopsfenks.arcana.skills.components.targeters.OriginTargeter())
        val chunk = mockk<Chunk>(); every { chunk.entities } answers { assertTrue(f.scheduler.current is Location); arrayOf(f.root.caster, f.root.victim) }
        every { f.root.world.getChunkAt(any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); chunk }
        every { f.root.world.getNearbyEntities(any<BoundingBox>()) } answers { assertTrue(f.scheduler.current is Location); listOf(f.root.caster, f.root.victim) }
        val selected = mutableListOf<SkillActor>()
        f.register("record") { _, _ -> CompiledSkillMechanic { current -> selected.add(current.invocation().target().orElseThrow()); f.success() } }
        f.write("set", effects = "  - {type: set_variable, scope: target, key: flag, value: true}")
        val binding = "bindings:\n  var_flag: {scope: target, key: flag, type: boolean, default: false}"
        f.write(options = binding, effects = "  - {type: record, targeter: {type: entities_in_radius, radius: 8, conditions: [{type: variable, binding: var_flag, equals: true}]}}")
        f.load(); val input = f.invocation().derive().target(SkillActor.of(f.root.caster)).build()
        f.platform.cast(SkillKey("arcana", "set"), input).toCompletableFuture().join()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess); assertEquals(listOf(SkillActor.of(f.root.caster)), selected)
        f.write(options = binding, effects = "  - {type: mark, targeter: {type: origin, conditions: [{type: variable, binding: var_flag, equals: true}]}}")
        f.load(); assertEquals(SkillResult.Status.NO_TARGET, f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().status())
    }
}
