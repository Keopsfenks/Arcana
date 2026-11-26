package com.keopsfenks.arcana.core.skills.platform;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Closed immutable value model. Live Bukkit objects and unbounded payloads are rejected. */
public final class SkillValue {
    public enum Kind { NUMBER, TEXT, BOOLEAN, LIST, MAP, KEY, LOCATION, VECTOR }
    public static final int MAX_BYTES = 256 * 1024;
    public static final int MAX_DEPTH = 64;
    private final Kind kind;
    private final Object value;
    private final int bytes;
    private final int depth;

    private SkillValue(Kind kind, Object value, int bytes, int depth) {
        this.kind = kind; this.value = value; this.bytes = bytes; this.depth = depth;
    }
    public static SkillValue of(Object value) {
        return freeze(value, 0, Collections.newSetFromMap(new IdentityHashMap<>()), new Budget());
    }
    public Kind kind() { return kind; }
    public double number() { return require(Kind.NUMBER, Double.class); }
    public String text() { return require(Kind.TEXT, String.class); }
    public boolean bool() { return require(Kind.BOOLEAN, Boolean.class); }
    @SuppressWarnings("unchecked") public List<SkillValue> list() { return require(Kind.LIST, List.class); }
    @SuppressWarnings("unchecked") public Map<String, SkillValue> map() { return require(Kind.MAP, Map.class); }
    public SkillKey key() { return require(Kind.KEY, SkillKey.class); }
    public SkillLocation location() { return require(Kind.LOCATION, SkillLocation.class); }
    public SkillVector vector() { return require(Kind.VECTOR, SkillVector.class); }
    private <T> T require(Kind expected, Class<T> type) {
        if (kind != expected) throw new IllegalStateException("Expected " + expected + ", got " + kind);
        return type.cast(value);
    }
    /** Converts immutable data to the original API's supported scalar/list/map representation. */
    public Object plain() {
        return switch (kind) {
            case NUMBER, TEXT, BOOLEAN -> value;
            case KEY -> key().toString();
            case LOCATION -> Map.of("world", location().world().toString(), "x", location().x(), "y", location().y(), "z", location().z(), "yaw", location().yaw(), "pitch", location().pitch());
            case VECTOR -> Map.of("x", vector().x(), "y", vector().y(), "z", vector().z());
            case LIST -> list().stream().map(SkillValue::plain).toList();
            case MAP -> {
                var result = new LinkedHashMap<String, Object>();
                map().forEach((key, entry) -> result.put(key, entry.plain()));
                yield Collections.unmodifiableMap(result);
            }
        };
    }
    private static SkillValue freeze(Object input, int level, Set<Object> active, Budget budget) {
        if (level > MAX_DEPTH) throw new IllegalArgumentException("Skill value nesting exceeds " + MAX_DEPTH);
        int start = budget.bytes;
        if (input instanceof SkillValue value) {
            if (level + value.depth > MAX_DEPTH) throw new IllegalArgumentException("Skill value nesting exceeds " + MAX_DEPTH);
            budget.add(value.bytes);
            return value;
        }
        if (input instanceof Number number) {
            double snapshot = number.doubleValue();
            if (!Double.isFinite(snapshot)) throw new IllegalArgumentException("Skill number must be finite");
            budget.add(16);
            return new SkillValue(Kind.NUMBER, snapshot, 16, 0);
        }
        if (input instanceof Enum<?> value) input = value.name();
        if (input instanceof String text) {
            if (text.length() > MAX_BYTES) throw new IllegalArgumentException("Skill value exceeds 256 KiB");
            budget.add(text.getBytes(StandardCharsets.UTF_8).length + 4);
            return new SkillValue(Kind.TEXT, text, budget.bytes - start, 0);
        }
        if (input instanceof Boolean flag) { budget.add(1); return new SkillValue(Kind.BOOLEAN, flag, 1, 0); }
        if (input instanceof SkillKey key) { budget.add(key.toString().length() + 4); return new SkillValue(Kind.KEY, key, budget.bytes - start, 0); }
        if (input instanceof SkillLocation location) { budget.add(64); return new SkillValue(Kind.LOCATION, location, 64, 0); }
        if (input instanceof SkillVector vector) { budget.add(24); return new SkillValue(Kind.VECTOR, vector, 24, 0); }
        if (input instanceof List<?> list) {
            if (!active.add(list)) throw new IllegalArgumentException("Cyclic skill value");
            budget.add(4);
            var result = new ArrayList<SkillValue>();
            int depth = 1;
            try {
                for (Object item : list) {
                    budget.add(1);
                    var child = freeze(item, level + 1, active, budget);
                    result.add(child); depth = Math.max(depth, child.depth + 1);
                }
            } finally { active.remove(list); }
            return new SkillValue(Kind.LIST, List.copyOf(result), budget.bytes - start, depth);
        }
        if (input instanceof Map<?, ?> map) {
            if (!active.add(map)) throw new IllegalArgumentException("Cyclic skill value");
            budget.add(4);
            var result = new LinkedHashMap<String, SkillValue>();
            int depth = 1;
            try {
                for (var entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException("Skill map keys must be strings");
                    if (key.length() > MAX_BYTES) throw new IllegalArgumentException("Skill value exceeds 256 KiB");
                    budget.add(key.getBytes(StandardCharsets.UTF_8).length + 4);
                    var child = freeze(entry.getValue(), level + 1, active, budget);
                    result.put(key, child); depth = Math.max(depth, child.depth + 1);
                }
            } finally { active.remove(map); }
            return new SkillValue(Kind.MAP, Collections.unmodifiableMap(result), budget.bytes - start, depth);
        }
        throw new IllegalArgumentException("Unsupported skill value: " + (input == null ? "null" : input.getClass().getSimpleName()));
    }
    private static final class Budget {
        private int bytes;
        void add(int amount) {
            if (amount < 0 || amount > MAX_BYTES - bytes) throw new IllegalArgumentException("Skill value exceeds 256 KiB");
            bytes += amount;
        }
    }
    @Override public boolean equals(Object other) { return other instanceof SkillValue entry && kind == entry.kind && value.equals(entry.value); }
    @Override public int hashCode() { return Objects.hash(kind, value); }
    @Override public String toString() { return kind + "[" + value + "]"; }
}
