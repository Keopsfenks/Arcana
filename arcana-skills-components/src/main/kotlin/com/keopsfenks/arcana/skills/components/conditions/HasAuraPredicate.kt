package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
class HasAuraPredicate : SkillPredicateType(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("has_aura", "Match filtered cached aura metadata with stacks and charges on the same instance", "subject", "name", "aura-type", "tag", "caster", "min-stacks", "min-charges")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val filters = linkedMapOf<String, Any>(); for (key in listOf("name", "aura-type", "tag")) if (arguments.has(key)) filters[key] = SkillKey.parse(arguments.string(key), compilation.namespace())
        val caster = arguments.string("caster", "own"); require(caster in setOf("own", "any")) { "${arguments.path}.caster: expected own or any" }; filters["caster"] = caster
        val stacks = NativeActions.number(arguments, compilation, "min-stacks", 1, 1.0, 64.0, true); val charges = NativeActions.number(arguments, compilation, "min-charges", 1, 0.0, 1_000_000.0, true)
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.AURAS, options = filters)
        return SkillPredicate { input -> input.data()[alias]?.takeIf { it.kind() == SkillValue.Kind.LIST }?.list()?.any { value ->
            val aura = value.map(); (ConditionSupport.number(aura["stacks"]) ?: -1.0) >= stacks.evaluate(input) && (ConditionSupport.number(aura["charges"]) ?: -1.0) >= charges.evaluate(input)
        } == true }
    }
}
