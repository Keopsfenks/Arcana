package com.keopsfenks.arcana.skills.components
import java.nio.file.*

/** Explicit startup helper; catalog registration itself never accesses the filesystem. */
object StandardSkillExamples {
    val NAMES = listOf("_v2_aura-shield.yml", "_v2_aura-periodic-buff.yml", "_v2_projectile-bolt.yml", "_v2_projectile-beam.yml", "_v2_projectile-chain.yml", "_v2_general-events.yml", "_v2_all-components-test.yml")
    fun copyTo(directory: Path) {
        Files.createDirectories(directory)
        for (name in NAMES) {
            val target = directory.resolve(name)
            if (Files.exists(target)) continue
            requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/v2/${name.removePrefix("_v2_")}")) { "Missing bundled example $name" }.use {
                try { Files.copy(it, target) } catch (_: FileAlreadyExistsException) { }
            }
        }
    }
}
