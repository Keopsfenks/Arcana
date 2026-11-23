package com.keopsfenks.arcana.skills.platform

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.legacy.LegacyActorException
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistry
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

class DefaultSkillPlatform(private val engine: DefaultSkillEngine, private val lifecycle: SkillLifecycleGate) : SkillPlatform {
    private val registries = ConcurrentHashMap<SkillComponentType<*>, PlatformRegistry<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : SkillPlatformComponent> registry(type: SkillComponentType<T>): PlatformRegistry<T> =
        registries.computeIfAbsent(type) { PlatformRegistry(type, lifecycle) { engine.invalidateRegistration(it) } } as PlatformRegistry<T>

    override fun <T : SkillPlatformComponent> register(owner: Plugin, type: SkillComponentType<T>, key: SkillKey, component: T): SkillRegistration =
        registry(type).register(owner, key, component)

    internal fun registrations(): List<SkillRegistration> = registries.values.flatMap { it.snapshot() }

    override fun cast(key: SkillKey, invocation: SkillInvocation): CompletionStage<SkillResult> {
        if (key.namespace() != "arcana") return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill: $key"))
        return try { engine.cast(key.value(), LegacyInvocationAdapter.toLegacy(invocation)) }
        catch (error: LegacyActorException) { CompletableFuture.completedFuture(error.result) }
    }

    override fun trigger(key: SkillKey, invocation: SkillInvocation): CompletionStage<List<SkillResult>> {
        if (key.namespace() != "arcana") return CompletableFuture.completedFuture(emptyList())
        return try { engine.trigger(key.value(), LegacyInvocationAdapter.toLegacy(invocation)) }
        catch (error: LegacyActorException) { CompletableFuture.completedFuture(listOf(error.result)) }
    }
}
