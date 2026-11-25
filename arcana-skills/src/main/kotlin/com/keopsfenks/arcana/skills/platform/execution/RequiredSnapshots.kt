package com.keopsfenks.arcana.skills.platform.execution
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.targets.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.*
import com.keopsfenks.arcana.core.skills.platform.SkillSnapshotRequest.Kind

/** Enrich only declared immutable fields; predicates never receive live world access. */
object RequiredSnapshots {
    fun capture(input: SkillInvocation, requests: List<CompiledSnapshotRequest>, scheduler: SkillScheduler, state: PlatformExecutionState, frame: InvocationFrame?, platform: DefaultSkillPlatform?): CompletableFuture<SkillInvocation> {
        if (requests.isEmpty()) return CompletableFuture.completedFuture(input)
        val host = requireNotNull(platform); val values = LinkedHashMap(input.data())
        values.keys.removeIf { it.startsWith("arcana_snapshot_") }
        var result = CompletableFuture.completedFuture(Unit)
        for (request in requests) result = result.thenCompose {
            check(state, frame)
            val actor = request.request.role().reference(input).orElse(null)
            val captured = if (actor == null) CompletableFuture.completedFuture(null) else captureOne(input, actor, request, scheduler, state, frame, host)
            captured.thenApply { value -> check(state, frame); if (value != null) values[request.alias] = value; Unit }
        }
        return result.thenApply { input.derive().replaceData(values).build() }
    }
    fun local(input: SkillInvocation, requests: List<CompiledSnapshotRequest>, platform: DefaultSkillPlatform?): SkillInvocation {
        if (requests.isEmpty()) return input
        val host = requireNotNull(platform); val values = LinkedHashMap(input.data())
        for (request in requests.filter { it.local }) {
            val actor = request.request.role().reference(input).orElse(null) ?: continue
            val value = localValue(input, actor, request, host)
            if (value != null) values[request.alias] = value
        }
        return input.derive().replaceData(values).build()
    }
    private fun check(state: PlatformExecutionState, frame: InvocationFrame?) { state.checkActive(); frame?.checkActive() }
    private fun <T> track(work: ScheduledWork<T>, state: PlatformExecutionState, frame: InvocationFrame?) = state.track(frame?.track(work) ?: work)
    private fun valid(request: CompiledSnapshotRequest, platform: DefaultSkillPlatform) {
        request.resource?.let { if (!platform.componentActive(it)) throw PlatformDependencyException("Resource snapshot provider retired") }
        request.stat?.let { if (!platform.componentActive(it)) throw PlatformDependencyException("Stat snapshot provider retired") }
    }
    private fun captureOne(input: SkillInvocation, actor: SkillActor, request: CompiledSnapshotRequest, scheduler: SkillScheduler, state: PlatformExecutionState, frame: InvocationFrame?, platform: DefaultSkillPlatform): CompletableFuture<SkillValue?> {
        val kind = request.request.kind()
        if (kind == Kind.LINE_OF_SIGHT) return sight(input, actor, request, scheduler, state, frame)
        if (kind in setOf(Kind.BIOME, Kind.TIME, Kind.WEATHER)) {
            val point = input.data()["${request.request.role().prefix()}_location"]?.location() ?: return CompletableFuture.completedFuture(null)
            return track(scheduler.global { check(state, frame); Bukkit.getWorld(point.world()) }, state, frame).thenCompose { world ->
                if (world == null) CompletableFuture.completedFuture(null) else if (kind == Kind.BIOME) {
                    track(scheduler.at(Location(world, point.x(), point.y(), point.z())) {
                        check(state, frame)
                        if (!world.isChunkLoaded(kotlin.math.floor(point.x()).toInt() shr 4, kotlin.math.floor(point.z()).toInt() shr 4)) null
                        else SkillValue.of(world.getBiome(kotlin.math.floor(point.x()).toInt(), kotlin.math.floor(point.y()).toInt(), kotlin.math.floor(point.z()).toInt()).key.toString())
                    }, state, frame)
                } else track(scheduler.global {
                    check(state, frame)
                    if (kind == Kind.TIME) SkillValue.of(Math.floorMod(world.time, 24000L))
                    else SkillValue.of(if (world.isThundering) "thunder" else if (world.hasStorm()) "rain" else "clear")
                }, state, frame)
            }
        }
        if (kind == Kind.RESOURCE || kind == Kind.STAT) {
            val work = track(scheduler.on(actor.entity()) {
                check(state, frame); valid(request, platform)
                if (!InvocationSnapshots.valid(actor.entity())) CompletableFuture.completedFuture(null)
                else if (kind == Kind.RESOURCE) request.resource!!.component.balance(actor).thenApply { number -> require(number.isFinite()) { "Nonfinite resource balance" }; SkillValue.of(number) }
                else {
                    val key = request.request.options().getValue("key").key()
                    request.stat!!.component.snapshot(actor, setOf(key)).thenApply { snapshot ->
                        require(snapshot.size <= 256 && snapshot.values.all { it.isFinite() }) { "Invalid stat predicate snapshot" }
                        snapshot[key]?.let(SkillValue::of)
                    }
                }
            }, state, frame)
            return work.thenCompose { it }.handle { value, error ->
                check(state, frame); valid(request, platform)
                if (error == null) value else if (unsupported(error)) null else throw CompletionException(error)
            }
        }
        return track(scheduler.on(actor.entity()) {
            check(state, frame); if (!InvocationSnapshots.valid(actor.entity())) null else localValue(input, actor, request, platform)
        }, state, frame)
    }
    private fun localValue(input: SkillInvocation, actor: SkillActor, request: CompiledSnapshotRequest, platform: DefaultSkillPlatform): SkillValue? {
        valid(request, platform)
        val entity = actor.entity(); val argument = request.request.argument()
        val value: SkillValue? = when (request.request.kind()) {
            Kind.PERMISSION -> SkillValue.of(actor.player().orElse(null)?.hasPermission(argument) == true)
            Kind.FOOD -> actor.player().orElse(null)?.let { SkillValue.of(it.foodLevel) }
            Kind.MOVEMENT -> when (argument) {
                "sneaking" -> actor.player().orElse(null)?.let { SkillValue.of(it.isSneaking) }
                "sprinting" -> actor.player().orElse(null)?.let { SkillValue.of(it.isSprinting) }
                "swimming" -> SkillValue.of(entity.isSwimming)
                "gliding" -> SkillValue.of(entity.isGliding)
                "on_ground" -> SkillValue.of(entity.isOnGround)
                else -> null
            }
            Kind.EQUIPMENT -> actor.player().orElse(null)?.let { player ->
                val inventory = player.inventory
                val item = when (argument) { "main_hand" -> inventory.itemInMainHand; "off_hand" -> inventory.itemInOffHand; "helmet" -> inventory.helmet; "chestplate" -> inventory.chestplate; "leggings" -> inventory.leggings; "boots" -> inventory.boots; else -> null }
                item?.let { SkillValue.of(mapOf("material" to it.type.name, "amount" to it.amount)) }
            }
            Kind.RESOURCE -> try { require(request.resource!!.component.supportsLocal()); val balance = request.resource.component.balanceLocal(actor); require(balance.isFinite()); SkillValue.of(balance) } catch (error: UnsupportedOperationException) { null }
            Kind.STAT -> {
                require(request.stat!!.component.supportsLocal())
                val key = request.request.options().getValue("key").key(); val values = request.stat.component.snapshotLocal(actor, setOf(key))
                require(values.size <= 256 && values.values.all { it.isFinite() }); values[key]?.let(SkillValue::of)
            }
            Kind.AURAS -> {
                val options = request.request.options(); val name = options["name"]?.key(); val type = options["aura-type"]?.key(); val tag = options["tag"]?.key()
                val own = options["caster"]?.text() != "any"
                val matching = platform.auras.instances(actor).filter { aura -> (name == null || aura.definition.key == name) && (type == null || aura.definition.type == type) && (tag == null || tag in aura.definition.tags) && (!own || aura.caster == input.actor()) }
                SkillValue.of(matching.map { mapOf("stacks" to it.stacks, "charges" to it.charges) })
            }
            else -> throw UnsupportedOperationException("Snapshot requires an asynchronous owner handoff")
        }
        valid(request, platform); return value
    }
    private fun sight(input: SkillInvocation, actor: SkillActor, request: CompiledSnapshotRequest, scheduler: SkillScheduler, state: PlatformExecutionState, frame: InvocationFrame?): CompletableFuture<SkillValue?> {
        val otherRole = SkillSnapshotRequest.Role.valueOf(request.request.argument())
        val other = otherRole.reference(input).orElse(null) ?: return CompletableFuture.completedFuture(null)
        fun eyes(subject: SkillActor) = track(scheduler.on(subject.entity()) {
            check(state, frame)
            if (!InvocationSnapshots.valid(subject.entity())) null else subject.entity().eyeLocation.let { SkillLocation(requireNotNull(it.world).uid, it.x, it.y, it.z) }
        }, state, frame)
        return eyes(actor).thenCompose { from -> eyes(other).thenCompose { to ->
            val limit = request.request.options()["max-distance"]?.number() ?: 256.0
            if (from == null || to == null) CompletableFuture.completedFuture(null)
            else if (TargetGeometry.distanceSquared(from, to) > limit*limit) CompletableFuture.completedFuture(SkillValue.of(false))
            else {
                val query = RegionQuery(scheduler, state = state, frame = frame)
                query.loaded(com.keopsfenks.arcana.skills.platform.projectiles.CollisionResolver.bounds(from, to, 0.0)).thenCompose { loaded ->
                    if (!loaded) CompletableFuture.completedFuture(SkillValue.of(false)) else query.collisions(from, to, 0.0).thenApply { hits -> SkillValue.of(hits.none { it.actor().isEmpty }) }
                }.toCompletableFuture()
            }
        } }
    }
    private fun unsupported(error: Throwable): Boolean {
        var value = error
        while ((value is CompletionException || value is ExecutionException) && value.cause != null) value = value.cause!!
        return value is UnsupportedOperationException
    }
}
