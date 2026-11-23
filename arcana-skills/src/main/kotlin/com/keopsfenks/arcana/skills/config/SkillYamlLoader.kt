package com.keopsfenks.arcana.skills.config

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.SkillReloadResult
import com.keopsfenks.arcana.core.skills.SkillRegistration
import java.util.Collections
import com.keopsfenks.arcana.skills.compile.CompiledSkill
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

data class SkillLoadResult(val snapshot: SkillSnapshot?, val report: SkillReloadResult)

class SkillYamlLoader(private val compiler: () -> SkillCompilerImpl) {
    fun load(directory: Path): SkillLoadResult {
        val errors = mutableListOf<String>()
        val compiled = LinkedHashMap<String, CompiledSkill>()
        try {
            Files.createDirectories(directory)
            val files = Files.walk(directory).use { paths ->
                paths.filter { Files.isRegularFile(it) && !it.fileName.toString().startsWith("_") &&
                    it.fileName.toString().lowercase(Locale.ROOT).let { name -> name.endsWith(".yml") || name.endsWith(".yaml") }
                }.sorted().toList()
            }
            require(files.size <= 1000) { "Maximum 1000 skill files allowed" }
            for (file in files) {
                try {
                    require(Files.size(file) <= 1_048_576) { "file exceeds 1 MiB" }
                    val options = LoaderOptions().apply {
                        isAllowDuplicateKeys = false
                        maxAliasesForCollections = 10
                        nestingDepthLimit = 64
                        codePointLimit = 1_048_576
                    }
                    val raw = Files.newBufferedReader(file).use { Yaml(SafeConstructor(options)).load<Any>(it) }
                    val arguments = SkillArguments(emptyMap<String, Any>(), file.toString()).child("", raw)
                    val skill = compiler().compile(arguments)
                    require(!compiled.containsKey(skill.id)) { "duplicate skill ID ${skill.id} (also in ${compiled[skill.id]?.source})" }
                    compiled[skill.id] = skill
                } catch (error: Exception) {
                    errors.add("$file: ${error.message ?: error.javaClass.simpleName}")
                }
            }
        } catch (error: Exception) {
            errors.add("$directory: ${error.message ?: error.javaClass.simpleName}")
        }
        for (skill in compiled.values) {
            for ((id, path) in skill.references) if (id !in compiled) errors.add("$path: unknown skill $id")
        }
        if (errors.isNotEmpty()) return SkillLoadResult(null, SkillReloadResult(false, 0, 0, errors))
        // Literal cast edges are known at load time. Walk iteratively so cycles cannot overflow
        // and owners of any reachable component invalidate the entire pending root invocation.
        val resolved = compiled.values.map { skill ->
            val dependencies = LinkedHashSet<SkillRegistration>()
            val visited = HashSet<String>()
            val pending = ArrayDeque<String>()
            pending.add(skill.id)
            while (pending.isNotEmpty()) {
                val id = pending.removeLast()
                if (!visited.add(id)) continue
                val child = compiled.getValue(id)
                dependencies.addAll(child.dependencies)
                pending.addAll(child.references.keys)
            }
            skill.copy(dependencies = Collections.unmodifiableSet(dependencies))
        }
        return SkillLoadResult(SkillSnapshot(resolved), SkillReloadResult(true, compiled.size, compiled.values.count { !it.enabled }, emptyList()))
    }
}
