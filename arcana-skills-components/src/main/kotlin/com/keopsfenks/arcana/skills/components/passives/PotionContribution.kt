package com.keopsfenks.arcana.skills.components.passives
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.potion.PotionEffectType
import java.util.Locale

class PotionContribution(private val potions: PotionContributions, private val resolver: (String) -> PotionEffectType? = { Registry.EFFECT.get(requireNotNull(NamespacedKey.fromString(if (':' in it) it else "minecraft:$it"))) }) : SkillContribution(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("potion", "Owned refreshing potion contribution; preserves external effects", "potion", "amplifier", "ambient", "particles", "icon")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillContribution {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val type = resolver(arguments.string("potion").lowercase(Locale.ROOT)) ?: throw arguments.error("potion", "unknown potion type")
        val amplifier = arguments.number("amplifier", 0.0).also { require(it in 0.0..255.0 && kotlin.math.floor(it) == it) { "${arguments.path}.amplifier: expected integer 0..255" } }.toInt()
        val ambient = arguments.bool("ambient", false); val particles = arguments.bool("particles", true); val icon = arguments.bool("icon", true)
        return CompiledSkillContribution { context, identity ->
            val actor = context.invocation().actor(); var lease: SkillContributionLease? = null
            context.on(actor) { lease = potions.add(actor, identity, type, amplifier, ambient, particles, icon); SkillResult.success() }
                .thenApply { outcome -> if (!outcome.isSuccess) throw SkillProviderException(outcome); requireNotNull(lease) }.toCompletableFuture()
        }
    }
}
