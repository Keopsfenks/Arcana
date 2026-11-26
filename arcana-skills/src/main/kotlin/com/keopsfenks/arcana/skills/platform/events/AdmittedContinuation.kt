package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/** Contains snapshots and an exact committed admission, never an Event or its facade. */
class AdmittedContinuation internal constructor(private val context: PlatformExecutionContext, private val revision: DefinitionRevision,
    private val supplied: com.keopsfenks.arcana.core.skills.platform.SkillInvocation) : AutoCloseable {
    private val claimed = AtomicBoolean()
    val actor get() = context.frame.invocation.actor()
    fun resume(values: Map<String, com.keopsfenks.arcana.core.skills.platform.SkillValue> = emptyMap()): CompletableFuture<SkillResult> {
        if (!claimed.compareAndSet(false, true)) return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.CANCELLED, "Continuation already consumed"))
        val state = context.state
        try {
            state.checkActive(); context.frame.checkActive()
            val accepted = context.platform.admissions.reuse(requireNotNull(context.admission), revision, context.invocation().actor())
            if (!accepted.result.isSuccess) state.root.result.complete(accepted.result)
            else {
                require(values.keys.all { it == "damage" }) { "Unsupported event snapshot field" }
                val restored = context.invocation().derive().target(supplied.target().orElse(null)).trigger(supplied.trigger().orElse(null))
                    .selection(supplied.selection()).data(values).build()
                context.derived(restored).thenCompose { it.execute(context.definition.effects) }.whenComplete { value, error ->
                    state.root.result.complete(if (error == null) value else PlatformExecutionState.failure(error))
                }
            }
        } catch (error: Throwable) { state.root.result.complete(PlatformExecutionState.failure(error)) }
        return state.root.result
    }
    override fun close() { if (claimed.compareAndSet(false, true)) context.state.root.cancel("Event continuation discarded") }
}
