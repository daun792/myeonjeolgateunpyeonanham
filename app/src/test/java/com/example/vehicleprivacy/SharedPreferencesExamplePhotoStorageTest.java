package com.example.vehicleprivacy;

import android.content.SharedPreferences;
import org.junit.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class SharedPreferencesExamplePhotoStorageTest {
    private static final String KEY = "example_photo_approvals_v1";

    @Test public void incorrectTypeIsPreservedAndOtherKeysAreNotChanged() {
        FakePreferences prefs = new FakePreferences();
        prefs.values.put(KEY, 42L);
        prefs.values.put("last_preset", "가족");
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(new SharedPreferencesExamplePhotoStorage(prefs));
        assertTrue(approvals.hasStorageError());
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertEquals(42L, prefs.values.get(KEY));
        assertTrue(approvals.clear());
        assertFalse(prefs.contains(KEY));
        assertEquals("가족", prefs.values.get("last_preset"));
    }

    @Test public void failedCommitRestoresEarlierValueAndFailsClosed() {
        FakePreferences prefs = new FakePreferences();
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(new SharedPreferencesExamplePhotoStorage(prefs));
        assertTrue(approvals.setApproved(ExamplePhotoApprovals.Photo.SKY, true));
        Object original = prefs.values.get(KEY);
        prefs.failNextCommit = true;
        assertFalse(approvals.setApproved(ExamplePhotoApprovals.Photo.PET, true));
        assertTrue(approvals.hasStorageError());
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertEquals(original, prefs.values.get(KEY));
    }

    private static final class FakePreferences implements SharedPreferences {
        final Map<String, Object> values = new HashMap<>();
        boolean failNextCommit;
        public Map<String, ?> getAll() { return new HashMap<>(values); }
        public String getString(String key, String fallback) { return (String) values.getOrDefault(key, fallback); }
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String key, Set<String> fallback) { return (Set<String>) values.getOrDefault(key, fallback); }
        public int getInt(String key, int fallback) { return (Integer) values.getOrDefault(key, fallback); }
        public long getLong(String key, long fallback) { return (Long) values.getOrDefault(key, fallback); }
        public float getFloat(String key, float fallback) { return (Float) values.getOrDefault(key, fallback); }
        public boolean getBoolean(String key, boolean fallback) { return (Boolean) values.getOrDefault(key, fallback); }
        public boolean contains(String key) { return values.containsKey(key); }
        public Editor edit() { return new FakeEditor(); }
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        private final class FakeEditor implements Editor {
            final Map<String, Object> changes = new HashMap<>();
            public Editor putString(String key, String value) { changes.put(key, value); return this; }
            public Editor putStringSet(String key, Set<String> value) { changes.put(key, new HashSet<>(value)); return this; }
            public Editor putInt(String key, int value) { changes.put(key, value); return this; }
            public Editor putLong(String key, long value) { changes.put(key, value); return this; }
            public Editor putFloat(String key, float value) { changes.put(key, value); return this; }
            public Editor putBoolean(String key, boolean value) { changes.put(key, value); return this; }
            public Editor remove(String key) { changes.put(key, null); return this; }
            public Editor clear() { values.clear(); return this; }
            private void update() { changes.forEach((key, value) -> { if (value == null) values.remove(key); else values.put(key, value); }); }
            public boolean commit() { update(); boolean result = !failNextCommit; failNextCommit = false; return result; }
            public void apply() { update(); }
        }
    }
}
