package com.keopsfenks.arcana.skills.platform

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.legacy.LegacyActorException
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistry
import com.keopsfenks.arcana.skills.platform.sources.*
import com.keopsfenks.arcana.skills.platform.compile.SourceSkillCompiler
import com.keopsfenks.arcana.skills.config.SkillLoadResult
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import java.nio.file.Path
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

class DefaultSkillPlatform(
    private val engine: DefaultSkillEngine,
    private val lifecycle: SkillLifecycleGate,
    owner: ArcanaPlugin,
    directory: Path,
    environment: ExpressionEnvironment
) : SkillPlatform {
    private val registries = ConcurrentHashMap<SkillComponentType<*>, PlatformRegistry<*>>()
    val repository = DefinitionRepository(lifecycle, engine::compileSourceLegacy,
        { definition -> SourceSkillCompiler(this, environment, definition.key().namespace()).compile(definition) },
        engine::publishDefinitions, engine::invalidateRegistration)

    init { repository.register(owner, DirectoryDefinitionSource(SkillKey("arcana", "default"), directory), core = true) }

    @Suppress("UNCHECKED_CAST")
    fun <T : SkillPlatformComponent> registry(type: SkillComponentType<T>): PlatformRegistry<T> =
        registries.computeIfAbsent(type) { PlatformRegistry(type, lifecycle) { engine.invalidateRegistration(it) } } as PlatformRegistry<T>

    override fun <T : SkillPlatformComponent> register(owner: Plugin, type: SkillComponentType<T>, key: SkillKey, component: T): SkillRegistration =
        registry(type).register(owner, key, component)

    internal fun registrations(): List<SkillRegistration> = registries.values.flatMap { it.snapshot() } + repository.registrations()
    override fun registerSource(owner: Plugin, source: SkillDefinitionSource) = repository.register(owner, source)
    override fun reloadSource(source: SkillKey): SkillReloadResult = repository.commit(repository.prepare(setOf(source)))
    override fun definitions() = repository.definitions()
    internal fun prepareReload(): SkillLoadResult {
        val candidate = repository.prepare()
        return SkillLoadResult(candidate.snapshot?.legacy, candidate.report, candidate)
    }
    internal fun legacyTrigger(key: SkillKey): SkillRegistration? = if (key.namespace() == "arcana") engine.triggers.get(key.value()) else null

    override fun cast(key: SkillKey, invocation: SkillInvocation): CompletionStage<SkillResult> {
        return try { engine.cast(DefinitionRevision.legacyId(key), LegacyInvocationAdapter.toLegacy(invocation)) }
        catch (error: LegacyActorException) { CompletableFuture.completedFuture(error.result) }
    }

    override fun trigger(key: SkillKey, invocation: SkillInvocation): CompletionStage<List<SkillResult>> {
        return try { engine.trigger(DefinitionRevision.legacyId(key), LegacyInvocationAdapter.toLegacy(invocation)) }
        catch (error: LegacyActorException) { CompletableFuture.completedFuture(listOf(error.result)) }
    }
}
