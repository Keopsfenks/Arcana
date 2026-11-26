package com.keopsfenks.arcana.skills.components.providers

import org.bukkit.entity.Player

data class ExperienceProgress(val level: Int, val progress: Float)

/** Integer point balances derived from current level/progress, never lifetime collected experience. */
object VanillaExperience {
    fun pointsForLevel(level: Int): Long {
        require(level in 0..32768) { "Experience level exceeds the supported point range" }
        val value = level.toLong()
        return when {
            level <= 16 -> value * value + 6 * value
            level <= 31 -> (5 * value * value - 81 * value + 720) / 2
            else -> (9 * value * value - 325 * value + 4440) / 2
        }
    }
    private fun next(level: Int): Int = when { level <= 15 -> 2 * level + 7; level <= 30 -> 5 * level - 38; else -> 9 * level - 158 }
    fun total(level: Int, progress: Float): Int {
        require(progress.isFinite() && progress in 0f..1f) { "Experience progress must be in [0,1]" }
        val total = pointsForLevel(level) + Math.round(progress.toDouble() * next(level))
        require(total in 0..Int.MAX_VALUE.toLong()) { "Experience balance exceeds the supported point range" }
        return total.toInt()
    }
    fun split(points: Int): ExperienceProgress {
        require(points >= 0)
        var low = 0; var high = 32768
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (pointsForLevel(middle) <= points) low = middle else high = middle - 1
        }
        return ExperienceProgress(low, ((points - pointsForLevel(low)).toDouble() / next(low)).toFloat())
    }
    fun read(player: Player) = total(player.level, player.exp)
    fun write(player: Player, points: Int) { val result = split(points); player.level = result.level; player.exp = result.progress }
}
