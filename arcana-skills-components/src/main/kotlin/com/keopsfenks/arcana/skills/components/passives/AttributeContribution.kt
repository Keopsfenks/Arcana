package com.keopsfenks.arcana.skills.components.passives
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.*
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CompletableFuture

class AttributeContribution(private val scheduler: SkillScheduler, private val resolver: (String) -> Attribute? = { Registry.ATTRIBUTE.get(requireNotNull(NamespacedKey.fromString(if (':' in it) it else "minecraft:$it"))) }) : SkillContribution(SkillDescriptor.of("attribute", "Owned attribute modifier; never resets the base value")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillContribution {
        arguments.values.keys.firstOrNull { it !in setOf("type", "attribute", "amount", "operation", "stat") }?.let { throw arguments.error(it, "unknown passive option") }
        val attribute = resolver(arguments.string("attribute").lowercase(Locale.ROOT)) ?: throw arguments.error("attribute", "unknown attribute")
        val amount = compilation.number(arguments.raw("amount", 0), "${arguments.path}.amount")
        val operation = try { AttributeModifier.Operation.valueOf(arguments.string("operation", "ADD_NUMBER").uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { throw arguments.error("operation", "unknown modifier operation") }
        val stat = if (arguments.has("stat")) SkillKey.parse(arguments.string("stat"), compilation.namespace()) else null
        return CompiledSkillContribution { context, identity ->
            val actor = context.invocation().actor(); var lease: SkillContributionLease? = null
            context.on(actor) { entity ->
                val value = amount.evaluate(context.invocation()); require(value.isFinite() && kotlin.math.abs(value) <= 1_000_000) { "${arguments.path}.amount: expected finite value within +/-1000000" }
                val instance = entity.getAttribute(attribute) ?: throw SkillProviderException(SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Actor does not have this attribute"))
                val modifier = AttributeModifier(NamespacedKey(identity.namespace(), identity.value()), value, operation)
                instance.addTransientModifier(modifier)
                val closed = AtomicBoolean()
                lease = object : SkillContributionLease {
                    override fun stats() = if (stat == null) emptyMap() else mapOf(stat to value)
                    override fun close() {
                        if (closed.compareAndSet(false, true)) scheduler.on(actor.entity()) {
                            // Remove the exact modifier we added, never another owner's value.
                            if (instance.modifiers.any { it.key == modifier.key && it == modifier }) instance.removeModifier(modifier)
                        }
                    }
                }
                SkillResult.success()
            }.thenApply { outcome -> if (!outcome.isSuccess) throw SkillProviderException(outcome); requireNotNull(lease) }.toCompletableFuture()
        }
    }
}
