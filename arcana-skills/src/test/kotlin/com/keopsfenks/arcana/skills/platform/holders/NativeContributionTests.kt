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
    private fun potionType(): PotionEffectType {
        // Paper initializes potion constants through RegistryAccess before any Bukkit server exists.
        // Mock only that lookup during class initialization; no production fake registry is packaged.
        mockkStatic(RegistryAccess::class)
        val registries = mutableMapOf<Any, Registry<Keyed>>()
        fun registry(key: Any): Registry<Keyed> = registries.getOrPut(key) {
            @Suppress("UNCHECKED_CAST")
            Proxy.newProxyInstance(Registry::class.java.classLoader, arrayOf(Registry::class.java)) { _, method, args ->
                when (method.name) {
                    "get", "getOrThrow" -> TestPotionType(args!![0] as NamespacedKey)
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
        val type = TestPotionType(NamespacedKey.minecraft("speed"))
        unmockkStatic(RegistryAccess::class)
        return type

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

}
