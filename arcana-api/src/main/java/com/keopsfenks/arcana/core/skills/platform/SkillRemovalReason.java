package com.keopsfenks.arcana.core.skills.platform;
/** First terminal reason wins. Invalid owner/revision removal never invokes disposed skill hooks. */
public enum SkillRemovalReason {
    EXPIRED, DISPELLED, OVERWRITTEN, CHARGES_EXHAUSTED, CANCELLED, ACTOR_RETIRED,
    OWNER_UNLOADED, DEFINITION_RELOADED, ENGINE_STOPPED, BUDGET_EXCEEDED, ERROR
}
