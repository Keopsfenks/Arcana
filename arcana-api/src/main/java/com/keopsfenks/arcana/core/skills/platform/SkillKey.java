package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.regex.Pattern;

/** Qualified component or definition identity; never a file path. */
public record SkillKey(String namespace, String value) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_]{1,64}");
    private static final Pattern VALUE = Pattern.compile("[a-z0-9_]{1,100}");
    public SkillKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(value, "value");
        if (!NAMESPACE.matcher(namespace).matches() || !VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid skill key: " + namespace + ":" + value);
        }
    }
    public static SkillKey parse(String text, String defaultNamespace) {
        Objects.requireNonNull(text, "text");
        int colon = text.indexOf(':');
        if (colon != text.lastIndexOf(':')) throw new IllegalArgumentException("Invalid skill key: " + text);
        return colon < 0 ? new SkillKey(defaultNamespace, text) : new SkillKey(text.substring(0, colon), text.substring(colon + 1));
    }
    @Override public String toString() { return namespace + ":" + value; }
}
