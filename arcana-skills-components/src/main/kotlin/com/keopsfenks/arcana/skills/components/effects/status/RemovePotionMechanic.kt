package com.keopsfenks.arcana.skills.components.effects.status
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.passives.PotionContributions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.potion.PotionEffectType

class RemovePotionMechanic(private val potions: PotionContributions, private val resolver: (String) -> PotionEffectType? = StatusSupport::potion) : SkillMechanic(SkillDescriptor.of("remove_potion", "Dispel timed Arcana potion claims; external removal requires an explicit flag")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "potion", "external"); val recipient = NativeActions.recipient(arguments)
        val type = StatusSupport.type(arguments, resolver); val external = arguments.bool("external", false)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity ->
            potions.removeOwned(SkillActor.of(entity), type); if (external) entity.removePotionEffect(type); SkillResult.success()
        } }
    }
}
