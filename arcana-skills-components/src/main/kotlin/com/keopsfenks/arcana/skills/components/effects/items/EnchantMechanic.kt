package com.keopsfenks.arcana.skills.components.effects.items
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.actions.NativeMaterials
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.Locale

/** Resolved native operations allow bounds tests without bootstrapping Paper's static registries. */
internal class EnchantBinding(val maximum: Int, val canApply: (ItemStack) -> Boolean, val apply: (ItemMeta, Int, Boolean) -> Boolean, val remove: (ItemMeta) -> Boolean)
class EnchantMechanic internal constructor(private val resolve: (String) -> EnchantBinding?) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("enchant", "Player-only main-hand enchantment bounded by native compatibility and level", "target", "enchantment", "level", "unsafe")) {
    constructor() : this({ key ->
        val type = Registry.ENCHANTMENT.get(requireNotNull(NamespacedKey.fromString(if (':' in key) key else "minecraft:$key")))
        type?.let { EnchantBinding(it.maxLevel, it::canEnchantItem, { meta, level, unsafe -> meta.addEnchant(it, level, unsafe) }, { meta -> meta.removeEnchant(it) }) }
    })
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val level = NativeActions.number(arguments, compilation, "level", 1, 0.0, 255.0, true)
        val binding = resolve(arguments.string("enchantment").lowercase(Locale.ROOT)) ?: throw arguments.error("enchantment", "unknown enchantment")
        val unsafe = arguments.bool("unsafe", false)
        return CompiledSkillMechanic { context -> ItemSupport.player(context, recipient) { player ->
            val copy = player.inventory.itemInMainHand.clone(); val meta = copy.itemMeta; val value = level.evaluate(context.invocation()).toInt()
            if (NativeMaterials.empty(copy.type) || meta == null) SkillResult.of(SkillResult.Status.NO_TARGET, "Held item has no enchantable metadata") else {
                require(unsafe || value <= binding.maximum) { "${arguments.path}.level: exceeds native enchantment maximum" }
                require(value == 0 || unsafe || binding.canApply(copy)) { "${arguments.path}.enchantment: incompatible held item" }
                if (value == 0) binding.remove(meta) else binding.apply(meta, value, unsafe)
                copy.itemMeta = meta; player.inventory.setItemInMainHand(copy); SkillResult.success()
            }
        } }
    }
}
