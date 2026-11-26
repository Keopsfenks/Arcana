package com.keopsfenks.arcana.skills.platform.targets

import com.keopsfenks.arcana.core.skills.platform.*
import kotlin.math.*

/** Pure finite world geometry. Minecraft yaw/pitch are degrees, forward at yaw=0 points +Z. */
object TargetGeometry {
    fun distanceSquared(one: SkillLocation, two: SkillLocation): Double = if (one.world() != two.world()) Double.POSITIVE_INFINITY else
        (one.x() - two.x()).pow(2) + (one.y() - two.y()).pow(2) + (one.z() - two.z()).pow(2)
    fun forward(location: SkillLocation): SkillVector {
        val yaw = Math.toRadians(location.yaw().toDouble()); val pitch = Math.toRadians(location.pitch().toDouble())
        return SkillVector(-sin(yaw) * cos(pitch), -sin(pitch), cos(yaw) * cos(pitch))
    }
    fun offset(location: SkillLocation, vector: SkillVector, scale: Double = 1.0) = SkillLocation(location.world(),
        location.x() + vector.x() * scale, location.y() + vector.y() * scale, location.z() + vector.z() * scale, location.yaw(), location.pitch())
    fun center(invocation: SkillInvocation): SkillLocation = invocation.location().orElse(null) ?: invocation.origin().orElse(null)
        ?: invocation.data()["actor_location"]?.location() ?: throw IllegalArgumentException("Targeter requires a location snapshot")
    fun radius(center: SkillLocation, radius: Double) = SkillBounds(center.world(), center.x() - radius, center.y() - radius, center.z() - radius,
        center.x() + radius, center.y() + radius, center.z() + radius)
    fun inCone(point: SkillLocation, origin: SkillLocation, direction: SkillVector, radius: Double, angle: Double): Boolean {
        require(radius.isFinite() && radius >= 0 && angle.isFinite() && angle in 0.0..360.0)
        val magnitude = sqrt(direction.x().pow(2) + direction.y().pow(2) + direction.z().pow(2))
        require(magnitude > 1e-12) { "Cone direction must not be zero" }
        val distance = distanceSquared(point, origin)
        if (distance > radius * radius + 1e-9) return false
        if (distance <= 1e-18) return true
        val dot = ((point.x() - origin.x()) * direction.x() + (point.y() - origin.y()) * direction.y() + (point.z() - origin.z()) * direction.z()) / (sqrt(distance) * magnitude)
        return dot + 1e-12 >= cos(Math.toRadians(angle / 2))
    }
    fun inLine(point: SkillLocation, start: SkillLocation, end: SkillLocation, width: Double): Boolean {
        require(width.isFinite() && width >= 0)
        if (point.world() != start.world() || start.world() != end.world()) return false
        val dx = end.x() - start.x(); val dy = end.y() - start.y(); val dz = end.z() - start.z()
        val length = dx * dx + dy * dy + dz * dz
        if (length <= 1e-18) return distanceSquared(point, start) <= width * width
        val projection = ((point.x() - start.x()) * dx + (point.y() - start.y()) * dy + (point.z() - start.z()) * dz) / length
        if (projection < -1e-12 || projection > 1 + 1e-12) return false
        val nearest = SkillLocation(start.world(), start.x() + dx * projection, start.y() + dy * projection, start.z() + dz * projection)
        return distanceSquared(point, nearest) <= width * width + 1e-9
    }
}
