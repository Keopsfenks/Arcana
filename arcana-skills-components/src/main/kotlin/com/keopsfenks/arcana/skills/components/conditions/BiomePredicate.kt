package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.NamespacedKey
import java.util.Locale
class BiomePredicate : SkillPredicateType(SkillDescriptor.of("biome", "Match namespaced biome data captured on the owning location")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "biomes")
        val biomes = ConditionSupport.strings(arguments, "biomes").map { val key = it.lowercase(Locale.ROOT); requireNotNull(NamespacedKey.fromString(if (':' in key) key else "minecraft:$key")) { "${arguments.path}.biomes: invalid biome resource key" }.toString() }.toSet()
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.BIOME)
        return SkillPredicate { it.data()[alias]?.takeIf { value -> value.kind() == SkillValue.Kind.TEXT }?.text() in biomes }
    }
}
