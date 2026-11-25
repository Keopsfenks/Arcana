package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.auras.AuraInstance
import java.util.concurrent.CompletableFuture
internal object AuraEventComponentSupport {
    fun compile(kind: SkillKey, arguments: SkillArguments, compilation: SkillCompilation, descriptor: SkillDescriptor): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, descriptor)
        val effects = AuraSupport.hook(arguments, compilation, "effects")
        val interceptors = compilation.interceptors(arguments.list("interceptors", emptyList<Any>()), "${arguments.path}.interceptors")
        require(effects.isNotEmpty() || interceptors.isNotEmpty()) { "${arguments.path}: effects or interceptors required" }
        val condition = if (interceptors.isEmpty()) compilation.predicates(arguments.list("conditions", emptyList<Any>()), "${arguments.path}.conditions") else {
            compilation.requireLocalBindings(arguments.path)
            compilation.localPredicates(arguments.list("conditions", emptyList<Any>()), "${arguments.path}.conditions")
        }
        val consume = arguments.number("consume-charge", 0.0).also { require(it in 0.0..1_000_000.0 && kotlin.math.floor(it) == it) { "${arguments.path}.consume-charge: expected integer 0..1000000" } }.toInt()
        require(consume == 0 || interceptors.isNotEmpty()) { "${arguments.path}.consume-charge: only local interception consumes directly; normal hooks can call add_aura_charge" }
        return CompiledSkillMechanic { context ->
            val aura = context.services().require(AuraInstance::class.java)
            aura.managed.platform.auraEvents.subscribe(kind, aura, effects, interceptors, condition, consume)
            CompletableFuture.completedFuture(SkillResult.success())
        }
    }
}
