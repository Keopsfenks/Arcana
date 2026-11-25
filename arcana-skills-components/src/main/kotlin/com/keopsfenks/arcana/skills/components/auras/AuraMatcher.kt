package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.auras.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
internal class AuraMatcher(args: SkillArguments, compiler: SkillCompilation) {
    private val current = args.bool("current", false)
    private val name = if (args.has("name")) SkillKey.parse(args.string("name"), compiler.namespace()) else null
    private val type = if (args.has("aura-type")) SkillKey.parse(args.string("aura-type"), compiler.namespace()) else null
    private val tag = if (args.has("tag")) SkillKey.parse(args.string("tag"), compiler.namespace()) else null
    private val own = args.string("caster", "own").also { require(it in setOf("own", "any")) { "${args.path}.caster: expected own or any" } } == "own"
    fun select(context: PlatformExecutionContext): List<AuraInstance> {
        val input = context.invocation(); val recipient = input.target().orElse(input.actor())
        val candidates = if (current) listOf(context.state.managedState?.aura ?: throw SkillProviderException(com.keopsfenks.arcana.core.skills.SkillResult.of(com.keopsfenks.arcana.core.skills.SkillResult.Status.UNSUPPORTED_CONTEXT, "current aura requires an aura hook")))
            else context.services().require(AuraManager::class.java).instances(recipient)
        return candidates.filter { aura -> aura.active() &&
            (name == null || aura.skill() == name) && (type == null || aura.definition.type == type) && (tag == null || tag in aura.definition.tags) && (current || !own || aura.caster == input.actor())
        }
    }
}
