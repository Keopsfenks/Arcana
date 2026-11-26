package com.keopsfenks.arcana.skills.platform.projectiles
import com.keopsfenks.arcana.core.skills.platform.SkillVector
import kotlin.math.sqrt

/** Motion units are blocks per simulation tick; no implicit entity or chunk is created. */
data class ProjectileOptions(
    val speed: Double = 1.0, val direction: SkillVector = SkillVector(0.0, 0.0, 1.0),
    val gravity: Double = 0.0, val lifetimeTicks: Long = 100, val radius: Double = .1,
    val homing: Double = 0.0, val bounces: Int = 0, val bounceEnergy: Double = 1.0,
    val pierce: Int = 0, val maxHits: Int = 64, val immuneTicks: Long = 10,
    val hitCaster: Boolean = false, val spreadDegrees: Double = 0.0, val seed: Long = 0
) {
    init {
        require(speed.isFinite() && speed in .001..64.0) { "Projectile speed must be .001..64 blocks/tick" }
        require(gravity.isFinite() && gravity in -16.0..16.0)
        require(lifetimeTicks in 1..1_728_000 && radius.isFinite() && radius in 0.0..16.0)
        require(homing.isFinite() && homing in 0.0..1.0 && bounces in 0..100 && pierce in 0..255)
        require(bounceEnergy.isFinite() && bounceEnergy in .001..1.0 && maxHits in 1..256 && immuneTicks in 0..1_728_000)
        require(spreadDegrees.isFinite() && spreadDegrees in 0.0..180.0)
        val length = sqrt(direction.x()*direction.x() + direction.y()*direction.y() + direction.z()*direction.z())
        require(length.isFinite() && length > 1e-9) { "Projectile direction must be nonzero" }
    }
}
