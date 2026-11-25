package com.keopsfenks.arcana.skills.components.effects.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.auras.*
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext

class ApplyAuraMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("apply_aura", "Apply an owned finite aura with immutable hooks and components", "aura", "name", "aura-type", "tags", "duration", "period", "stack-limit", "share-caster", "refresh", "overwrite", "charges", "on-start", "on-tick", "on-end", "components")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        val args = AuraSupport.resolve(arguments, compilation)
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(args, descriptor())
        val name = SkillKey.parse(args.string("name"), compilation.namespace())
        val type = if (args.has("aura-type")) SkillKey.parse(args.string("aura-type"), compilation.namespace()) else name
        val tags = args.list("tags", emptyList<Any>()).map { require(it is String) { "${args.path}.tags: expected strings" }; SkillKey.parse(it, compilation.namespace()) }.toSet()
        require(tags.size <= 64) { "${args.path}.tags: maximum 64 tags" }
        val duration = AuraSupport.duration(args, compilation, "duration"); val period = AuraSupport.duration(args, compilation, "period", "20t")
        val stacks = args.number("stack-limit", 1.0).also { require(it in 1.0..64.0 && kotlin.math.floor(it) == it) { "${args.path}.stack-limit: expected integer 1..64" } }.toInt()
        val charges = NodeOptions.number(args, compilation, "charges", 1, 1.0, 1_000_000.0, integer = true)
        val share = args.bool("share-caster", false); val refresh = args.bool("refresh", true); val overwrite = args.bool("overwrite", false)
        val start = AuraSupport.hook(args, compilation, "on-start"); val tick = AuraSupport.hook(args, compilation, "on-tick"); val end = AuraSupport.hook(args, compilation, "on-end")
        val components = compilation.auraComponents(args.list("components", emptyList<Any>()), "${args.path}.components")
        return CompiledSkillMechanic { context ->
            if (context !is PlatformExecutionContext) AuraSupport.unsupported() else {
                val input = context.invocation(); val revision = requireNotNull(context.state.snapshot.entries[context.definition.key])
                val definition = AuraDefinition(name, revision, start, tick, end, tags, components = components, type = type)
                val options = AuraOptions(duration.evaluate(input).toLong(), period.evaluate(input).toLong(), stacks, share, refresh, overwrite, charges.evaluate(input).toInt())
                context.services().require(AuraManager::class.java).apply(definition, input, options).thenApply { SkillResult.success() }
            }
        }
    }
}
