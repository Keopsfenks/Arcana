package com.keopsfenks.arcana.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillReloadResult
import org.bukkit.event.HandlerList
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

class SkillsRuntime(
    private val plugin: ArcanaPlugin,
    val engine: DefaultSkillEngine,
    private val directory: Path,
    private val installCatalog: (DefaultSkillEngine, ArcanaPlugin) -> Unit = { _, _ -> }
) {
    @Volatile var isStarted = false
        private set
    private var startupFinished = false
    private var reloadCancelsTasks = false
    private val listener = SkillsLifecycleListener(engine)
    private val coordinator = SkillReloadCoordinator(engine, ::report)

    @Synchronized fun start() {
        if (isStarted) return
        try {
            copyExamples()
            installCatalog(engine, plugin)
            plugin.server.pluginManager.registerEvents(listener, plugin)
            coordinator.initialize()
            isStarted = true
        } catch (error: Exception) { engine.shutdown(); HandlerList.unregisterAll(listener); throw error }
    }

    fun prepareReload(cancelTasks: Boolean): Boolean {
        if (!isStarted) return true
        reloadCancelsTasks = cancelTasks
        return coordinator.prepare(allowEmptyStartup = !startupFinished)
    }

    fun afterReload() {
        if (!isStarted) return
        val result = coordinator.commit()
        if (reloadCancelsTasks) startupFinished = true
        check(result == null || result.success()) { "Skills reload was invalidated: ${result?.errors()?.joinToString()}" }
    }

    @Synchronized fun stop() {
        isStarted = false
        engine.shutdown()
        HandlerList.unregisterAll(listener)
    }

    private fun report(result: SkillReloadResult) {
        if (result.success()) plugin.logger.info("Arcana Skills: ${result.loaded()} skills loaded (${result.disabled()} disabled)")
        else result.errors().forEach { plugin.logger.warning("Arcana Skills reload rejected: $it") }
    }

    private fun copyExamples() {
        Files.createDirectories(directory)
        for (name in EXAMPLES) {
            val target = directory.resolve(name)
            if (Files.exists(target)) continue
            val source = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/$name")) { "Missing bundled skill example $name" }
            source.use {
                try { Files.copy(it, target) } catch (_: FileAlreadyExistsException) { /* Another startup already created it. */ }
            }
        }
    }

    companion object { val EXAMPLES = listOf("_welcome.yml", "_vampiric_strike.yml", "_combo.yml") }
}
