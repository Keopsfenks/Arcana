package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.Material
import java.util.Locale
class EquipmentPredicate : SkillPredicateType(SkillDescriptor.of("equipment", "Match one requested player slot material and amount snapshot")) {
    override fun supportsLocal() = true
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): SkillPredicate {
        ComponentArguments.keys(arguments, "subject", "slot", "materials", "min-amount")
        val slot = arguments.string("slot", "main_hand").lowercase(Locale.ROOT); require(slot in setOf("main_hand", "off_hand", "helmet", "chestplate", "leggings", "boots")) { "${arguments.path}.slot: unknown slot" }
        val materials = ConditionSupport.strings(arguments, "materials").map { value -> try { Material.valueOf(value.removePrefix("minecraft:").uppercase(Locale.ROOT)).name } catch (_: IllegalArgumentException) { throw arguments.error("materials", "unknown material $value") } }.toSet()
        val amount = NativeActions.number(arguments, compilation, "min-amount", 1, 0.0, 4096.0, true)
        val alias = ConditionSupport.snapshot(arguments, compilation, SkillSnapshotRequest.Kind.EQUIPMENT, slot)
        return SkillPredicate { input -> val value = input.data()[alias]?.takeIf { it.kind() == SkillValue.Kind.MAP }?.map()
            value != null && value["material"]?.text() in materials && (ConditionSupport.number(value["amount"]) ?: -1.0) >= amount.evaluate(input)
        }
    }
}
