package com.keopsfenks.arcana.skills.components.actions
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileMotion
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector

internal object MovementActions {
    fun bounded(vector: SkillVector): Vector {
        val value = if (ProjectileMotion.length(vector) > 16.0) ProjectileMotion.scale(ProjectileMotion.unit(vector), 16.0) else vector
        return Vector(value.x(), value.y(), value.z())
    }
    fun impulse(input: SkillInvocation, entity: LivingEntity, strength: Double, pull: Boolean): Vector {
        val from = input.data()["actor_location"]?.location() ?: input.origin().orElseThrow()
        val at = entity.location
        if (from.world() != at.world?.uid) return Vector()
        val sign = if (pull) -1.0 else 1.0
        val direction = SkillVector(at.x-from.x(), at.y-from.y(), at.z-from.z())
        return bounded(ProjectileMotion.scale(ProjectileMotion.unit(direction), strength*sign))
    }
}
