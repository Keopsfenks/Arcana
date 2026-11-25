package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.entity.EntityType
import java.util.Locale
class EntityTypePredicate : SkillPredicateType(SkillDescriptor.of("entity_type", "Match compiled Bukkit entity enum names on one role")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "types"); val role = ConditionSupport.subject(arguments)
        val types = ConditionSupport.strings(arguments, "types").map { value -> try { EntityType.valueOf(value.removePrefix("minecraft:").uppercase(Locale.ROOT)).name } catch (_: IllegalArgumentException) { throw arguments.error("types", "unknown entity type $value") } }.toSet()
        return SkillPredicate { input -> ConditionSupport.data(input, role, "type")?.takeIf { it.kind() == SkillValue.Kind.TEXT }?.text() in types }
    }
}
