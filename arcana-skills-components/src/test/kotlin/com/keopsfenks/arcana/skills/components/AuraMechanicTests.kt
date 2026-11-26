package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.auras.*
import com.keopsfenks.arcana.skills.components.auras.*
import com.keopsfenks.arcana.skills.components.interceptors.*
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import io.mockk.*
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AuraMechanicTests {
    @TempDir lateinit var directory: Path
    private fun fixture() = NativeComponentFixture(directory) { names ->
        mockk<com.keopsfenks.arcana.core.math.ExpressionEnvironment>().also { environment ->
            every { environment.compileOrThrow(any()) } answers {
                val index = names.indexOf(firstArg<String>())
                require(index >= 0) { "This fixture accepts one named numeric slot" }
                object : com.keopsfenks.arcana.core.math.CompiledExpression {
                    override fun evaluate(vararg values: Double) = values[index]
                    override fun evaluate(random: com.keopsfenks.arcana.core.math.RandomSource, vararg values: Double) = evaluate(*values)
                    override fun getVariableCount() = names.size
                    override fun isDeterministic() = true
                }
            }
        }
    }.also { f ->
        f.install(listOf(ApplyAuraMechanic(), RemoveAuraMechanic(), AddAuraChargeMechanic()))
        listOf(AuraTickComponent(), AuraAttackComponent(), AuraDamagedComponent(), AuraStatComponent()).forEach { component -> f.platform.register(f.root.plugin, SkillComponentType.AURA_COMPONENT, SkillKey("arcana", component.descriptor().id()), component) }
        f.platform.register(f.root.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "cancel_event"), CancelEventInterceptor())
    }
    @Test fun inlineAndNamedHooksRetainParametersAndTimedComponentsUseTheSameOwnedAura() {
        val f = fixture(); var endPower = -1.0
        f.register("end_mark") { _, _ -> CompiledSkillMechanic { endPower = it.invocation().parameters().getValue("power").number(); f.success() } }
        f.write("end", options = "parameters:\n  power: {type: number}", effects = "  - type: end_mark")
        f.write(options = "parameters:\n  power: {type: number, default: 7}", effects = """
          - type: apply_aura
            name: pulse
            duration: 5t
            period: 2t
            on-start: [{type: mark, name: start}]
            on-end: {skill: end}
            components:
              - type: tick
                interval: 2t
                effects: [{type: mark, name: tick}]
        """.trimIndent())
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals(listOf("start"), f.marks)
        f.scheduler.advance(2); f.scheduler.advance(2); f.scheduler.advance(1)
        assertEquals(listOf("start", "tick", "tick"), f.marks); assertEquals(7.0, endPower); assertEquals(0, f.platform.managed.size)
    }
    @Test fun namedAuraTemplatesValidateBeforePublishingAndTagRemovalPreservesUnrelatedAuras() {
        val f = fixture()
        f.write(options = """
          templates:
            auras:
              pulse:
                type: apply_aura
                name: pulse
                tags: [magic]
                duration: 100t
        """.trimIndent(), effects = "  - {type: apply_aura, aura: pulse}\n  - {type: apply_aura, name: other, tags: [physical], duration: 100t}")
        f.write("remove", effects = "  - {type: remove_aura, tag: magic, caster: own}")
        f.load(); f.cast().join(); assertEquals(2, f.platform.auras.size)
        assertTrue(f.cast("remove").join().isSuccess)
        assertEquals(listOf("other"), f.platform.auras.instances(f.invocation().actor()).map { it.skill().value() })
        f.write("broken", effects = "  - {type: apply_aura, aura: absent}")
        val failed = f.engine.reload(); assertFalse(failed.success()); assertEquals(2, f.platform.definitions().size)
        assertTrue(f.platform.auras.instances(f.invocation().actor()).single().active())
    }
    @Test fun statsScaleWithStacksAndChargeExhaustionRemovesOnlyTheNamedAura() {
        val f = fixture()
        f.write(effects = """
          - type: apply_aura
            name: power
            tags: [buff]
            duration: 100t
            stack-limit: 2
            components: [{type: stat, stat: power, amount: 3, per-stack: true}]
        """.trimIndent())
        f.write("consume", effects = "  - {type: add_aura_charge, name: power, amount: -1, caster: own}")
        f.load(); f.cast().join(); val actor = f.invocation().actor()
        assertEquals(3.0, f.platform.contributions.snapshot(actor)[SkillKey("arcana", "power")])
        f.cast().join(); assertEquals(6.0, f.platform.contributions.snapshot(actor)[SkillKey("arcana", "power")])
        f.cast("consume").join(); assertTrue(f.platform.contributions.snapshot(actor).isEmpty()); assertEquals(0, f.platform.managed.size)
    }
    @Test fun eventSubscriptionsIgnoreGeneratedDamageAndAreRemovedAtOwnerShutdown() {
        val f = fixture()
        f.write(effects = """
          - type: apply_aura
            name: event_buff
            duration: 100t
            components:
              - {type: attack, effects: [{type: mark, name: attack}]}
              - {type: damaged, effects: [{type: mark, name: damaged}]}
        """.trimIndent())
        f.load(); f.cast().join(); val actor = f.invocation().actor()
        f.platform.auraEvents.emit(SkillKey("arcana", "attack"), actor, f.invocation())
        f.platform.auraEvents.emit(SkillKey("arcana", "damaged"), actor, f.invocation())
        assertTrue(f.marks.isEmpty()); f.scheduler.advance(1); assertEquals(listOf("attack", "damaged"), f.marks)
        SkillDamageScope.during { f.platform.auraEvents.emit(SkillKey("arcana", "attack"), actor, f.invocation()) }
        f.scheduler.advance(1); assertEquals(2, f.marks.size)
        assertEquals(2, f.platform.auraEvents.subscriptionCount)
        f.platform.auraEvents.emit(SkillKey("arcana", "attack"), actor, f.invocation()) // queued before unload
        f.engine.closeOwner(f.root.plugin); assertEquals(0, f.platform.auraEvents.subscriptionCount); assertEquals(0, f.platform.managed.size)
        f.platform.auraEvents.emit(SkillKey("arcana", "attack"), actor, f.invocation()); f.scheduler.advance(100); assertEquals(2, f.marks.size)
    }
    @Test fun shieldInterceptionCancelsSynchronouslyAndConsumesItsTwoChargesOncePerAcceptedEvent() {
        val f = fixture()
        f.write(effects = """
          - type: apply_aura
            name: shield
            duration: 100t
            charges: 2
            components:
              - type: damaged
                interceptors: [{type: cancel_event}]
                consume-charge: 1
        """.trimIndent())
        f.load(); f.cast().join(); val actor = f.invocation().actor()
        var cancelled = false
        val event = mockk<EntityDamageEvent>(); every { event.isCancelled } answers { cancelled }; every { event.isCancelled = any() } answers { cancelled = firstArg() }; every { event.damage } returns 4.0
        f.scheduler.on(f.root.caster) { f.platform.auraEvents.intercept(SkillKey("arcana", "damaged"), actor, f.invocation(), event) }.future.join()
        assertTrue(cancelled); assertEquals(1, f.platform.auras.instances(actor).single().charges)
        cancelled = false
        f.scheduler.on(f.root.caster) { f.platform.auraEvents.intercept(SkillKey("arcana", "damaged"), actor, f.invocation(), event) }.future.join()
        assertTrue(cancelled); assertTrue(f.platform.auras.instances(actor).isEmpty()); assertEquals(0, f.platform.auraEvents.subscriptionCount)
    }
    @Test fun typeAndCasterPoliciesKeepAnotherCastersAuraUntilExplicitAnyRemoval() {
        val f = fixture()
        f.write(effects = "  - {type: apply_aura, name: shared, aura-type: buff, tags: [magic], duration: 100t}")
        f.write("own", effects = "  - {type: remove_aura, aura-type: buff, caster: own}")
        f.write("any", effects = "  - {type: remove_aura, tag: magic, caster: any}")
        f.load(); f.cast().join()
        val player = SkillActor.of(f.root.caster)
        f.platform.cast(SkillKey("arcana", "one"), f.invocation(mob = true).derive().target(player).build()).toCompletableFuture().join()
        assertEquals(2, f.platform.auras.instances(player).size)
        f.cast("own").join(); assertEquals(SkillActor.of(f.root.victim), f.platform.auras.instances(player).single().caster)
        f.cast("any").join(); assertEquals(0, f.platform.managed.size)
    }
    @Test fun badHooksUnknownComponentsAndUnsafeLocalBindingsRejectWithoutChangingActiveDefinitions() {
        val f = fixture()
        f.write(effects = "  - {type: apply_aura, name: working, duration: 100t}"); f.load(); f.cast().join()
        val invalid = listOf(
            "  - {type: apply_aura, name: broken, duration: 100t, on-end: {skill: absent}}",
            "  - {type: apply_aura, name: broken, duration: 100t, components: [{type: missing}]}",
            "  - {type: apply_aura, name: broken, duration: 0t}",
            "  - {type: apply_aura, name: broken, duration: 100t, components: [{type: tick, interval: 0t, effects: [{type: mark}]}]}",
            "  - {type: apply_aura, name: broken, duration: 100t, components: [{type: damaged, interceptors: [{type: cancel_event, delay: 1t}]}]}"
        )
        for (effects in invalid) { f.write("broken", effects = effects); val report = f.engine.reload(); assertFalse(report.success(), effects); assertEquals(1, f.platform.definitions().size); assertEquals(1, f.platform.auras.size) }
        f.write("broken", options = "bindings:\n  var_value: {scope: actor, type: number, persistent: true}", effects = "  - {type: apply_aura, name: broken, duration: 100t, components: [{type: damaged, interceptors: [{type: cancel_event}]}]}")
        assertFalse(f.engine.reload().success()); assertEquals(1, f.platform.auras.size)
        f.engine.shutdown()
    }
    @Test fun aHookCanExhaustItsCurrentAuraWithoutCancellingAnAlreadyAdoptedChild() {
        val f = fixture(); lateinit var child: com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
        f.register("adopt_child") { _, _ -> CompiledSkillMechanic { execution ->
            child = com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState.adopt(execution as com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext, execution.invocation().actor()); f.success()
        } }
        f.write(effects = """
          - type: apply_aura
            name: parent
            duration: 100t
            components:
              - type: tick
                interval: 1t
                effects:
                  - {type: adopt_child}
                  - {type: add_aura_charge, current: true, amount: -1}
        """.trimIndent())
        f.load(); f.cast().join(); f.scheduler.advance(1)
        assertEquals(0, f.platform.auras.size); assertTrue(child.lease.active); assertEquals(1, f.platform.managed.size)
        child.close(); assertEquals(0, f.platform.managed.size)
    }
    @Test fun aPublicApiOnlyComponentOwnsItsTimerAndCleanupWithoutRuntimeServices() {
        val f = fixture(); var cleanups = 0
        f.platform.register(f.root.plugin, SkillComponentType.AURA_COMPONENT, SkillKey("arcana", "custom"), object : SkillAuraComponent(SkillDescriptor.of("custom", "API test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                val hooks = compilation.effects(arguments.list("effects"), "${arguments.path}.effects")
                return CompiledSkillMechanic { context ->
                    val scope = context.services().require(SkillAuraContext::class.java)
                    scope.own(AutoCloseable { cleanups++ }); scope.subscribeTicks(1, hooks); f.success()
                }
            }
        })
        f.write(effects = "  - {type: apply_aura, name: custom, duration: 2t, components: [{type: custom, effects: [{type: mark, name: api}]}]}")
        f.load(); f.cast().join(); f.scheduler.advance(1); assertEquals(listOf("api"), f.marks)
        f.scheduler.advance(1); assertEquals(1, cleanups); assertEquals(0, f.platform.managed.size)
    }

    @Test fun chargeDependentStatsReconcileAfterLocalInterceptionAndExplicitChargeChanges() {
        val f = fixture()
        f.write(effects = """
          - type: apply_aura
            name: shield
            duration: 100t
            charges: 3
            components:
              - {type: stat, stat: charge_power, amount: aura_charges, per-stack: false}
              - {type: damaged, interceptors: [{type: cancel_event}], consume-charge: 1}
        """.trimIndent())
        f.write("charge", effects = "  - {type: add_aura_charge, name: shield, amount: 1}")
        f.load(); f.cast().join(); val actor = f.invocation().actor(); val key = SkillKey("arcana", "charge_power")
        assertEquals(3.0, f.platform.contributions.snapshot(actor)[key])
        var cancelled = false
        val event = mockk<EntityDamageEvent>(); every { event.isCancelled } answers { cancelled }; every { event.isCancelled = any() } answers { cancelled = firstArg() }; every { event.damage } returns 4.0
        f.scheduler.on(f.root.caster) { f.platform.auraEvents.intercept(SkillKey("arcana", "damaged"), actor, f.invocation(), event) }.future.join()
        f.scheduler.advance(1); assertEquals(2.0, f.platform.contributions.snapshot(actor)[key])
        f.cast("charge").join(); assertEquals(3.0, f.platform.contributions.snapshot(actor)[key])
        f.engine.shutdown()
    }
    @Test fun publicCustomEventDispatchReachesAnApiOnlyAuraSubscription() {
        val f = fixture(); val kind = SkillKey("arcana", "custom_echo")
        f.register("selection_check") { _, _ -> CompiledSkillMechanic { context -> assertEquals(listOf(SkillActor.of(f.root.victim)), context.invocation().selection().entities()); f.success() } }
        f.platform.register(f.root.plugin, SkillComponentType.AURA_COMPONENT, SkillKey("arcana", "custom_event"), object : SkillAuraComponent(SkillDescriptor.of("custom_event", "API event test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                val hooks = compilation.effects(listOf(mapOf("type" to "selection_check"), mapOf("type" to "mark", "name" to "custom")), arguments.path)
                return CompiledSkillMechanic { context -> context.services().require(SkillAuraContext::class.java).subscribeEvent(kind, hooks, emptyList(), SkillPredicate { true }, 0); f.success() }
            }
        })
        f.write(effects = "  - {type: apply_aura, name: custom, duration: 100t, components: [{type: custom_event}]}")
        f.load(); f.cast().join(); f.platform.trigger(kind, f.invocation().derive().selection(SkillSelection(listOf(SkillActor.of(f.root.victim)), emptyList())).build()).toCompletableFuture().join()
        assertTrue(f.marks.isEmpty()); f.scheduler.advance(1); assertEquals(listOf("custom"), f.marks)
        f.engine.shutdown()
    }
    @Test fun aGiftedShieldUsesOnlyRecipientOwnedSnapshotsAndInvalidatesItsEventFacade() {
        val f = fixture(); var retained: SkillEventAccess? = null
        f.platform.register(f.root.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "capture"), object : SkillInterceptor(SkillDescriptor.of("capture", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillInterceptor { access, input, _ ->
                assertEquals(SkillActor.of(f.root.caster), input.actor()); assertTrue(input.target().isEmpty); assertTrue(input.trigger().isEmpty); assertTrue(input.selection().entities().isEmpty())
                retained = access; SkillResult.success()
            }
        })
        f.write(effects = "  - {type: apply_aura, name: shield, duration: 100t, charges: 2, components: [{type: damaged, interceptors: [{type: capture}, {type: cancel_event}], consume-charge: 1}]}")
        f.load(); val recipient = SkillActor.of(f.root.caster); val caster = SkillActor.of(f.root.victim)
        f.platform.cast(SkillKey("arcana", "one"), f.invocation(mob = true).derive().target(recipient).build()).toCompletableFuture().join()
        val foreign = mockk<EntityDamageEvent>()
        f.platform.auraEvents.intercept(SkillKey("arcana", "damaged"), recipient, f.invocation(), foreign); verify { foreign wasNot Called }
        var cancelled = false
        val event = mockk<EntityDamageEvent>(); every { event.isCancelled } answers { cancelled }; every { event.isCancelled = any() } answers { cancelled = firstArg() }; every { event.damage } returns 4.0
        val input = f.invocation().derive().target(caster).trigger(caster).selection(SkillSelection(listOf(caster), emptyList())).build()
        f.scheduler.on(f.root.caster) { f.platform.auraEvents.intercept(SkillKey("arcana", "damaged"), recipient, input, event) }.future.join()
        assertTrue(cancelled); assertThrows(IllegalStateException::class.java) { retained!!.cancelled() }
        val field = retained!!.javaClass.getDeclaredField("event").also { it.isAccessible = true }; assertNull(field.get(retained))
        f.engine.shutdown()
    }

    @Test fun outOfOrderStatSnapshotsCannotRestoreAnOlderStackContribution() {
        val f = fixture(); var hold = false; var casts = 0
        val pending = mutableListOf<java.util.concurrent.CompletableFuture<Map<SkillKey, Double>>>()
        val values = mapOf(SkillKey("arcana", "value") to 1.0)
        f.platform.register(f.root.plugin, SkillComponentType.STAT, SkillKey("arcana", "probe"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("probe", "Async snapshot barrier")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = if (hold) java.util.concurrent.CompletableFuture<Map<SkillKey, Double>>().also(pending::add) else java.util.concurrent.CompletableFuture.completedFuture(values)
        })
        f.register("before_apply") { _, _ -> CompiledSkillMechanic { casts++; if (casts > 1) hold = true; f.success() } }
        f.write(options = "bindings:\n  stat_probe: {provider: probe, key: value}", effects = "  - type: before_apply\n  - {type: apply_aura, name: power, duration: 100t, stack-limit: 3, components: [{type: stat, stat: power, amount: 3}]}")
        f.load(); f.cast().join(); val second = f.cast(); assertFalse(second.isDone)
        hold = false; val third = f.cast(); assertFalse(third.isDone); assertEquals(2, pending.size)
        pending[1].complete(values); assertTrue(third.join().isSuccess)
        pending[0].complete(values); assertTrue(second.join().isSuccess)
        assertEquals(9.0, f.platform.contributions.snapshot(f.invocation().actor())[SkillKey("arcana", "power")])
        f.engine.shutdown()
    }
    @Test fun disabledShippedShieldAndPeriodicBuffExamplesCompileAndWorkWhenExplicitlyEnabled() {
        val f = fixture()
        f.install(listOf(com.keopsfenks.arcana.skills.components.effects.variables.SetVariableMechanic(), com.keopsfenks.arcana.skills.components.effects.variables.AddVariableMechanic(), com.keopsfenks.arcana.skills.components.effects.variables.RemoveVariableMechanic()))
        for (name in listOf("aura-shield", "aura-periodic-buff")) {
            val text = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/v2/$name.yml")).bufferedReader().use { it.readText() }
            assertTrue(text.contains("enabled: false"))
            java.nio.file.Files.writeString(f.root.directory.resolve("$name.yml"), text.replace("enabled: false", "enabled: true"))
        }
        var pulses = 0.0
        f.register("read_pulses") { _, _ -> CompiledSkillMechanic { context -> pulses = context.services().require(SkillVariables::class.java).get(SkillVariableScope.ACTOR, SkillKey("arcana", "example_pulses")).orElseThrow().number(); f.success() } }
        f.write("read", effects = "  - type: read_pulses")
        f.load(); assertTrue(f.cast("example_aura_periodic_buff").join().isSuccess); assertTrue(f.cast("example_aura_shield").join().isSuccess)
        f.scheduler.advance(20); f.cast("read").join(); assertEquals(1.0, pulses)
        assertEquals(2.0, f.platform.contributions.snapshot(f.invocation().actor())[SkillKey("arcana", "example_power")])
        f.scheduler.advance(1000); assertEquals(0, f.platform.managed.size)
    }

}
