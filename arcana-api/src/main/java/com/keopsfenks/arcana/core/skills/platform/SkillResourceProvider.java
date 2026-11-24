package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Owned resource reservation and optional balance snapshots; no economy or progression model is assumed. */
public interface SkillResourceProvider extends SkillPlatformComponent {
    CompletionStage<SkillResourceReservation> reserve(SkillActor actor, double amount);
    /** Pure validation; called during preflight for constants and before runtime reservations. */
    default void validateAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Resource amount must be finite and nonnegative");
    }
    default boolean supportsLocal() { return false; }
    /** Available only after the caller proves ownership of every required actor. Must finish synchronously. */
    default SkillResourceReservation reserveLocal(SkillActor actor, double amount) {
        return SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Resource has no synchronous reservation capability"));
    }
    default CompletionStage<Double> balance(SkillActor actor) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Resource does not expose balance snapshots"));
    }
    default double balanceLocal(SkillActor actor) { throw new UnsupportedOperationException("Resource has no synchronous balance capability"); }
}
