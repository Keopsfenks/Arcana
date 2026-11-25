package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class CustomSkillTrigger(id: String = "custom") : SkillPlatformTrigger(SkillDescriptor.of(id, "Namespaced immutable API event"), emptySet()) {
    private val listener = AtomicReference<Consumer<SkillInvocation>?>()
    override fun listen(consumer: Consumer<SkillInvocation>): AutoCloseable { check(listener.compareAndSet(null, consumer)); return AutoCloseable { listener.compareAndSet(consumer, null) } }
    fun fire(input: SkillInvocation) { listener.get()?.accept(input) }
}
