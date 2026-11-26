package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.Objects;

/**
 * A prepared resource hold. Commit and release must be idempotent and non-blocking.
 * Successful preparation guarantees commit can finish without further validation or I/O.
 * Local reservations are finalized within the actor ownership callback that prepared them.
 */
public interface SkillResourceReservation {
    default SkillResult result() { return SkillResult.success(); }
    void commit();
    void release();
    static SkillResourceReservation denied(SkillResult reason) {
        Objects.requireNonNull(reason);
        if (reason.isSuccess()) throw new IllegalArgumentException("A denied reservation requires a failure result");
        return new SkillResourceReservation() {
            @Override public SkillResult result() { return reason; }
            @Override public void commit() { }
            @Override public void release() { }
        };
    }
}
