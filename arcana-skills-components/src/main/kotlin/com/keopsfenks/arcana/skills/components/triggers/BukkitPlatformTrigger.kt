package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.platform.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

abstract class BukkitPlatformTrigger(id: String, private val plugin: ArcanaPlugin, keys: Set<String>) : SkillPlatformTrigger(SkillDescriptor.of(id, "Immutable native $id event"), keys), Listener {
    private val listener = AtomicReference<Consumer<SkillInvocation>?>()
    override fun listen(consumer: Consumer<SkillInvocation>): AutoCloseable {
        check(listener.compareAndSet(null, consumer)) { "Trigger already listening" }
        try { plugin.server.pluginManager.registerEvents(this, plugin) } catch (error: Throwable) { listener.set(null); throw error }
        return AutoCloseable { if (listener.compareAndSet(consumer, null)) HandlerList.unregisterAll(this) }
    }
    protected fun emit(actor: LivingEntity, target: LivingEntity? = null, values: Map<String, Any> = emptyMap()) {
        val fire = listener.get() ?: return
        fire.accept(SkillInvocation.builder(SkillActor.of(actor)).target(target?.let(SkillActor::of)).data(values).build())
    }
}
