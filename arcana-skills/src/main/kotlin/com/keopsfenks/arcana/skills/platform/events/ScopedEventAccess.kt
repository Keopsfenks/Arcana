package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.platform.SkillEventAccess
import org.bukkit.event.Event
import org.bukkit.event.Cancellable
import org.bukkit.event.entity.EntityDamageEvent

class ScopedEventAccess(event: Event) : SkillEventAccess, AutoCloseable {
    private var event: Event? = event
    private val thread = Thread.currentThread()
    private fun current(): Event { check(Thread.currentThread() === thread) { "Event facade used outside its callback thread" }; return checkNotNull(event) { "Event callback has returned" } }
    override fun supportsDamage() = current() is EntityDamageEvent
    override fun damage() = (current() as? EntityDamageEvent ?: error("Event does not support damage")).damage
    override fun damage(amount: Double) { require(amount.isFinite() && amount >= 0 && amount <= 1_000_000) { "Damage must be finite and between 0 and 1000000" }; (current() as? EntityDamageEvent ?: error("Event does not support damage")).damage = amount }
    override fun supportsCancellation() = current() is Cancellable
    override fun cancelled() = (current() as? Cancellable)?.isCancelled ?: false
    override fun cancelled(value: Boolean) { (current() as? Cancellable ?: error("Event cannot be cancelled")).isCancelled = value }
    override fun close() { event = null }
}
