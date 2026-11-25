package com.keopsfenks.arcana.skills.components.actions
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import net.kyori.adventure.text.Component
import java.util.regex.Pattern

internal object NativeText {
    private val tokens = Pattern.compile("\\{(actor|player|target|skill|param\\.[a-z0-9_]{1,100}|data\\.[a-z0-9_]{1,100})}")
    fun template(args: SkillArguments, key: String): String = args.string(key).also { require(it.isNotBlank() && it.length <= 8192) { "${args.path}.$key: expected 1..8192 characters" } }
    private fun value(key: String, input: SkillInvocation): String {
        val value = when {
            key.startsWith("param.") -> input.parameters()[key.substringAfter('.')]
            key.startsWith("data.") -> input.data()[key.substringAfter('.')]
            key == "actor" || key == "player" -> input.data()["actor_name"]
            key == "target" -> input.data()["target_name"]
            else -> input.data()["skill"]
        }
        return value?.plain()?.toString().orEmpty().also { require(it.length <= 32768) { "Text placeholder exceeds 32768 characters" } }
    }
    fun render(template: String, input: SkillInvocation, parser: (String) -> Component): Component {
        var size = template.length
        return parser(template).replaceText { builder -> builder.match(tokens).replacement { match, _ ->
            val text = value(match.group(1), input); size += text.length; require(size <= 32768) { "Expanded text exceeds 32768 characters" }; Component.text(text)
        } }
    }
    fun command(template: String, input: SkillInvocation): String {
        var size = template.length
        val result = tokens.matcher(template).replaceAll { match ->
            val text = value(match.group(1), input); size += text.length; require(size <= 32768) { "Expanded command exceeds 32768 characters" }; java.util.regex.Matcher.quoteReplacement(text)
        }
        return com.keopsfenks.arcana.skills.effects.EffectValues.command(result)
    }
    fun duration(args: SkillArguments, compiler: SkillCompilation, key: String, fallback: String) = NodeOptions.duration(if (args.has(key)) args else SkillArguments(args.values + (key to fallback), args.path), compiler, key)
}
