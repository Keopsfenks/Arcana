package com.keopsfenks.arcana.skills.components.effects.items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.recipe.parts.EmptyTestableItem
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import org.bukkit.entity.Player

internal object ItemSupport {
    fun resolve(args: SkillArguments, lookup: (String) -> TestableItem?): TestableItem {
        val item = lookup(args.string("item")); require(item != null && item !is EmptyTestableItem) { "${args.path}.item: unknown item lookup" }; return item
    }
    fun player(context: SkillExecutionContext, recipient: (SkillInvocation) -> SkillActor?, action: (Player) -> SkillResult) = NativeActions.run(context, recipient) { entity ->
        if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Inventory operation requires a player") else action(entity)
    }
}
