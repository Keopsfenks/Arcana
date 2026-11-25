package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
class PermissionPredicate : SkillPredicateType(SkillDescriptor.of("permission", "Match only the exact declared player permission snapshot")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "permission"); val node = arguments.string("permission").also { require(it.isNotBlank() && it.length <= 256) { "${arguments.path}.permission: expected nonempty permission up to 256 characters" } }
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.PERMISSION, node)
        return SkillPredicate { it.data()[alias]?.takeIf { value -> value.kind() == SkillValue.Kind.BOOLEAN }?.bool() == true }
    }
}
