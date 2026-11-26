package com.keopsfenks.arcana.skills.components.effects.projectiles

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.projectiles.*
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry

internal object ProjectileArguments {
    val HOOK_KEYS = arrayOf("on-start", "on-tick", "on-hit", "on-end", "hit-conditions")
    val MOTION_KEYS = arrayOf("speed", "direction", "gravity", "duration", "radius", "homing", "bounces", "bounce-energy", "pierce", "max-hits", "immune-delay", "hit-caster", "spread", "seed")
    class Hooks(args: SkillArguments, compiler: SkillCompilation) {
        val start = AuraSupport.hook(args, compiler, "on-start"); val tick = AuraSupport.hook(args, compiler, "on-tick")
        val hit = AuraSupport.hook(args, compiler, "on-hit"); val end = AuraSupport.hook(args, compiler, "on-end")
        val condition = compiler.predicates(args.list("hit-conditions", emptyList<Any>()), "${args.path}.hit-conditions")
        fun projectile() = ProjectileHooks(start, tick, hit, end, condition)
    }
    fun number(args: SkillArguments, compiler: SkillCompilation, key: String, fallback: Number, min: Double, max: Double, integer: Boolean = false) = NodeOptions.number(args, compiler, key, fallback, min, max, integer)
    fun direction(args: SkillArguments, compiler: SkillCompilation): (SkillInvocation) -> SkillVector {
        if (!args.has("direction")) return { TargetGeometry.forward(it.location().orElseGet { it.origin().orElseThrow() }) }
        val section = args.section("direction")
        section.values.keys.firstOrNull { it !in setOf("x", "y", "z") }?.let { throw section.error(it, "unknown direction coordinate") }
        val x = number(section, compiler, "x", 0, -1_000_000.0, 1_000_000.0); val y = number(section, compiler, "y", 0, -1_000_000.0, 1_000_000.0); val z = number(section, compiler, "z", 0, -1_000_000.0, 1_000_000.0)
        if (section.values.values.all { it is Number }) require((section.number("x", 0.0)*section.number("x", 0.0) + section.number("y", 0.0)*section.number("y", 0.0) + section.number("z", 0.0)*section.number("z", 0.0)) > 1e-18) { "${args.path}.direction: must be nonzero" }
        return { input -> val value = SkillVector(x.evaluate(input), y.evaluate(input), z.evaluate(input)); require(ProjectileMotion.length(value) > 1e-9) { "${args.path}.direction: must be nonzero" }; ProjectileMotion.unit(value) }
    }
    fun motion(args: SkillArguments, compiler: SkillCompilation, homingDefault: Double): (SkillInvocation) -> ProjectileOptions {
        val speed = number(args, compiler, "speed", 1, .001, 64.0); val direction = direction(args, compiler)
        val gravity = number(args, compiler, "gravity", 0, -16.0, 16.0); val duration = AuraSupport.duration(args, compiler, "duration", "5s")
        val radius = number(args, compiler, "radius", .1, 0.0, 16.0); val homing = number(args, compiler, "homing", homingDefault, 0.0, 1.0)
        val bounce = number(args, compiler, "bounces", 0, 0.0, 100.0, true); val energy = number(args, compiler, "bounce-energy", 1, .001, 1.0)
        val pierce = number(args, compiler, "pierce", 0, 0.0, 255.0, true); val max = number(args, compiler, "max-hits", 64, 1.0, 256.0, true)
        val immune = NodeOptions.duration(if (args.has("immune-delay")) args else SkillArguments(args.values + ("immune-delay" to "10t"), args.path), compiler, "immune-delay"); val spread = number(args, compiler, "spread", 0, 0.0, 180.0)
        val seed = args.number("seed", 0.0).also { require(it.isFinite() && kotlin.math.floor(it) == it && it in -9_007_199_254_740_991.0..9_007_199_254_740_991.0) { "${args.path}.seed: expected an exact integer" } }.toLong()
        val caster = args.bool("hit-caster", false)
        return { input -> ProjectileOptions(speed.evaluate(input), direction(input), gravity.evaluate(input), duration.evaluate(input).toLong(), radius.evaluate(input), homing.evaluate(input), bounce.evaluate(input).toInt(), energy.evaluate(input), pierce.evaluate(input).toInt(), max.evaluate(input).toInt(), immune.evaluate(input).toLong(), caster, spread.evaluate(input), seed) }
    }
}
