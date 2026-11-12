package com.example.vehicleprivacy;

import android.content.SharedPreferences;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SharedPreferencesTestNotificationStorageTest {
    private static final String KEY = "test_notifications_v1";
    private static final String BACKUP = "test_notifications_corrupt_backup";

    @Test public void unexpectedValueTypeIsReportedAndBackedUpWithoutTouchingOtherSettings() {
        FakePreferences preferences = new FakePreferences();
        preferences.values.put(KEY, 42L);
        preferences.values.put("last_preset", "가족");
        TestNotificationRepository repo = new TestNotificationRepository(
                new SharedPreferencesTestNotificationStorage(preferences));
        assertTrue(repo.hasCorruptData());
        assertEquals(42L, preferences.values.get(KEY));
        assertFalse(preferences.contains(BACKUP));

        repo.addTestNotification(1234);
        assertEquals(42L, preferences.values.get(BACKUP));
        assertTrue(preferences.values.get(KEY) instanceof String);
        assertEquals("가족", preferences.values.get("last_preset"));
        assertEquals(1, new TestNotificationRepository(
                new SharedPreferencesTestNotificationStorage(preferences)).getNotifications().size());
    }

    @Test public void failedCommitRestoresMemoryEvenWhenCommitMutatedItFirst() {
        FakePreferences preferences = new FakePreferences();
        preferences.values.put(KEY, true);
        preferences.values.put("last_preset", "가족");
        TestNotificationRepository repo = new TestNotificationRepository(
                new SharedPreferencesTestNotificationStorage(preferences));
        preferences.failNextCommit = true;
        try {
            repo.addTestNotification(1234);
            fail("commit failure must be reported");
        } catch (IllegalStateException expected) { }
        assertEquals(true, preferences.values.get(KEY));
        assertFalse(preferences.contains(BACKUP));
        assertEquals("가족", preferences.values.get("last_preset"));
        assertTrue(repo.hasCorruptData());
        assertTrue(new TestNotificationRepository(
                new SharedPreferencesTestNotificationStorage(preferences)).hasCorruptData());
    }

    @Test public void failedCommitRestoresExistingValidArchive() {
        FakePreferences preferences = new FakePreferences();
        SharedPreferencesTestNotificationStorage storage = new SharedPreferencesTestNotificationStorage(preferences);
        TestNotificationRepository repo = new TestNotificationRepository(storage);
        repo.addTestNotification(1234);
        Object original = preferences.values.get(KEY);
        preferences.failNextCommit = true;
        try {
            repo.clearTestNotifications();
            fail("commit failure must be reported");
        } catch (IllegalStateException expected) { }
        assertEquals(original, preferences.values.get(KEY));
        assertEquals(1, repo.getNotifications().size());
        assertEquals(1, new TestNotificationRepository(storage).getNotifications().size());
    }

    @Test public void repeatedCorruptionsKeepEachOriginalBackup() {
        FakePreferences preferences = new FakePreferences();
        preferences.values.put(KEY, "first invalid archive");
        SharedPreferencesTestNotificationStorage storage = new SharedPreferencesTestNotificationStorage(preferences);
        new TestNotificationRepository(storage).addTestNotification(1234);
        assertEquals("first invalid archive", preferences.values.get(BACKUP));

        preferences.values.put(KEY, 42L);
        new TestNotificationRepository(storage).addTestNotification(5678);
        assertEquals("first invalid archive", preferences.values.get(BACKUP));
        assertEquals(42L, preferences.values.get(BACKUP + "_2"));
    }

    private static final class FakePreferences implements SharedPreferences {
        final Map<String, Object> values = new HashMap<>();
        boolean failNextCommit;
        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String fallback) { return (String) values.getOrDefault(key, fallback); }
        @SuppressWarnings("unchecked")
        @Override public Set<String> getStringSet(String key, Set<String> fallback) { return (Set<String>) values.getOrDefault(key, fallback); }
        @Override public int getInt(String key, int fallback) { return (Integer) values.getOrDefault(key, fallback); }
        @Override public long getLong(String key, long fallback) { return (Long) values.getOrDefault(key, fallback); }
        @Override public float getFloat(String key, float fallback) { return (Float) values.getOrDefault(key, fallback); }
        @Override public boolean getBoolean(String key, boolean fallback) { return (Boolean) values.getOrDefault(key, fallback); }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }

        private final class FakeEditor implements Editor {
            final Map<String, Object> changes = new HashMap<>();
            boolean clear;
            @Override public Editor putString(String key, String value) { changes.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) { changes.put(key, new HashSet<>(value)); return this; }
            @Override public Editor putInt(String key, int value) { changes.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { changes.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { changes.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { changes.put(key, value); return this; }
            @Override public Editor remove(String key) { changes.put(key, null); return this; }
            @Override public Editor clear() { clear = true; return this; }
            private void update() {
                if (clear) values.clear();
                changes.forEach((key, value) -> { if (value == null) values.remove(key); else values.put(key, value); });
            }
            @Override public boolean commit() {
                update();
                boolean result = !failNextCommit;
                failNextCommit = false;
                return result;
            }
            @Override public void apply() { update(); }
        }
    }
}
