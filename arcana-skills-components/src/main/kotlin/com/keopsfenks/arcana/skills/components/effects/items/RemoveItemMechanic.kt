package com.keopsfenks.arcana.skills.components.effects.items
import com.keopsfenks.arcana.core.items.*
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.actions.NativeMaterials
import com.keopsfenks.arcana.skills.config.ComponentArguments

class RemoveItemMechanic(private val resolve: (String) -> TestableItem? = Items::lookup) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("remove_item", "Player-only exact-count removal after matching-slot preflight", "target", "item", "amount")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val amount = NativeActions.number(arguments, compilation, "amount", 1, 1.0, 4096.0, true); val item = ItemSupport.resolve(arguments, resolve)
        return CompiledSkillMechanic { context -> ItemSupport.player(context, recipient) { player ->
            val requested = amount.evaluate(context.invocation()).toInt(); val storage = player.inventory.storageContents
            require(storage.size <= 54) { "Storage inventory exceeds 54 slots" }
            val matches = storage.indices.filter { storage[it]?.let { stack -> !NativeMaterials.empty(stack.type) && item.matches(stack) } == true }
            if (matches.sumOf { storage[it]!!.amount.toLong() } < requested) SkillResult.of(SkillResult.Status.NO_TARGET, "Not enough matching items") else {
                var remaining = requested
                for (index in matches) if (remaining > 0) {
                    val copy = storage[index]!!.clone(); val count = minOf(remaining, copy.amount)
                    copy.amount -= count; remaining -= count; player.inventory.setItem(index, if (copy.amount == 0) null else copy)
                }
                SkillResult.success()
            }
        } }
    }
}
