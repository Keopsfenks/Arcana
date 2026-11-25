package com.keopsfenks.arcana.skills.platform.projectiles
import com.keopsfenks.arcana.core.skills.platform.*
import kotlin.math.*

object ProjectileMotion {
    fun length(v: SkillVector) = sqrt(v.x()*v.x() + v.y()*v.y() + v.z()*v.z())
    fun scale(v: SkillVector, factor: Double) = SkillVector(v.x()*factor, v.y()*factor, v.z()*factor)
    fun unit(v: SkillVector): SkillVector = length(v).let { if (it < 1e-12) SkillVector(0.0, 0.0, 0.0) else scale(v, 1/it) }
    fun initial(options: ProjectileOptions): SkillVector {
        val direction = unit(options.direction)
        if (options.spreadDegrees == 0.0) return scale(direction, options.speed)
        val random = java.util.Random(options.seed)
        val yaw = atan2(direction.z(), direction.x()) + Math.toRadians((random.nextDouble()*2-1)*options.spreadDegrees)
        val pitch = asin(direction.y().coerceIn(-1.0, 1.0)) + Math.toRadians((random.nextDouble()*2-1)*options.spreadDegrees)
        return SkillVector(cos(pitch)*cos(yaw)*options.speed, sin(pitch)*options.speed, cos(pitch)*sin(yaw)*options.speed)
    }
    fun advance(current: SkillVector, options: ProjectileOptions, toward: SkillVector?): SkillVector {
        val velocity = if (toward == null || length(toward) < 1e-9) current else {
            val target = scale(unit(toward), length(current)); val mix = options.homing
            val blended = SkillVector(current.x()*(1-mix)+target.x()*mix, current.y()*(1-mix)+target.y()*mix, current.z()*(1-mix)+target.z()*mix)
            if (length(blended) < 1e-9) current else scale(unit(blended), length(current))
        }
        val value = SkillVector(velocity.x(), velocity.y()-options.gravity, velocity.z())
        return if (length(value) > 64) scale(unit(value), 64.0) else value
    }
    fun reflect(velocity: SkillVector, normal: SkillVector, energy: Double): SkillVector {
        val n = unit(normal); val dot = velocity.x()*n.x()+velocity.y()*n.y()+velocity.z()*n.z()
        return SkillVector((velocity.x()-2*dot*n.x())*energy, (velocity.y()-2*dot*n.y())*energy, (velocity.z()-2*dot*n.z())*energy)
    }
    fun move(from: SkillLocation, velocity: SkillVector) = SkillLocation(from.world(), from.x()+velocity.x(), from.y()+velocity.y(), from.z()+velocity.z(), from.yaw(), from.pitch())
}
