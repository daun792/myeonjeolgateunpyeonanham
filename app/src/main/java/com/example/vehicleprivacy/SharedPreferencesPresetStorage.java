package com.example.vehicleprivacy;

import android.content.SharedPreferences;

/** Writes both preset data and the legacy selected name in one preference edit. */
public final class SharedPreferencesPresetStorage implements PresetRepository.Storage {
    private final SharedPreferences preferences;
    public SharedPreferencesPresetStorage(SharedPreferences preferences) { this.preferences = preferences; }
    @Override public String get(String key) { return preferences.getString(key, null); }
    @Override public void put(String data, String selectedName) {
        preferences.edit().putString("preset_data_v1", data).putString("last_preset", selectedName).apply();
    }
}
