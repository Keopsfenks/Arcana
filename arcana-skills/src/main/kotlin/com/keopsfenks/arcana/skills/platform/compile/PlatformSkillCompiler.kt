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
import com.keopsfenks.arcana.skills.platform.admission.*
import com.keopsfenks.arcana.skills.platform.providers.CompiledStatBinding

/** Native definition/node compilation, independent of the installed component catalog. */
open class PlatformSkillCompiler(
    private val platform: DefaultSkillPlatform,
    private val environment: ExpressionEnvironment,
    private val namespace: String,
    private val environmentFactory: ((Array<String>) -> ExpressionEnvironment)? = null
) : SkillCompilation {
    private val dependencies = LinkedHashSet<SkillRegistration>()
    private val references = LinkedHashMap<SkillKey, String>()
    private val templates = LinkedHashMap<Pair<SkillKey, SkillKey>, SkillArguments>()
    private var localBindings: ((String) -> Unit)? = null
    private var localOnly = false
    private var nodes = 0
    private var depth = 0
    private var numbers = PlatformExpressionCompiler(environment, environmentFactory, baseBindings())
    override fun namespace() = namespace

    fun compile(definition: SkillDefinition): CompiledPlatformSkill {
        dependencies.clear(); references.clear(); templates.clear(); nodes = 0; depth = 0; localOnly = false
        val args = SkillArguments(definition.configuration(), definition.origin())
        val allowed = setOf("schema", "id", "enabled", "triggers", "permission", "chance", "cooldown", "cooldown-group", "conditions", "effects", "parameters", "bindings", "costs", "charges", "every", "interceptors", "passives", "activation", "templates")
        args.values.keys.firstOrNull { it !in allowed }?.let { throw args.error(it, "unsupported skill option") }
        val parameters = ParameterBindings.declarations(args, namespace)
        val variables = VariableBinding.declarations(args, namespace)
        val stats = statBindings(args)
        localBindings = { path ->
            require(stats.all { it.local }) { "$path: local interception requires local stat providers" }
            require(variables.none { it.persistent }) { "$path: local interception cannot prefetch persistent variables" }
        }
        val numeric = LinkedHashMap(baseBindings())
        parameters.filter { it.type() == SkillValue.Kind.NUMBER }.forEach { parameter ->
            numeric["param_${parameter.name()}"] = { invocation ->
                requireNotNull(invocation.parameters()[parameter.name()]) { "${args.path}.parameters.${parameter.name()}: value is unavailable" }.number()
            }
        }
        variables.filter { it.type == SkillValue.Kind.NUMBER }.forEach { binding ->
            numeric[binding.alias] = { invocation -> requireNotNull(invocation.data()[binding.alias]) { "${args.path}.bindings.${binding.alias}: value is unavailable" }.number() }
        }
        stats.forEach { binding -> numeric[binding.alias] = { invocation -> requireNotNull(invocation.data()[binding.alias]) { "${binding.path}: stat snapshot is unavailable" }.number() } }
        numbers = PlatformExpressionCompiler(environment, environmentFactory, numeric)
        if (args.has("templates")) {
            val groups = args.section("templates")
            groups.values.forEach { (groupName, _) ->
                val group = SkillKey.parse(groupName, namespace)
                require(group.namespace() == namespace) { "${groups.path}.$groupName: template group must belong to $namespace" }
                val values = groups.section(groupName)
                values.values.forEach { (name, _) ->
                    val key = SkillKey.parse(name, namespace)
                    require(key.namespace() == namespace) { "${values.path}.$name: template must belong to $namespace" }
                    require(templates.put(group to key, values.section(name)) == null) { "${values.path}.$name: duplicate template" }
                }
            }
            require(templates.size <= 256) { "${groups.path}: maximum 256 templates per definition" }
        }
        // Validate declared templates even when no effect currently references them.
        templates.values.forEach { template -> effects(listOf(template.values), template.path) }
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
        localOnly = args.list("interceptors", emptyList<Any>()).isNotEmpty()
        val conditions = predicates(args.list("conditions", emptyList<Any>()), "${args.path}.conditions")
        localOnly = false
        val effects = effects(args.list("effects", emptyList<Any>()), "${args.path}.effects")
        val passives = args.list("passives", emptyList<Any>()).mapIndexed { index, raw ->
            val node = args.child("passives[$index]", raw); count(node.path)
            val key = resolve(SkillComponentType.CONTRIBUTION, node.string("type"))
            val handle = platform.registry(SkillComponentType.CONTRIBUTION).get(key) ?: throw node.error("type", "unknown passive contribution $key")
            dependencies.add(handle)
            handle.component.compile(node, this)
        }
        require(effects.isNotEmpty() || passives.isNotEmpty()) { "${args.path}.effects: at least one effect or passive is required" }
        val activation = args.string("activation", "global")
        require(activation in setOf("global", "holder")) { "${args.path}.activation: expected global or holder" }
        val every = args.number("every", 1.0).also { require(it in 1.0..1_000_000.0 && kotlin.math.floor(it) == it) { "${args.path}.every: expected an integer between 1 and 1000000" } }.toInt()
        val costs = costs(args)
        val charges = if (!args.has("charges")) null else {
            val section = args.section("charges")
            section.values.keys.firstOrNull { it !in setOf("max", "refill", "group") }?.let { throw section.error(it, "unknown charge option") }
            val max = section.number("max", 1.0).also { require(it in 1.0..1_000_000.0 && kotlin.math.floor(it) == it) { "${section.path}.max: expected an integer between 1 and 1000000" } }.toInt()
            val refill = SkillArgumentsReader.durationNanos(section.raw("refill"), "${section.path}.refill")
            require(refill > 0) { "${section.path}.refill: must be positive" }
            CompiledCharges(SkillKey.parse(section.string("group", definition.key().value()), namespace), ChargeOptions(max, refill))
        }
        localOnly = true
        val interceptors = com.keopsfenks.arcana.skills.platform.events.InterceptorCompiler(this, platform, dependencies).compile(args)
        localOnly = false
        if (interceptors.isNotEmpty()) {
            require(costs.all { it.local }) { "${args.path}.costs: interceptors require local resource providers" }
            require(stats.all { it.local }) { "${args.path}.bindings: interceptors require local stat providers" }
            require(variables.none { it.persistent }) { "${args.path}.bindings: persistent prefetch is unavailable during interception" }
        }
        return CompiledPlatformSkill(definition.key(), args.bool("enabled", true), java.util.List.copyOf(triggers),
            if (args.has("permission")) args.string("permission").also { require(it.isNotBlank()) { "${args.path}.permission: must not be blank" } } else null,
            chance, SkillArgumentsReader.durationNanos(args.raw("cooldown", "0s"), "${args.path}.cooldown"),
            SkillKey.parse(args.string("cooldown-group", definition.key().value()), namespace), conditions, effects,
            Collections.unmodifiableSet(LinkedHashSet(dependencies)), Collections.unmodifiableMap(LinkedHashMap(references)), definition.origin(), parameters, variables, costs, charges, every, stats, interceptors, java.util.List.copyOf(passives), activation == "holder")
    }

    private fun costs(args: SkillArguments): List<CompiledCost> = java.util.List.copyOf(args.list("costs", emptyList<Any>()).mapIndexed { index, raw ->
        val cost = args.child("costs[$index]", raw)
        cost.values.keys.firstOrNull { it !in setOf("resource", "amount") }?.let { throw cost.error(it, "unknown cost option") }
        val handle = platform.providers.resource(cost.string("resource"), namespace, "${cost.path}.resource")
        dependencies.add(handle)
        val value = cost.raw("amount", 1)
        if (value is Number) try { handle.component.validateAmount(value.toDouble()) } catch (error: IllegalArgumentException) { throw cost.error("amount", error.message ?: "invalid amount") }
        val compiled = number(value, "${cost.path}.amount")
        CompiledCost(handle, SkillNumericExpression { invocation -> compiled.evaluate(invocation).also { handle.component.validateAmount(it) } }, handle.component.supportsLocal(), cost.path)
    })

    private fun statBindings(args: SkillArguments): List<CompiledStatBinding> {
        if (!args.has("bindings")) return emptyList()
        val section = args.section("bindings")
        return java.util.List.copyOf(section.values.filterKeys { it.startsWith("stat_") }.map { (alias, raw) ->
            val binding = section.child(alias, raw)
            require(alias.matches(Regex("stat_[a-z0-9_]{1,100}"))) { "${binding.path}: invalid stat alias" }
            binding.values.keys.firstOrNull { it !in setOf("provider", "key", "default") }?.let { throw binding.error(it, "unknown stat binding option") }
            val handle = platform.providers.stat(binding.string("provider"), namespace, "${binding.path}.provider")
            dependencies.add(handle)
            val fallback = if (!binding.has("default")) null else binding.number("default", 0.0)
            CompiledStatBinding(alias, handle, SkillKey.parse(binding.string("key", alias.removePrefix("stat_")), handle.key.namespace()), fallback, handle.component.supportsLocal(), binding.path)
        })
    }

    override fun auraComponents(definitions: List<*>, path: String): List<CompiledSkillMechanic> {
        require(definitions.size <= 64) { "$path: maximum 64 aura components" }
        return java.util.List.copyOf(definitions.mapIndexed { index, raw ->
            val args = SkillArguments(emptyMap<String, Any>(), path).child("[$index]", raw)
            count(args.path)
            nested(args.path) {
                val key = resolve(SkillComponentType.AURA_COMPONENT, args.string("type"))
                val handle = platform.registry(SkillComponentType.AURA_COMPONENT).get(key) ?: throw args.error("type", "unknown aura component $key")
                dependencies.add(handle)
                val compiled = handle.component.compile(args, this)
                CompiledSkillMechanic { context ->
                    if (!platform.componentActive(handle)) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Aura component $key retired"))
                    else compiled.execute(context)
                }
            }
        })
    }
    override fun template(group: SkillKey, name: SkillKey, path: String): SkillArguments = templates[group to name] ?: throw IllegalArgumentException("$path: unknown template $group/$name")
    override fun requireLocalBindings(path: String) { requireNotNull(localBindings) { "$path: binding declarations unavailable" }.invoke(path) }
    override fun localPredicates(definitions: List<*>, path: String): SkillPredicate {
        val previous = localOnly; localOnly = true
        return try { predicates(definitions, path) } finally { localOnly = previous }
    }
    override fun interceptors(definitions: List<*>, path: String): List<CompiledSkillInterceptor> {
        val previous = localOnly; localOnly = true
        return try { com.keopsfenks.arcana.skills.platform.events.InterceptorCompiler(this, platform, dependencies).compile(SkillArguments(mapOf("interceptors" to definitions), path)) }
        finally { localOnly = previous }
    }
    override fun number(value: Any?, path: String) = numbers.number(value, path)

    private fun baseBindings(): Map<String, (SkillInvocation) -> Double> = (SkillCompilerImpl.VARIABLES + arrayOf("aura_stacks", "aura_charges")).associateWith { name ->
        { invocation: SkillInvocation ->
            val value = invocation.data()[name]
            if (value == null && name == "damage") 0.0 else requireNotNull(value) { "Snapshot binding $name is unavailable" }.number()
        }
    }

    override fun effects(definitions: List<*>, path: String): List<CompiledSkillMechanic> {
        require(!localOnly) { "$path: normal effects are unavailable during interceptor compilation" }
        return java.util.List.copyOf(definitions.mapIndexed { index, raw ->
            val args = SkillArguments(emptyMap<String, Any>(), path).child("[$index]", raw)
            count(args.path)
            nested(args.path) {
                val key = resolve(SkillComponentType.MECHANIC, args.string("type"))
                val handle = platform.registry(SkillComponentType.MECHANIC).get(key) ?: throw args.error("type", "unknown mechanic $key")
                dependencies.add(handle)
                val options = NodeOptions.compile(args, this, ::targeter, ::mutator)
                val own = SkillArguments(args.values.filterKeys { it !in NodeOptions.OPTIONS }, args.path)
                val compiled = try { handle.component.compile(own, this) }
                catch (error: Exception) { throw args.error("", error.message ?: "invalid mechanic") }
                CompiledNode(CompiledSkillMechanic { execution ->
                    if (!handle.isActive) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Mechanic $key was removed"))
                    else compiled.execute(execution)
                }, options)
            }
        })
    }

    private fun targeter(args: SkillArguments): CompiledSkillTargeter = nested(args.path) {
        count(args.path)
        val key = resolve(SkillComponentType.TARGETER, args.string("type"))
        val handle = platform.registry(SkillComponentType.TARGETER).get(key) ?: throw args.error("type", "unknown targeter $key")
        dependencies.add(handle)
        val compiled = try { handle.component.compile(args, this) } catch (error: Exception) { throw args.error("", error.message ?: "invalid targeter") }
        CompiledSkillTargeter { context -> check(handle.isActive) { "Targeter $key was removed" }; compiled.select(context) }
    }
    private fun mutator(args: SkillArguments): CompiledSkillMutator = nested(args.path) {
        count(args.path)
        val key = resolve(SkillComponentType.MUTATOR, args.string("type"))
        val handle = platform.registry(SkillComponentType.MUTATOR).get(key) ?: throw args.error("type", "unknown mutator $key")
        dependencies.add(handle)
        val compiled = try { handle.component.compile(args, this) } catch (error: Exception) { throw args.error("", error.message ?: "invalid mutator") }
        CompiledSkillMutator { invocation -> check(handle.isActive) { "Mutator $key was removed" }; compiled.mutate(invocation) }
    }

    override fun predicate(definition: SkillArguments): SkillPredicate = nested(definition.path) {
        count(definition.path)
        val key = resolve(SkillComponentType.PREDICATE, definition.string("type"))
        val handle = platform.registry(SkillComponentType.PREDICATE).get(key) ?: throw definition.error("type", "unknown condition $key")
        require(!localOnly || handle.component.supportsLocal()) { "${definition.path}: condition $key does not support local interception" }
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
        if (localOnly) require(when (val component = handle.component) {
            is SkillPredicateType -> component.supportsLocal()
            is SkillResourceProvider -> component.supportsLocal()
            is SkillStatProvider -> component.supportsLocal()
            is SkillInterceptor -> true
            else -> false
        }) { "Component $key cannot be used during local interception" }
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
        require(++depth <= 64) { "$path: component nesting exceeds 64" }
        try { return action() } finally { depth-- }
    }
    private fun count(path: String) { require(++nodes <= 1024) { "$path: too many component nodes (maximum 1024)" } }
}
