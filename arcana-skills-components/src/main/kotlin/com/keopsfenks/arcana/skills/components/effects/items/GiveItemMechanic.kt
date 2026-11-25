package com.keopsfenks.arcana.skills.components.effects.items
import com.keopsfenks.arcana.core.items.*
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.actions.NativeMaterials
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext

class GiveItemMechanic(private val resolve: (String) -> TestableItem? = Items::lookup) : SkillMechanic(SkillDescriptor.of("give_item", "Player-only cloned Arcana item lookup, at most 64 native stacks")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "item", "amount"); val recipient = NativeActions.recipient(arguments)
        val amount = NativeActions.number(arguments, compilation, "amount", 1, 1.0, 4096.0, true)
        val item = ItemSupport.resolve(arguments, resolve).item; require(!NativeMaterials.empty(item.type)) { "${arguments.path}.item: expected a nonempty item" }
        return CompiledSkillMechanic { context -> ItemSupport.player(context, recipient) { player ->
            var remaining = amount.evaluate(context.invocation()).toInt(); val maximum = item.maxStackSize.coerceIn(1, 99)
            require((remaining+maximum-1)/maximum <= 64) { "${arguments.path}.amount: maximum 64 stacks per operation" }
            var outcome = SkillResult.success()
            while (remaining > 0 && outcome.isSuccess) {
                (context as? PlatformExecutionContext)?.state?.step()
                val count = minOf(remaining, maximum); val copy = item.clone(); copy.amount = count
                val leftovers = player.inventory.addItem(copy); remaining -= count
                if (leftovers.isNotEmpty()) outcome = SkillResult.of(SkillResult.Status.NO_TARGET, "Inventory full; previously inserted items remain")
            }
            outcome
        } }
    }
}
