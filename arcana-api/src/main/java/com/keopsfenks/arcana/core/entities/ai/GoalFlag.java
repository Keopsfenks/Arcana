package com.keopsfenks.arcana.core.entities.ai;

/**
 * Flags for AI goals, denoting which controls of the mob a goal occupies while running.
 */
public enum GoalFlag {
    /**
     * Move.
     */
    MOVE,

    /**
     * Look around.
     */
    LOOK,

    /**
     * Jump.
     */
    JUMP,

    /**
     * Target.
     */
    TARGET
}
