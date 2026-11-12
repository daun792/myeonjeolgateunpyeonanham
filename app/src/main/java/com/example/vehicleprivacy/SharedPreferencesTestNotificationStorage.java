package com.example.vehicleprivacy;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Stores sample metadata in application-private preferences without extra permissions. */
public final class SharedPreferencesTestNotificationStorage implements TestNotificationRepository.Storage {
    private static final String KEY = "test_notifications_v1";
    private static final String CORRUPT_BACKUP_KEY = "test_notifications_corrupt_backup";
    private static final int MAX_CORRUPT_BACKUPS = 100;
    private final SharedPreferences preferences;

    public SharedPreferencesTestNotificationStorage(SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override public String read() {
        synchronized (preferences) {
            Object value = preferences.getAll().get(KEY);
            if (value == null) return null;
            // A wrong preference type is corrupt data, not a reason to crash on startup.
            return value instanceof String ? (String) value : "!invalid-preference-type";
        }
    }

    @Override public void save(String data, String corruptData) {
        synchronized (preferences) {
            Map<String, ?> before = preferences.getAll();
            SharedPreferences.Editor edit = preferences.edit().putString(KEY, data);
            String backupKey = null;
            if (corruptData != null) {
                backupKey = nextBackupKey(before);
                // Preserve the actual stored type and value, not the corruption marker returned by read().
                Object original = before.containsKey(KEY) ? before.get(KEY) : corruptData;
                putValue(edit, backupKey, original);
            }
            try {
                if (!edit.commit()) throw new IllegalStateException("Preference commit failed");
            } catch (RuntimeException failure) {
                // commit() can update the in-memory map even when the disk write fails.
                // Restore both keys immediately; an apply() also attempts to restore disk state.
                try {
                    SharedPreferences.Editor rollback = preferences.edit();
                    restoreKey(rollback, before, KEY);
                    if (backupKey != null) restoreKey(rollback, before, backupKey);
                    rollback.apply();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw new IllegalStateException("테스트 알림을 저장하지 못했습니다.", failure);
            }
        }
    }

    private static String nextBackupKey(Map<String, ?> before) {
        for (int index = 1; index <= MAX_CORRUPT_BACKUPS; index++) {
            String key = index == 1 ? CORRUPT_BACKUP_KEY : CORRUPT_BACKUP_KEY + "_" + index;
            if (!before.containsKey(key)) return key;
        }
        throw new IllegalStateException("손상된 테스트 알림 백업 공간이 가득 찼습니다.");
    }

    private static void restoreKey(SharedPreferences.Editor editor, Map<String, ?> before, String key) {
        if (before.containsKey(key)) putValue(editor, key, before.get(key));
        else editor.remove(key);
    }

    private static void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set<?>) {
            Set<String> strings = new HashSet<>();
            for (Object item : (Set<?>) value) {
                if (!(item instanceof String)) throw new IllegalStateException("Unsupported preference value");
                strings.add((String) item);
            }
            editor.putStringSet(key, strings);
        } else throw new IllegalStateException("Unsupported preference value");
    }
}
