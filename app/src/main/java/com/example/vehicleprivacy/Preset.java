package com.example.vehicleprivacy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Settings describe the user's request only; they do not control vehicle functions. */
public final class Preset {
    private final String id;
    private String name;
    private final Map<String, String> values;

    Preset(String id, String name, Map<String, String> values) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.values = new HashMap<>(values);
    }
    Preset copy() { return new Preset(id, name, values); }
    void setName(String name) { this.name = name; }
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean getBoolean(String key, boolean fallback) {
        String value = values.get(key);
        return "true".equals(value) ? true : "false".equals(value) ? false : fallback;
    }
    public void setBoolean(String key, boolean value) { setString(key, Boolean.toString(value)); }
    public String getString(String key, String fallback) { return values.containsKey(key) ? values.get(key) : fallback; }
    public void setString(String key, String value) { values.put(Objects.requireNonNull(key), Objects.requireNonNull(value)); }
    public Map<String, String> getValues() { return new HashMap<>(values); }
    public void replaceValues(Map<String, String> replacement) {
        Map<String, String> copy = new HashMap<>(Objects.requireNonNull(replacement));
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            Objects.requireNonNull(entry.getKey());
            Objects.requireNonNull(entry.getValue());
        }
        values.clear();
        values.putAll(copy);
    }
}
