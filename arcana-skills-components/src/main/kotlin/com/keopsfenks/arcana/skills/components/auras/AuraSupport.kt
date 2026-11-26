package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.flow.FlowSupport
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import java.util.concurrent.CompletableFuture

internal object AuraSupport {
    fun duration(args: SkillArguments, compiler: SkillCompilation, key: String, fallback: String? = null): SkillNumericExpression {
        require(args.has(key) || fallback != null) { "${args.path}.$key: required" }
        val actual = if (args.has(key)) args else SkillArguments(args.values + (key to fallback), args.path)
        val raw = actual.raw(key)
        if (raw is Number || raw is String && raw.matches(Regex("[0-9]+(?:\\.[0-9]+)?(?:ms|s|m|t)"))) require(SkillArgumentsReader.ticks(raw, "${args.path}.$key") >= 1) { "${args.path}.$key: must be at least one tick" }
        val duration = NodeOptions.duration(actual, compiler, key)
        return SkillNumericExpression { input -> duration.evaluate(input).also { require(it in 1.0..1_728_000.0) { "${args.path}.$key: expected 1..1728000 ticks" } } }
    }
    fun hook(args: SkillArguments, compiler: SkillCompilation, key: String): List<CompiledSkillMechanic> {
        if (!args.has(key)) return emptyList()
        val raw = args.raw(key)
        if (raw is List<*>) return compiler.effects(raw, "${args.path}.$key")
        val named = if (raw is String) SkillArguments(mapOf("skill" to raw), "${args.path}.$key") else args.section(key)
        named.values.keys.firstOrNull { it !in setOf("skill", "parameters") }?.let { throw named.error(it, "unknown named hook option") }
        val skill = SkillKey.parse(named.string("skill"), compiler.namespace()); compiler.referenceSkill(skill, "${named.path}.skill")
        val parameters = if (!named.has("parameters")) null else named.section("parameters").values.mapValues { (name, value) ->
            SkillKey("arcana", name); FlowSupport.parameter(value, compiler, "${named.path}.parameters.$name")
        }
        return listOf(CompiledSkillMechanic { context ->
            val input = context.invocation()
            val supplied = parameters?.mapValues { it.value(input) } ?: if (context is PlatformExecutionContext) {
                val declared = context.state.snapshot.entries[skill]?.native?.parameters?.map { it.name() }?.toSet() ?: emptySet()
                input.parameters().filterKeys { it in declared }
            } else emptyMap()
            context.cast(skill, supplied)
        })
    }
    fun resolve(args: SkillArguments, compiler: SkillCompilation, seen: Set<SkillKey> = emptySet()): SkillArguments {
        if (!args.has("aura")) return args
        val key = SkillKey.parse(args.string("aura"), compiler.namespace())
        require(key !in seen && seen.size < 64) { "${args.path}.aura: cyclic or excessive named aura references" }
        val template = compiler.template(SkillKey(compiler.namespace(), "auras"), key, "${args.path}.aura")
        require(SkillKey.parse(template.string("type"), "arcana") == SkillKey("arcana", "apply_aura")) { "${template.path}.type: named aura must use apply_aura" }
        val inherited = resolve(template, compiler, seen + key)
        return SkillArguments(inherited.values + args.values.filterKeys { it !in setOf("aura", "type") }, "${args.path} (template ${template.path})")
    }
    fun unsupported() = CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Aura requires an Arcana execution context"))
}
