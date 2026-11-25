package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.Locale
class WeatherPredicate : SkillPredicateType(SkillDescriptor.of("weather", "Match global world clear, rain or thunder snapshots")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "states"); val states = ConditionSupport.strings(arguments, "states", 3).map { it.lowercase(Locale.ROOT) }.toSet()
        require(states.all { it in setOf("clear", "rain", "thunder") }) { "${arguments.path}.states: unknown weather state" }
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.WEATHER)
        return SkillPredicate { it.data()[alias]?.takeIf { value -> value.kind() == SkillValue.Kind.TEXT }?.text() in states }
    }
}
