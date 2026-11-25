package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.concurrent.CompletableFuture

class SoundMechanic : SkillMechanic(SkillDescriptor.of("sound", "Play a resource-key sound on the owning location")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "sound", "volume", "pitch"); val location = WorldActions.location(arguments)
        val sound = sound(arguments); val volume = NativeActions.number(arguments, compilation, "volume", 1, 0.0, 100.0); val pitch = NativeActions.number(arguments, compilation, "pitch", 1, .5, 2.0)
        return CompiledSkillMechanic { context -> val input = context.invocation(); val at = location(input)
            if (at == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Sound location missing")) else WorldActions.at(context, at) { point -> point.world!!.playSound(point, sound, volume.evaluate(input).toFloat(), pitch.evaluate(input).toFloat()); SkillResult.success() }
        }
    }
    companion object { internal fun sound(args: SkillArguments): String { val key = args.string("sound"); require(key.matches(Regex("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+"))) { "${args.path}.sound: expected resource key" }; return if (':' in key) key else "minecraft:$key" } }
}
