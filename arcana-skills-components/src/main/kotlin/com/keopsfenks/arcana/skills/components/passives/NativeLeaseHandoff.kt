package com.keopsfenks.arcana.skills.components.passives
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/** Compensates a native write even when cancellation wins before its lease can be returned. */
internal class NativeLeaseHandoff {
    private val native = AtomicReference<SkillContributionLease?>()
    private val installed = AtomicBoolean()
    private val failed = AtomicBoolean()
    fun install(lease: SkillContributionLease) {
        check(installed.compareAndSet(false, true))
        native.set(lease)
        if (failed.get()) native.getAndSet(null)?.close()
    }
    fun finish(stage: CompletionStage<SkillResult>): CompletableFuture<SkillContributionLease> =
        stage.thenApply { outcome ->
            if (!outcome.isSuccess) throw SkillProviderException(outcome)
            requireNotNull(native.get()) { "Native contribution returned no lease" }
        }.whenComplete { _, error ->
            if (error != null) { failed.set(true); native.getAndSet(null)?.close() }
        }.toCompletableFuture()
}
