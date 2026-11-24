package com.keopsfenks.arcana.skills.platform.providers

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistration
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState
import com.keopsfenks.arcana.skills.platform.execution.PlatformDependencyException
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import java.util.concurrent.CompletableFuture

data class CompiledStatBinding(val alias: String, val provider: PlatformRegistration<SkillStatProvider>, val key: SkillKey, val defaultValue: Double?, val local: Boolean, val path: String)

object StatBindings {
    fun captureLocal(input: SkillInvocation, bindings: List<CompiledStatBinding>, platform: DefaultSkillPlatform): SkillInvocation {
        val values = LinkedHashMap(input.data())
        for ((provider, requested) in bindings.groupBy { it.provider }) {
            check(platform.componentActive(provider)) { "Stat provider ${provider.key} retired" }
            check(requested.all { it.local }) { "Stat provider is not local" }
            val snapshot = java.util.Map.copyOf(provider.component.snapshotLocal(input.actor(), java.util.Set.copyOf(requested.map { it.key })))
            require(snapshot.values.all { it.isFinite() }) { "Nonfinite stat snapshot" }
            requested.forEach { binding -> values[binding.alias] = SkillValue.of(snapshot[binding.key] ?: binding.defaultValue ?: error("${binding.path}: stat value unavailable")) }
        }
        return input.derive().replaceData(values).build()
    }
    fun capture(input: SkillInvocation, bindings: List<CompiledStatBinding>, state: PlatformExecutionState, frame: InvocationFrame, platform: DefaultSkillPlatform): CompletableFuture<SkillInvocation> {
        if (bindings.isEmpty()) return CompletableFuture.completedFuture(input)
        val values = LinkedHashMap(input.data())
        bindings.forEach { values.remove(it.alias) }
        var result = CompletableFuture.completedFuture(Unit)
        for ((provider, requested) in bindings.groupBy { it.provider }) result = result.thenCompose {
            state.checkActive(); frame.checkActive()
            if (!platform.componentActive(provider)) throw PlatformDependencyException("Stat provider ${provider.key} is unavailable")
            val keys = java.util.Set.copyOf(requested.map { it.key })
            provider.component.snapshot(input.actor(), keys).thenApply { snapshot ->
                state.checkActive(); frame.checkActive()
                if (!platform.componentActive(provider)) throw PlatformDependencyException("Stat provider ${provider.key} retired")
                val copied = java.util.Map.copyOf(snapshot)
                copied.values.forEach { require(it.isFinite()) { "Stat provider ${provider.key} returned a non-finite value" } }
                for (binding in requested) {
                    val value = copied[binding.key] ?: binding.defaultValue
                        ?: throw IllegalArgumentException("${binding.path}: provider ${provider.key} returned no value for ${binding.key}")
                    values[binding.alias] = SkillValue.of(value)
                }
                Unit
            }.toCompletableFuture()
        }
        return result.thenApply { input.derive().replaceData(values).build() }
    }
}
