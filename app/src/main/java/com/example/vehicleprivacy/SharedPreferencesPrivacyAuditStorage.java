package com.example.vehicleprivacy;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Storage for the app-local audit key only; never stores notification contents. */
public final class SharedPreferencesPrivacyAuditStorage implements PrivacyAuditLog.Storage {
    private static final String KEY = "privacy_audit_v1";
    private final SharedPreferences preferences;

    public SharedPreferencesPrivacyAuditStorage(SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    @Override public String read() {
        synchronized (preferences) {
            Object value = preferences.getAll().get(KEY);
            if (value == null) return null;
            return value instanceof String ? (String) value : "!invalid-preference-type";
        }
    }

    @Override public void save(String value) {
        synchronized (preferences) {
            Map<String, ?> before = preferences.getAll();
            SharedPreferences.Editor editor = preferences.edit();
            if (value == null) editor.remove(KEY);
            else editor.putString(KEY, value);
            try {
                if (!editor.commit()) throw new IllegalStateException("Audit write failed");
            } catch (RuntimeException failure) {
                // SharedPreferences may change its in-memory value before a failed disk write.
                try {
                    SharedPreferences.Editor rollback = preferences.edit();
                    if (before.containsKey(KEY)) putOriginal(rollback, before.get(KEY));
                    else rollback.remove(KEY);
                    rollback.apply();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw new IllegalStateException("Audit write failed", failure);
            }
        }
    }

    private static void putOriginal(SharedPreferences.Editor editor, Object value) {
        if (value instanceof String) editor.putString(KEY, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(KEY, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(KEY, (Integer) value);
        else if (value instanceof Long) editor.putLong(KEY, (Long) value);
        else if (value instanceof Float) editor.putFloat(KEY, (Float) value);
        else if (value instanceof Set<?>) {
            Set<String> strings = new HashSet<>();
            for (Object item : (Set<?>) value) {
                if (!(item instanceof String)) throw new IllegalStateException("Invalid audit value");
                strings.add((String) item);
            }
            editor.putStringSet(KEY, strings);
        } else throw new IllegalStateException("Invalid audit value");
    }
}
