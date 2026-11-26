package com.keopsfenks.arcana.skills.platform.holders
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.passives.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import io.mockk.*
import org.bukkit.NamespacedKey
import org.bukkit.attribute.*
import org.bukkit.potion.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Registry
import org.bukkit.Keyed
import java.lang.reflect.Proxy

class NativeContributionTests {
    private class TestBiome(private val identifier: NamespacedKey) : org.bukkit.block.Biome {
        override fun getKey() = identifier
        override fun name() = identifier.key.uppercase(java.util.Locale.ROOT)
        override fun ordinal() = 0
        override fun compareTo(other: org.bukkit.block.Biome) = identifier.toString().compareTo(other.key.toString())
    }
    private class TestPotionType(private val identifier: NamespacedKey) : PotionEffectType() {
        override fun getKey() = identifier
        override fun createEffect(duration: Int, amplifier: Int) = PotionEffect(this, duration, amplifier)
        override fun isInstant() = false
        override fun getCategory() = PotionEffectTypeCategory.BENEFICIAL
        override fun getColor() = org.bukkit.Color.WHITE
        override fun getDurationModifier() = 1.0
        override fun getId() = 0
        override fun getName() = identifier.key
        override fun getEffectAttributes(): Map<Attribute, AttributeModifier> = emptyMap()
        override fun getAttributeModifierAmount(attribute: Attribute, amplifier: Int) = 0.0
        override fun getEffectCategory() = Category.BENEFICIAL
        override fun getTranslationKey() = "effect.${identifier.namespace}.${identifier.key}"
        override fun translationKey() = translationKey
    }
    @TempDir lateinit var directory: Path
    private fun fixture() = PlatformFixture(directory).also { f -> f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } } }
    private fun holder(f: PlatformFixture, skills: List<String>) = f.platform.register(f.legacy.plugin, SkillComponentType.HOLDER, SkillKey("arcana", "slots"), object : SkillHolderProvider {
        override fun descriptor() = SkillDescriptor.of("slots", "test")
        override fun holders(actor: SkillActor) = CompletableFuture.completedFuture(listOf(SkillHolder(SkillKey("arcana", "ring"), "main", skills.map { SkillKey("arcana", it) }, emptyMap())))
    })
    @Test fun attributeApplicationCompensatesIfHolderRetiresInsideTheNativeWrite() {
        val f = fixture(); val attribute = mockk<Attribute>(); val instance = mockk<AttributeInstance>(); val modifiers = mutableListOf<AttributeModifier>()
        lateinit var provider: com.keopsfenks.arcana.core.skills.SkillRegistration
        every { attribute.key } returns NamespacedKey.minecraft("max_health")
        every { f.legacy.caster.getAttribute(attribute) } returns instance
        every { instance.modifiers } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.toList() }
        every { instance.addTransientModifier(any()) } answers { modifiers.add(firstArg()); provider.close(); Unit }
        every { instance.removeModifier(any<AttributeModifier>()) } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.remove(firstArg<AttributeModifier>()); Unit }
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "attribute"), AttributeContribution(f.scheduler) { attribute })
        f.write(options = "activation: holder\npassives: [{type: attribute, attribute: max_health, amount: 5}]"); f.load(); provider = holder(f, listOf("native"))
        f.platform.refreshHolders(f.invocation().actor()).handle { _, _ -> Unit }.toCompletableFuture().join()
        assertTrue(modifiers.isEmpty(), "Retired native modifier leaked"); assertEquals(0, f.platform.managed.size)
        verify(exactly = 1) { instance.removeModifier(any<AttributeModifier>()) }; f.engine.shutdown()
    }
    @Test fun timedStunApplicationCompensatesIfSourceRetiresInsideTheNativeWrite() {
        val f = fixture(); val attribute = mockk<Attribute>(); val instance = mockk<AttributeInstance>(); val modifiers = mutableListOf<AttributeModifier>()
        every { f.legacy.caster.getAttribute(attribute) } returns instance
        every { instance.modifiers } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.toList() }
        every { instance.addTransientModifier(any()) } answers { modifiers.add(firstArg()); f.engine.closeOwner(f.legacy.plugin); Unit }
        every { instance.removeModifier(any<AttributeModifier>()) } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.remove(firstArg<AttributeModifier>()); Unit }
        val type = com.keopsfenks.arcana.skills.components.effects.status.StunMechanic { if (it == "movement_speed") attribute else null }
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "stun"), type)
        f.write(effects = "  - type: stun"); f.load(); f.cast().handle { _, _ -> Unit }.join()
        assertTrue(modifiers.isEmpty(), "Retired timed stun leaked"); assertEquals(0, f.platform.managed.size); f.engine.shutdown()
    }
    @Test fun timedPotionApplicationCompensatesIfSourceRetiresInsideTheNativeWrite() {
        val f = fixture(); val type = potionType(); var current: PotionEffect? = null
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers { current = firstArg(); f.engine.closeOwner(f.legacy.plugin); true }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null }
        val potions = PotionContributions(f.scheduler)
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "potion"), com.keopsfenks.arcana.skills.components.effects.status.PotionMechanic(potions) { type })
        f.write(effects = "  - type: potion\n    potion: speed"); f.load(); f.cast().handle { _, _ -> Unit }.join()
        assertNull(current, "Retired timed potion leaked"); assertEquals(0, f.platform.managed.size); potions.close(); f.engine.shutdown()
    }
    @Test fun potionApplicationCompensatesIfHolderRetiresInsideTheNativeWrite() {
        val f = fixture(); val type = potionType(); var current: PotionEffect? = null; var writes = 0
        lateinit var provider: com.keopsfenks.arcana.core.skills.SkillRegistration
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers { current = firstArg(); writes++; provider.close(); true }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null }
        val potions = PotionContributions(f.scheduler)
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "potion"), PotionContribution(potions) { type })
        f.write(options = "activation: holder\npassives: [{type: potion, potion: speed}]"); f.load(); provider = holder(f, listOf("native"))
        f.platform.refreshHolders(f.invocation().actor()).handle { _, _ -> Unit }.toCompletableFuture().join()
        assertNull(current, "Retired native potion leaked"); assertEquals(0, f.platform.managed.size)
        val before = writes; f.scheduler.advance(40); assertEquals(before, writes); potions.close(); f.engine.shutdown()
    }
    @Test fun removingOneAttributeContributionRetainsEveryOtherModifier() {
        val f = fixture(); val attribute = mockk<Attribute>(); val instance = mockk<AttributeInstance>(); val modifiers = mutableListOf<AttributeModifier>()
        every { attribute.key } returns NamespacedKey.minecraft("max_health")
        every { f.legacy.caster.getAttribute(attribute) } answers { f.scheduler.assertOwner(f.legacy.caster); instance }
        every { instance.modifiers } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.toList() }
        every { instance.addTransientModifier(any()) } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.add(firstArg()); Unit }
        every { instance.removeModifier(any<AttributeModifier>()) } answers { f.scheduler.assertOwner(f.legacy.caster); modifiers.remove(firstArg<AttributeModifier>()); Unit }
        val external = AttributeModifier(NamespacedKey("other", "health"), 3.0, AttributeModifier.Operation.ADD_NUMBER); modifiers.add(external)
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "attribute"), AttributeContribution(f.scheduler) { attribute })
        f.write(options = "activation: holder\npassives: [{type: attribute, attribute: max_health, amount: 5}]"); f.load()
        val provider = holder(f, listOf("native")); f.platform.refreshHolders(f.invocation().actor()).join()
        assertEquals(2, modifiers.size); assertTrue(modifiers.any { it.key.namespace == "arcana" && it.amount == 5.0 })
        provider.close(); assertEquals(listOf(external), modifiers); provider.close(); assertEquals(listOf(external), modifiers)
    }
    private fun nativeTypes(): Pair<PotionEffectType, org.bukkit.block.Biome> {
        // Paper initializes potion constants through RegistryAccess before any Bukkit server exists.
        // Mock only that lookup during class initialization; no production fake registry is packaged.
        mockkStatic(RegistryAccess::class)
        mockkStatic(io.papermc.paper.InternalAPIBridge::class)
        val bridge = Proxy.newProxyInstance(io.papermc.paper.InternalAPIBridge::class.java.classLoader, arrayOf(io.papermc.paper.InternalAPIBridge::class.java)) { _, method, _ ->
            if (method.name == "constructLegacyCustomBiome") TestBiome(NamespacedKey.minecraft("custom")) else error("Unexpected internal fixture method ${method.name}")
        } as io.papermc.paper.InternalAPIBridge
        every { io.papermc.paper.InternalAPIBridge.get() } returns bridge
        val registries = mutableMapOf<Any, Registry<Keyed>>()
        fun registry(key: Any): Registry<Keyed> = registries.getOrPut(key) {
            @Suppress("UNCHECKED_CAST")
            Proxy.newProxyInstance(Registry::class.java.classLoader, arrayOf(Registry::class.java)) { _, method, args ->
                when (method.name) {
                    "get", "getOrThrow" -> if (key == RegistryKey.BIOME) TestBiome(args!![0] as NamespacedKey) else TestPotionType(args!![0] as NamespacedKey)
                    "toString" -> "test registry"
                    "hashCode" -> System.identityHashCode(key)
                    "equals" -> false
                    else -> error("Unexpected registry method ${method.name}")
                }
            } as Registry<Keyed>
        }
        val access = Proxy.newProxyInstance(RegistryAccess::class.java.classLoader, arrayOf(RegistryAccess::class.java)) { _, method, args ->
            if (method.name == "getRegistry") registry(args!![0]) else error("Unexpected RegistryAccess method ${method.name}")
        } as RegistryAccess
        every { RegistryAccess.registryAccess() } returns access
        try {
            val type = TestPotionType(NamespacedKey.minecraft("speed"))
            val biome = TestBiome(NamespacedKey.minecraft("plains"))
            return type to biome
        } finally { unmockkStatic(RegistryAccess::class); unmockkStatic(io.papermc.paper.InternalAPIBridge::class) }

    }
    private fun potionType() = nativeTypes().first
    @Test fun biomeMetadataIsCapturedOnLoadedLocationAndWorldClockOnGlobalContext() {
        val f = fixture(); val biome = nativeTypes().second
        mockkStatic(org.bukkit.Bukkit::class)
        try {
            every { org.bukkit.Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.legacy.world }
            every { f.legacy.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is org.bukkit.Location); true }
            every { f.legacy.world.getBiome(any<Int>(), any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is org.bukkit.Location); biome }
            every { f.legacy.world.time } answers { f.scheduler.assertOwner("global"); 6500L }
            every { f.legacy.world.isThundering } answers { f.scheduler.assertOwner("global"); true }
            for (type in listOf(com.keopsfenks.arcana.skills.components.conditions.BiomePredicate(), com.keopsfenks.arcana.skills.components.conditions.TimePredicate(), com.keopsfenks.arcana.skills.components.conditions.WeatherPredicate())) f.platform.register(f.legacy.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", type.descriptor().id()), type)
            f.write(options = "conditions: [{type: biome, biomes: [plains]}, {type: time, min: 6500, max: 6500}, {type: weather, states: [thunder]}]")
            f.load(); assertTrue(f.cast().join().isSuccess)
        } finally { f.engine.shutdown(); unmockkStatic(org.bukkit.Bukkit::class) }
    }
    @Test fun potionCleanupNeverRestoresAnExpiredSnapshotOrRemovesANewerExternalPotion() {
        val f = fixture(); val type = potionType()
        var current: PotionEffect? = null
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers { f.scheduler.assertOwner(f.legacy.caster); current = firstArg(); true }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null }
        val potions = PotionContributions(f.scheduler)
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "potion"), PotionContribution(potions) { type })
        f.write(options = "activation: holder\npassives: [{type: potion, potion: speed, amplifier: 1}]"); f.load()
        val provider = holder(f, listOf("native")); f.platform.refreshHolders(f.invocation().actor()).join(); assertEquals(1, current!!.amplifier)
        val external = PotionEffect(type, 200, 4)
        f.scheduler.on(f.legacy.caster) { current = external; potions.externalChange(f.invocation().actor(), type) }.future.join()
        provider.close(); assertSame(external, current)
        // A new holder never overwrites an existing external effect with a saved old snapshot.
        holder(f, listOf("native")); f.platform.refreshHolders(f.invocation().actor()).join(); assertSame(external, current)
        f.engine.cancel(f.legacy.playerId); assertSame(external, current); potions.close()
    }
    @Test fun closingTheStrongerOwnedPotionImmediatelyRevealsTheRemainingOwner() {
        val f = fixture(); val type = potionType(); var current: PotionEffect? = null
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers {
            f.scheduler.assertOwner(f.legacy.caster)
            val next = firstArg<PotionEffect>()
            if (current != null && current!!.amplifier > next.amplifier) false else { current = next; true }
        }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null }
        val potions = PotionContributions(f.scheduler); val actor = f.invocation().actor()
        val weak = f.scheduler.on(f.legacy.caster) { potions.add(actor, SkillKey("alpha", "weak"), type, 1, false, true, true) }.future.join()
        val strong = f.scheduler.on(f.legacy.caster) { potions.add(actor, SkillKey("beta", "strong"), type, 3, false, true, true) }.future.join()
        assertEquals(3, current!!.amplifier)
        strong.close(); assertEquals(1, current!!.amplifier)
        weak.close(); assertNull(current); potions.close()
    }
    @Test fun nativeTimedPotionMechanicsExpireIndependentlyAndRemovalKeepsExternalEffects() {
        val f = fixture(); val type = potionType(); var current: PotionEffect? = null
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers { f.scheduler.assertOwner(f.legacy.caster); current = firstArg(); true }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null; Unit }
        val potions = PotionContributions(f.scheduler)
        val apply = com.keopsfenks.arcana.skills.components.effects.status.PotionMechanic(potions) { type }
        val remove = com.keopsfenks.arcana.skills.components.effects.status.RemovePotionMechanic(potions) { type }
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "potion"), apply)
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "remove_potion"), remove)
        f.write("short", effects = "  - {type: potion, potion: speed, amplifier: 3, duration: 3t}")
        f.write("long", effects = "  - {type: potion, potion: speed, amplifier: 1, duration: 10t}")
        f.write("remove", effects = "  - {type: remove_potion, potion: speed}"); f.load()
        assertTrue(f.cast("short").join().isSuccess); assertTrue(f.cast("long").join().isSuccess); assertEquals(3, current!!.amplifier)
        f.scheduler.advance(3); assertEquals(1, current!!.amplifier)
        val external = PotionEffect(type, 200, 4)
        f.scheduler.on(f.legacy.caster) { current = external; potions.externalChange(f.invocation().actor(), type) }.future.join()
        assertTrue(f.cast("remove").join().isSuccess); assertSame(external, current)
        f.engine.closeOwner(f.legacy.plugin); assertSame(external, current); assertEquals(0, f.platform.managed.size); potions.close()
    }
    @Test fun removingTimedPotionsDoesNotDestroyAPersistentHolderContribution() {
        val f = fixture(); val type = potionType(); var current: PotionEffect? = null
        every { f.legacy.caster.getPotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current }
        every { f.legacy.caster.addPotionEffect(any()) } answers { f.scheduler.assertOwner(f.legacy.caster); current = firstArg(); true }
        every { f.legacy.caster.removePotionEffect(type) } answers { f.scheduler.assertOwner(f.legacy.caster); current = null; Unit }
        val potions = PotionContributions(f.scheduler); val actor = f.invocation().actor()
        val holder = f.scheduler.on(f.legacy.caster) { potions.add(actor, SkillKey("other", "holder"), type, 0, false, true, true) }.future.join()
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "potion"), com.keopsfenks.arcana.skills.components.effects.status.PotionMechanic(potions) { type })
        f.platform.register(f.legacy.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "remove_potion"), com.keopsfenks.arcana.skills.components.effects.status.RemovePotionMechanic(potions) { type })
        f.write(effects = "  - {type: potion, potion: speed, amplifier: 3, duration: 100t}"); f.write("remove", effects = "  - {type: remove_potion, potion: speed}"); f.load()
        f.cast().join(); assertEquals(3, current!!.amplifier); f.cast("remove").join()
        assertNotNull(current); assertEquals(0, current!!.amplifier); assertEquals(0, f.platform.managed.size)
        f.engine.shutdown(); assertEquals(0, current!!.amplifier); holder.close(); assertNull(current); potions.close()
    }

}
