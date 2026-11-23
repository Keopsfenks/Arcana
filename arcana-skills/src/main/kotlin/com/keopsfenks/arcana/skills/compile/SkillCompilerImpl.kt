package com.keopsfenks.arcana.skills.compile

import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import com.keopsfenks.arcana.skills.registry.ComponentRegistry
import java.util.Collections
import java.util.concurrent.CompletableFuture

class SkillCompilerImpl(
    private val environment: ExpressionEnvironment,
    private val effectTypes: ComponentRegistry<SkillEffect>,
    private val conditionTypes: ComponentRegistry<SkillCondition>,
    private val triggerTypes: ComponentRegistry<SkillTrigger>,
    private val filterCompiler: (SkillArguments) -> CompiledSkillCondition = {
        require(it.values.isEmpty()) { "${it.path}: no filter compiler registered" }
        CompiledSkillCondition { true }
    }
) : SkillCompiler {
    private val dependencies = LinkedHashSet<SkillRegistration>()
    private var depth = 0
    private var nodes = 0

    fun compile(definition: SkillArguments): CompiledSkill {
        dependencies.clear()
        depth = 0
        nodes = 0
        val allowed = setOf("id", "enabled", "triggers", "permission", "chance", "cooldown", "cooldown-group", "filters", "conditions", "effects")
        definition.values.keys.firstOrNull { it !in allowed }?.let { throw definition.error(it, "unknown skill option") }
        val id = definition.string("id")
        require(id.matches(Regex("[a-z0-9_]{1,100}"))) { "${definition.path}.id: invalid skill ID" }
        val triggerIds = definition.list("triggers", emptyList<Any>()).mapIndexed { index, value ->
            require(value is String) { "${definition.path}.triggers[$index]: expected a string" }
            val handle = triggerTypes.get(value) ?: throw definition.error("triggers[$index]", "unknown trigger $value")
            dependencies.add(handle)
            value
        }.distinct()
        val chance = definition.number("chance", 100.0)
        require(chance in 0.0..100.0) { "${definition.path}.chance: must be between 0 and 100" }
        val conditions = conditions(definition.list("conditions", emptyList<Any>()), "${definition.path}.conditions")
        val filters = filterCompiler(if (definition.has("filters")) definition.section("filters") else SkillArguments(emptyMap<String, Any>(), "${definition.path}.filters"))
        val effects = effects(definition.list("effects"), "${definition.path}.effects")
        require(effects.isNotEmpty()) { "${definition.path}.effects: at least one effect is required" }
        val group = definition.string("cooldown-group", id)
        require(group.matches(Regex("[a-z0-9_]{1,100}"))) { "${definition.path}.cooldown-group: invalid group" }
        return CompiledSkill(
            id, definition.bool("enabled", true), java.util.List.copyOf(triggerIds),
            if (definition.has("permission")) definition.string("permission").also {
                require(it.isNotBlank()) { "${definition.path}.permission: must not be blank" }
            } else null,
            chance, SkillArgumentsReader.durationNanos(definition.raw("cooldown", "0s"), "${definition.path}.cooldown"),
            group, conditions, filters, effects, Collections.unmodifiableSet(LinkedHashSet(dependencies)), definition.path
        )
    }

    override fun number(value: Any?, path: String): SkillNumber {
        if (value is Number) {
            val number = value.toDouble()
            require(number.isFinite()) { "$path: number must be finite" }
            return SkillNumber { number }
        }
        require(value is String && value.isNotBlank() && value.length <= 4096) { "$path: expected a number or an expression" }
        val compiled = try { environment.compileOrThrow(value) }
        catch (error: IllegalArgumentException) { throw IllegalArgumentException("$path: invalid expression: ${error.message}", error) }
        return SkillNumber { context ->
            val result = compiled.evaluate(*VARIABLES.map { context.getNumber(it) }.toDoubleArray())
            require(result.isFinite()) { "$path: expression produced a non-finite result" }
            result
        }
    }

    override fun effects(definitions: List<*>, path: String): List<CompiledSkillEffect> = nested(path) {
        java.util.List.copyOf(definitions.mapIndexed { index, value ->
            val args = SkillArguments(emptyMap<String, Any>(), path).child("[$index]", value)
            val type = args.string("type")
            val handle = effectTypes.get(type) ?: throw args.error("type", "unknown effect $type")
            dependencies.add(handle)
            val compiled = try { handle.component.compile(args, this) }
            catch (error: IllegalArgumentException) { throw args.error("", error.message ?: "invalid effect") }
            CompiledSkillEffect { execution ->
                if (!handle.isActive) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Effect $type was removed"))
                else compiled.execute(execution)
            }
        })
    }

    override fun condition(definition: SkillArguments): CompiledSkillCondition = nested(definition.path) {
        val type = definition.string("type")
        val handle = conditionTypes.get(type) ?: throw definition.error("type", "unknown condition $type")
        dependencies.add(handle)
        val compiled = try { handle.component.compile(definition, this) }
        catch (error: IllegalArgumentException) { throw definition.error("", error.message ?: "invalid condition") }
        CompiledSkillCondition { context ->
            check(handle.isActive) { "Condition $type was removed" }
            compiled.test(context)
        }
    }

    override fun conditions(definitions: List<*>, path: String): CompiledSkillCondition {
        val children = definitions.mapIndexed { index, value -> condition(SkillArguments(emptyMap<String, Any>(), path).child("[$index]", value)) }
        return CompiledSkillCondition { context -> children.all { it.test(context) } }
    }

    private fun <T> nested(path: String, action: () -> T): T {
        require(++nodes <= 1024) { "$path: too many component nodes (maximum 1024)" }
        require(++depth <= 64) { "$path: component nesting exceeds 64 levels" }
        try { return action() } finally { depth-- }
    }

    companion object {
        val VARIABLES = arrayOf("damage", "caster_health", "caster_max_health", "caster_food", "caster_level")
    }
}
