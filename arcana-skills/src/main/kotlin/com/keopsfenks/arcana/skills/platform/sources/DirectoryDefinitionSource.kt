package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/** Safe native documents; component compilation belongs to the staged repository transaction. */
class DirectoryDefinitionSource(private val key: SkillKey, private val directory: Path) : SkillDefinitionSource {
    override fun key() = key
    override fun load(): List<SkillDefinition> {
        Files.createDirectories(directory)
        val files = Files.walk(directory).use { paths ->
            paths.filter { Files.isRegularFile(it) && !it.fileName.toString().startsWith("_") &&
                it.fileName.toString().lowercase(Locale.ROOT).let { name -> name.endsWith(".yml") || name.endsWith(".yaml") }
            }.sorted().toList()
        }
        require(files.size <= 1000) { "$directory: maximum 1000 skill files allowed" }
        val errors = mutableListOf<String>()
        val definitions = mutableListOf<SkillDefinition>()
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
                val id = SkillKey.parse(arguments.string("id"), key.namespace())
                require(id.namespace() == key.namespace()) { "definition namespace must match source $key" }
                definitions.add(SkillDefinition(id, arguments.values, file.toString()))
            } catch (error: Exception) { errors.add("$file: ${error.message ?: error.javaClass.simpleName}") }
        }
        require(errors.isEmpty()) { errors.joinToString("\n") }
        return java.util.List.copyOf(definitions)
    }
}
