package com.keopsfenks.arcana.skills.platform.projectiles
import com.keopsfenks.arcana.core.skills.platform.*
import kotlin.math.*

object CollisionResolver {
    fun bounds(from: SkillLocation, to: SkillLocation, radius: Double) = SkillBounds(from.world(), min(from.x(), to.x())-radius, min(from.y(), to.y())-radius, min(from.z(), to.z())-radius, max(from.x(), to.x())+radius, max(from.y(), to.y())+radius, max(from.z(), to.z())+radius)
    fun ordered(hits: List<SkillCollision>, caster: SkillActor, hitCaster: Boolean): List<SkillCollision> {
        if (hits.size > 4096) throw com.keopsfenks.arcana.skills.platform.execution.PlatformBudgetException("Projectile segment exceeds 4096 contacts")
        return hits.filter { hitCaster || it.actor().orElse(null) != caster }.distinctBy { it.actor().map { a -> a.id().toString() }.orElse("block:${it.location()}") }
            .sortedWith(compareBy<SkillCollision> { it.fraction() }.thenBy { it.actor().map { a -> a.id().toString() }.orElse("") })
    }
}
