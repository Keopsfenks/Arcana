package com.keopsfenks.arcana.skills.platform.projectiles
import java.util.UUID

class ProjectileHitLedger(private val maximum: Int, private val immuneTicks: Long) {
    private val seen = HashMap<UUID, Long>(); var total = 0; private set
    init { require(maximum in 1..256 && immuneTicks in 0..1_728_000) }
    @Synchronized fun accept(actor: UUID?, tick: Long): Boolean {
        if (total >= maximum || actor != null && seen[actor]?.let { tick-it < immuneTicks } == true) return false
        total++; if (actor != null) seen[actor] = tick; return true
    }
    @Synchronized fun exhausted() = total >= maximum
}
