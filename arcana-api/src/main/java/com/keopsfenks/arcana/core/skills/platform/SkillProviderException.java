package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.Objects;

/** A provider capability, availability or reservation failure with an explicit skill outcome. */
public final class SkillProviderException extends IllegalStateException {
    private final SkillResult result;
    public SkillProviderException(SkillResult result) {
        super(Objects.requireNonNull(result).message());
        if (result.isSuccess()) throw new IllegalArgumentException("Provider exception requires a failure result");
        this.result = result;
    }
    public SkillResult result() { return result; }
}
