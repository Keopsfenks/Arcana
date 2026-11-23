package com.keopsfenks.arcana.core.skills;

import java.util.*;

/** Deeply immutable configuration with field-qualified validation errors. */
public final class SkillArguments {
    private final Map<String, Object> values;
    private final String path;

    public SkillArguments(Map<String, ?> values, String path) {
        this.path = Objects.requireNonNull(path, "path");
        this.values = freezeMap(values, path);
    }

    public Map<String, Object> getValues() { return values; }
    public String getPath() { return path; }
    public boolean has(String key) { return values.containsKey(key); }
    public Object raw(String key) { return values.get(key); }
    public Object raw(String key, Object fallback) { return values.getOrDefault(key, fallback); }
    public IllegalArgumentException error(String key, String message) {
        return new IllegalArgumentException(fieldPath(key) + ": " + message);
    }
    private String fieldPath(String key) {
        return path + (key.isEmpty() ? "" : key.startsWith("[") ? key : "." + key);
    }

    public String string(String key) {
        if (values.get(key) instanceof String result) return result;
        throw error(key, "expected a string");
    }
    public String string(String key, String fallback) { return has(key) ? string(key) : fallback; }
    public boolean bool(String key, boolean fallback) {
        if (!has(key)) return fallback;
        if (values.get(key) instanceof Boolean result) return result;
        throw error(key, "expected a boolean");
    }
    public double number(String key, double fallback) {
        if (!has(key)) return fallback;
        if (values.get(key) instanceof Number number && Double.isFinite(number.doubleValue())) return number.doubleValue();
        throw error(key, "expected a finite number");
    }
    public List<?> list(String key) {
        if (values.get(key) instanceof List<?> result) return result;
        throw error(key, "expected a list");
    }
    public List<?> list(String key, List<?> fallback) { return has(key) ? list(key) : fallback; }
    public SkillArguments section(String key) { return child(key, values.get(key)); }
    public SkillArguments child(String key, Object value) {
        if (!(value instanceof Map<?, ?> map)) throw error(key, "expected a mapping");
        var copy = new LinkedHashMap<String, Object>();
        map.forEach((k, v) -> {
            if (!(k instanceof String name)) throw error(key, "mapping keys must be strings");
            copy.put(name, v);
        });
        return new SkillArguments(copy, fieldPath(key));
    }

    private static Map<String, Object> freezeMap(Map<?, ?> source, String path) {
        return freezeMap(source, path, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static Map<String, Object> freezeMap(Map<?, ?> source, String path, int depth, Set<Object> active) {
        if (depth > 64 || !active.add(source)) throw new IllegalArgumentException(path + ": cyclic or excessively nested arguments");
        var copy = new LinkedHashMap<String, Object>();
        try {
            source.forEach((key, value) -> {
                if (!(key instanceof String name)) throw new IllegalArgumentException(path + ": mapping keys must be strings");
                copy.put(name, freeze(value, path + "." + name, depth + 1, active));
            });
            return Collections.unmodifiableMap(copy);
        } finally { active.remove(source); }
    }
    private static Object freeze(Object value, String path, int depth, Set<Object> active) {
        if (depth > 64) throw new IllegalArgumentException(path + ": argument nesting exceeds 64 levels");
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Enum<?>) return value;
        if (value instanceof Number number) {
            if (!Double.isFinite(number.doubleValue())) throw new IllegalArgumentException(path + ": number must be finite");
            return number;
        }
        if (value instanceof Map<?, ?> map) return freezeMap(map, path, depth, active);
        if (value instanceof List<?> list) {
            if (!active.add(list)) throw new IllegalArgumentException(path + ": cyclic arguments");
            try {
                var copy = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) copy.add(freeze(list.get(i), path + "[" + i + "]", depth + 1, active));
                return Collections.unmodifiableList(copy);
            } finally { active.remove(list); }
        }
        throw new IllegalArgumentException(path + ": unsupported value type " + value.getClass().getSimpleName());
    }
}
