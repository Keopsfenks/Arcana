package com.keopsfenks.arcana.skills.components.effects.items
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.actions.NativeMaterials
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class DurabilityMechanic(private val maximum: (ItemStack) -> Int = { item -> val meta = item.itemMeta; if (meta is Damageable && meta.hasMaxDamage()) meta.maxDamage else item.type.maxDurability.toInt() }) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("durability", "Player-only bounded main-hand item damage adjustment", "target", "amount")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val amount = NativeActions.number(arguments, compilation, "amount", 1, -1_000_000.0, 1_000_000.0, true)
        return CompiledSkillMechanic { context -> ItemSupport.player(context, recipient) { player ->
            val copy = player.inventory.itemInMainHand.clone(); val meta = copy.itemMeta
            if (NativeMaterials.empty(copy.type) || meta !is Damageable) SkillResult.of(SkillResult.Status.NO_TARGET, "Held item is not damageable") else {
                val max = maximum(copy); require(max in 1..1_000_000) { "Native maximum durability is outside 1..1000000" }
                meta.damage = (meta.damage.toLong()+amount.evaluate(context.invocation()).toLong()).coerceIn(0, max.toLong()).toInt()
                copy.itemMeta = meta; player.inventory.setItemInMainHand(copy); SkillResult.success()
            }
        } }
    }
}
