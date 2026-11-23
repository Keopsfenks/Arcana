package com.keopsfenks.arcana.core.skills;

import java.util.Objects;

/** Final admission/execution outcome. Effects applied before failure are not rolled back. */
public record SkillResult(Status status, String message) {
    public enum Status {
        SUCCESS, UNKNOWN_SKILL, DISABLED, NO_PERMISSION, FILTER_FAILED, CONDITION_FAILED,
        CHANCE_FAILED, COOLDOWN, NO_TARGET, CANCELLED, ERROR, DEPENDENCY_UNAVAILABLE,
        UNSUPPORTED_ACTOR, UNSUPPORTED_CONTEXT, INSUFFICIENT_RESOURCE, BUDGET_EXCEEDED, PROVIDER_UNAVAILABLE
    }

    public SkillResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public static SkillResult success() { return new SkillResult(Status.SUCCESS, ""); }
    public static SkillResult of(Status status, String message) { return new SkillResult(status, message); }
    public boolean isSuccess() { return status == Status.SUCCESS; }
}
