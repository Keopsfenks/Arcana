package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.entity.Player

class StopSoundMechanic : SkillMechanic(SkillDescriptor.of("stop_sound", "Player-only stop of one named sound")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "sound"); val recipient = NativeActions.recipient(arguments); val sound = SoundMechanic.sound(arguments)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> if (entity !is Player) SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Stop sound requires a player") else { entity.stopSound(sound); SkillResult.success() } } }
    }
}
