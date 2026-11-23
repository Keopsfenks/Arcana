package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.math.ExpressionCompilationException
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.values.ParameterBindings
import com.keopsfenks.arcana.skills.platform.values.VariableBinding
import java.util.Collections
import java.util.concurrent.CompletableFuture

/** Minimal factory compilation needed for source preflight, independent of node execution. */
class SourceSkillCompiler(
    private val platform: DefaultSkillPlatform,
    private val environment: ExpressionEnvironment,
    private val namespace: String,
    private val environmentFactory: ((Array<String>) -> ExpressionEnvironment)? = null
) : SkillCompilation {
    private val dependencies = LinkedHashSet<SkillRegistration>()
    private val references = LinkedHashMap<SkillKey, String>()
    private var nodes = 0
    private var depth = 0
    private var numbers = PlatformExpressionCompiler(environment, environmentFactory, baseBindings())
    override fun namespace() = namespace

    fun compile(definition: SkillDefinition): CompiledPlatformSkill {
        val args = SkillArguments(definition.configuration(), definition.origin())
        val allowed = setOf("schema", "id", "enabled", "triggers", "permission", "chance", "cooldown", "cooldown-group", "conditions", "effects", "parameters", "bindings")
        args.values.keys.firstOrNull { it !in allowed }?.let { throw args.error(it, "unsupported skill option") }
        val parameters = ParameterBindings.declarations(args, namespace)
        val variables = VariableBinding.declarations(args, namespace)
        val numeric = LinkedHashMap(baseBindings())
        parameters.filter { it.type() == SkillValue.Kind.NUMBER }.forEach { parameter ->
            numeric["param_${parameter.name()}"] = { invocation ->
                requireNotNull(invocation.parameters()[parameter.name()]) { "${args.path}.parameters.${parameter.name()}: value is unavailable" }.number()
            }
        }
        variables.filter { it.type == SkillValue.Kind.NUMBER }.forEach { binding ->
            numeric[binding.alias] = { invocation -> requireNotNull(invocation.data()[binding.alias]) { "${args.path}.bindings.${binding.alias}: value is unavailable" }.number() }
        }
        numbers = PlatformExpressionCompiler(environment, environmentFactory, numeric)
        val chance = args.number("chance", 100.0)
        require(chance in 0.0..100.0) { "${args.path}.chance: must be between 0 and 100" }
        val triggers = args.list("triggers", emptyList<Any>()).mapIndexed { i, value ->
            require(value is String) { "${args.path}.triggers[$i]: expected a string" }
            SkillKey.parse(value, "arcana")
        }
        // Trigger component resolution is added with the general-trigger contract; currently only
        // explicitly registered legacy trigger keys are accepted through the host's capability query.
        triggers.forEachIndexed { index, key ->
            val handle = platform.legacyTrigger(key) ?: throw args.error("triggers[$index]", "unknown trigger $key")
            dependencies.add(handle)
        }
        val conditions = predicates(args.list("conditions", emptyList<Any>()), "${args.path}.conditions")
        val effects = effects(args.list("effects"), "${args.path}.effects")
        require(effects.isNotEmpty()) { "${args.path}.effects: at least one effect is required" }
        return CompiledPlatformSkill(definition.key(), args.bool("enabled", true), java.util.List.copyOf(triggers),
            if (args.has("permission")) args.string("permission").also { require(it.isNotBlank()) { "${args.path}.permission: must not be blank" } } else null,
            chance, SkillArgumentsReader.durationNanos(args.raw("cooldown", "0s"), "${args.path}.cooldown"),
            SkillKey.parse(args.string("cooldown-group", definition.key().value()), namespace), conditions, effects,
            Collections.unmodifiableSet(LinkedHashSet(dependencies)), Collections.unmodifiableMap(LinkedHashMap(references)), definition.origin(), parameters, variables)
    }

    override fun number(value: Any?, path: String) = numbers.number(value, path)

    private fun baseBindings(): Map<String, (SkillInvocation) -> Double> = SkillCompilerImpl.VARIABLES.associateWith { name ->
        { invocation: SkillInvocation ->
            val value = invocation.data()[name]
            if (value == null && name == "damage") 0.0 else requireNotNull(value) { "Snapshot binding $name is unavailable" }.number()
        }
    }

    override fun effects(definitions: List<*>, path: String): List<CompiledSkillMechanic> = nested(path) {
        java.util.List.copyOf(definitions.mapIndexed { index, raw ->
            val args = SkillArguments(emptyMap<String, Any>(), path).child("[$index]", raw)
            val key = resolve(SkillComponentType.MECHANIC, args.string("type"))
            val handle = platform.registry(SkillComponentType.MECHANIC).get(key) ?: throw args.error("type", "unknown mechanic $key")
            dependencies.add(handle)
            val compiled = try { handle.component.compile(args, this) }
            catch (error: Exception) { throw args.error("", error.message ?: "invalid mechanic") }
            CompiledSkillMechanic { execution ->
                if (!handle.isActive) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Mechanic $key was removed"))
                else compiled.execute(execution)
            }
        })
    }

    override fun predicate(definition: SkillArguments): SkillPredicate = nested(definition.path) {
        val key = resolve(SkillComponentType.PREDICATE, definition.string("type"))
        val handle = platform.registry(SkillComponentType.PREDICATE).get(key) ?: throw definition.error("type", "unknown condition $key")
        dependencies.add(handle)
        val compiled = try { handle.component.compile(definition, this) }
        catch (error: Exception) { throw definition.error("", error.message ?: "invalid condition") }
        SkillPredicate { invocation -> check(handle.isActive) { "Condition $key was removed" }; compiled.test(invocation) }
    }
    override fun predicates(definitions: List<*>, path: String): SkillPredicate {
        val compiled = definitions.mapIndexed { index, raw -> predicate(SkillArguments(emptyMap<String, Any>(), path).child("[$index]", raw)) }
        return SkillPredicate { invocation -> compiled.all { it.test(invocation) } }
    }
    override fun <T : SkillPlatformComponent> component(type: SkillComponentType<T>, key: SkillKey): T {
        val handle = platform.registry(type).get(key) ?: throw IllegalArgumentException("Unknown ${type.id()} $key")
        dependencies.add(handle)
        return handle.component
    }
    override fun referenceSkill(key: SkillKey, path: String) { references[key] = path }
    private fun <T : SkillPlatformComponent> resolve(type: SkillComponentType<T>, name: String): SkillKey {
        val local = SkillKey.parse(name, namespace)
        if (':' in name || platform.registry(type).get(local) != null) return local
        return SkillKey.parse(name, "arcana")
    }
    private fun <T> nested(path: String, action: () -> T): T {
        require(++nodes <= 1024) { "$path: too many component nodes" }
        require(++depth <= 64) { "$path: component nesting exceeds 64" }
        try { return action() } finally { depth-- }
    }
}
