package com.keopsfenks.arcana.skills.components.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.auras.AuraInstance
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedWorkRegistry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
class AuraStatComponent : SkillAuraComponent(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("stat", "Owned cached stat contribution reconciled to the current stack count", "stat", "amount", "per-stack")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val stat = SkillKey.parse(arguments.string("stat"), compilation.namespace())
        val amount = NodeOptions.number(arguments, compilation, "amount", 0, -1_000_000.0, 1_000_000.0)
        val scaled = arguments.bool("per-stack", true)
        return CompiledSkillMechanic { activation ->
            val aura = activation.services().require(AuraInstance::class.java)
            val current = AtomicReference<ManagedWorkRegistry.ManagedLease?>()
            aura.managed.own(AutoCloseable { current.getAndSet(null)?.close() })
            val update = CompiledSkillMechanic { execution ->
                val context = execution as? PlatformExecutionContext
                if (context == null) AuraSupport.unsupported() else {
                    val input = context.invocation()
                    val version = input.data()["aura_change_version"]?.number()?.toLong() ?: aura.changeVersion
                    val value = amount.evaluate(input) * if (scaled) input.data().getValue("aura_stacks").number() else 1.0
                    require(value.isFinite()) { "${arguments.path}.amount: nonfinite scaled stat" }
                    if (!aura.active() || version != aura.changeVersion) CompletableFuture.completedFuture(SkillResult.success())
                    else {
                        val known = current.get()
                        if (known != null) {
                            context.platform.contributions.update(known, mapOf(stat to value))
                            CompletableFuture.completedFuture(SkillResult.success())
                        } else context.derived(input.derive().actor(aura.actor()).target(null).build()).thenCompose { recipient ->
                            val contribution = CompiledSkillContribution { _, _ -> CompletableFuture.completedFuture(object : SkillContributionLease {
                                override fun stats() = mapOf(stat to value)
                                override fun close() {}
                            }) }
                            recipient.platform.contributions.apply(aura.definition.revision, recipient, contribution).thenApply { lease ->
                                if (!aura.active() || version != aura.changeVersion || !current.compareAndSet(null, lease)) lease.close()
                                else aura.managed.own(lease)
                                SkillResult.success()
                            }.whenComplete { _, _ -> recipient.state.release(recipient.frame) }
                        }
                    }
                }
            }
            aura.onChanged(update)
            update.execute(activation)
        }
    }
}
