package com.keopsfenks.arcana.skills.components.effects.presentation
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeText
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

class ConsoleCommandMechanic(private val dispatch: (String) -> Boolean = { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it) }) : SkillMechanic(SkillDescriptor.of("console_command", "Dispatch one literal console command on the global scheduler")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "command"); val template = EffectValues.command(NativeText.template(arguments, "command"))
        return CompiledSkillMechanic { context ->
            if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Console command requires Arcana execution"))
            else { val command = NativeText.command(template, context.invocation()); context.track(context.engine.scheduler.global { context.state.checkActive(); context.frame.checkActive(); if (dispatch(command)) SkillResult.success() else SkillResult.of(SkillResult.Status.ERROR, "Server rejected console command") }) }
        }
    }
}
