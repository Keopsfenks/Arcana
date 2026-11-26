package com.keopsfenks.arcana.skills.platform.auras

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import java.util.concurrent.*

class AuraManager private constructor(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform, private val lifecycle: SkillLifecycleGate, private val clock: () -> Long, private val store: Store, private val bound: PlatformExecutionContext?) : AutoCloseable {
    private class Store { val entries = LinkedHashMap<AuraStackKey, AuraInstance>(); var stopped = false }
    constructor(engine: DefaultSkillEngine, platform: DefaultSkillPlatform, lifecycle: SkillLifecycleGate, clock: () -> Long) : this(engine, platform, lifecycle, clock, Store(), null)
    private val entries get() = store.entries
    fun forContext(context: PlatformExecutionContext) = AuraManager(engine, platform, lifecycle, clock, store, context)
    fun apply(definition: AuraDefinition, invocation: SkillInvocation, options: AuraOptions): CompletableFuture<SkillManagedHandle> {
        val parent = bound ?: return CompletableFuture.failedFuture(IllegalStateException("Use the invocation-bound AuraManager service or supply an execution context"))
        return apply(definition, invocation, options, parent)
    }
    val size get() = lifecycle.locked { entries.size }
    fun instances(target: SkillActor): List<AuraInstance> = lifecycle.locked { entries.values.filter { it.actor() == target && it.active() } }
    fun apply(definition: AuraDefinition, invocation: SkillInvocation, options: AuraOptions, context: PlatformExecutionContext): CompletableFuture<SkillManagedHandle> {
        return try {
            context.state.checkActive(); context.frame.checkActive()
            require(context.definition.key == definition.revision.key && context.state.snapshot.entries[context.definition.key] === definition.revision) { "Aura source revision does not match its execution" }
            val target = invocation.target().orElse(invocation.actor())
            val key = AuraStackKey(definition.key, definition.revision.revisionId, target.id(), if (options.shareCaster) null else invocation.actor().id())
            var created = false
            val instance = lifecycle.locked {
                check(!store.stopped && !engine.stopped && definition.revision.available()) { "Aura owner unavailable" }
                entries[key]?.takeIf { it.active() } ?: run {
                    val managed = ManagedExecutionState.adopt(context, target, definition.dependencies, invocation)
                    AuraInstance(definition, managed, options, invocation.actor(), target, clock) { retired -> lifecycle.locked { entries.remove(key, retired) }; Unit }
                        .also { entries[key] = it; created = true }
                }
            }
            if (created) { instance.start(); instance.started }
            else if (options.overwrite) {
                instance.remove(SkillRemovalReason.OVERWRITTEN)
                instance.finished.thenCompose { apply(definition, invocation, options.copy(overwrite = false), context) }
            } else instance.started.thenCompose { instance.reapply(options, context) }
        } catch (error: Throwable) { CompletableFuture.failedFuture(error) }
    }
    override fun close() {
        val all = lifecycle.locked {
            if (bound == null) { store.stopped = true; entries.values.toList() }
            else entries.values.filter { it.managed.originId == bound.state.root.id }
        }
        all.forEach { it.remove(if (bound == null) SkillRemovalReason.ENGINE_STOPPED else SkillRemovalReason.CANCELLED) }
    }
}
