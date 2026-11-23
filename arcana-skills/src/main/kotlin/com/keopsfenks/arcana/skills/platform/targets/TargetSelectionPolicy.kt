package com.keopsfenks.arcana.skills.platform.targets

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.Locale
import java.util.function.DoubleSupplier
import kotlin.math.floor

class TargetSelectionPolicy private constructor(
    private val limit: SkillNumericExpression,
    private val sort: String,
    private val players: Boolean,
    private val excludeSelf: Boolean,
    private val types: Set<String>,
    private val predicate: SkillPredicate
) {
    fun apply(invocation: SkillInvocation, candidates: List<SkillEntitySnapshot>, locations: List<SkillLocation>, random: DoubleSupplier): SkillSelection {
        val count = limit.evaluate(invocation).also { require(it.isFinite() && it in 1.0..256.0 && floor(it) == it) { "Target limit must be an integer between 1 and 256" } }.toInt()
        val entities = candidates.distinctBy { it.actor().id() }.filter { candidate ->
            (!excludeSelf || candidate.actor() != invocation.actor()) && (!players || candidate.data()["player"]?.bool() == true) &&
                (types.isEmpty() || candidate.data()["type"]?.text() in types) && predicate.test(selected(invocation, candidate))
        }.toMutableList()
        val orderedLocations = locations.distinct().filter { predicate.test(selected(invocation, it)) }.toMutableList()
        val origin = if (sort == "nearest" || sort == "farthest") TargetGeometry.center(invocation) else null
        val distance: (SkillLocation) -> Double = { TargetGeometry.distanceSquared(requireNotNull(origin), it) }
        when (sort) {
            "nearest" -> { entities.sortWith(compareBy<SkillEntitySnapshot> { distance(it.location()) }.thenBy { it.actor().id().toString() }); orderedLocations.sortWith(compareBy<SkillLocation> { distance(it) }.thenBy { it.toString() }) }
            "farthest" -> { entities.sortWith(compareByDescending<SkillEntitySnapshot> { distance(it.location()) }.thenBy { it.actor().id().toString() }); orderedLocations.sortWith(compareByDescending<SkillLocation> { distance(it) }.thenBy { it.toString() }) }
            else -> { entities.sortBy { it.actor().id().toString() }; orderedLocations.sortBy { it.toString() } }
        }
        if (sort == "random") { shuffle(entities, random); shuffle(orderedLocations, random) }
        val actors = entities.take(count).map { it.actor() }
        return SkillSelection(actors, orderedLocations.take(count - actors.size))
    }
    private fun <T> shuffle(values: MutableList<T>, random: DoubleSupplier) {
        for (index in values.lastIndex downTo 1) {
            val roll = random.asDouble.also { require(it.isFinite() && it >= 0 && it < 1) { "Random source must return a finite value in [0,1)" } }
            val next = (roll * (index + 1)).toInt()
            val value = values[index]; values[index] = values[next]; values[next] = value
        }
    }
    companion object {
        val OPTIONS = arrayOf("limit", "sort", "players-only", "exclude-self", "types", "conditions")
        fun compile(args: SkillArguments, compiler: SkillCompilation, defaultSort: String = "uuid", excludeSelf: Boolean = false, playersOnly: Boolean = false): TargetSelectionPolicy {
            val raw = args.raw("limit", 64)
            if (raw is Number) require(raw.toDouble().isFinite() && raw.toDouble() in 1.0..256.0 && floor(raw.toDouble()) == raw.toDouble()) { "${args.path}.limit: expected an integer between 1 and 256" }
            val sort = args.string("sort", defaultSort).lowercase(Locale.ROOT)
            require(sort in setOf("uuid", "nearest", "farthest", "random")) { "${args.path}.sort: unknown target ordering" }
            val types = args.list("types", emptyList<Any>()).map { require(it is String && it.matches(Regex("[A-Z_]+"))) { "${args.path}.types: expected entity type names" }; it }.toSet()
            return TargetSelectionPolicy(compiler.number(raw, "${args.path}.limit"), sort, playersOnly || args.bool("players-only", false),
                args.bool("exclude-self", excludeSelf), types, compiler.predicates(args.list("conditions", emptyList<Any>()), "${args.path}.conditions"))
        }
        fun selected(invocation: SkillInvocation, snapshot: SkillEntitySnapshot): SkillInvocation {
            val values = LinkedHashMap(invocation.data())
            values.keys.removeIf { it.startsWith("target_") || it == "entity" }
            snapshot.data().forEach { (key, value) -> values["target_$key"] = value }
            values["target_location"] = SkillValue.of(snapshot.location())
            snapshot.data()["type"]?.let { values["entity"] = it }
            return invocation.derive().target(snapshot.actor()).location(snapshot.location()).replaceData(values).selection(SkillSelection(listOf(snapshot.actor()), emptyList())).build()
        }
        fun selected(invocation: SkillInvocation, location: SkillLocation): SkillInvocation {
            val values = LinkedHashMap(invocation.data())
            values.keys.removeIf { it.startsWith("target_") || it == "entity" }
            return invocation.derive().target(null).location(location).replaceData(values).selection(SkillSelection(emptyList(), listOf(location))).build()
        }
    }
}
