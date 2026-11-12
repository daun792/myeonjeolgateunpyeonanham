package com.example.vehicleprivacy;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class PresetRepositoryTest {
    static final class MemoryStorage implements PresetRepository.Storage {
        final Map<String, String> values = new HashMap<>();
        @Override public String get(String key) { return values.get(key); }
        @Override public void put(String data, String selectedName) {
            values.put("preset_data_v1", data);
            values.put("last_preset", selectedName);
        }
    }

    @Test public void migratesLegacySelectionWithoutChangingRequest() {
        MemoryStorage storage = new MemoryStorage();
        storage.values.put("last_preset", "가족");
        storage.values.put("request_enabled", "true");
        PresetRepository repo = new PresetRepository(storage);
        assertEquals("가족", repo.getSelected().getName());
        assertEquals("가족", new PresetRepository(storage).getSelected().getName());
        assertEquals("true", storage.values.get("request_enabled"));
    }

    @Test public void settingsAreIndependentAndOnlySavedWhenExplicitlyCommitted() {
        MemoryStorage storage = new MemoryStorage();
        PresetRepository repo = new PresetRepository(storage);
        Preset first = repo.getSelected();
        Preset second = repo.add("동승자");
        first.setBoolean("notifications.hide", true);
        first.setString("audio.media", "pause");
        repo.save(first);
        second.setBoolean("notifications.hide", false);
        second.setString("audio.media", "mute");
        PresetRepository reloaded = new PresetRepository(storage);
        assertTrue(reloaded.getSelected().getBoolean("notifications.hide", false));
        assertEquals("pause", reloaded.getSelected().getString("audio.media", ""));
        Preset untouched = reloaded.getPresets().get(1);
        assertEquals("fallback", untouched.getString("audio.media", "fallback"));
        repo.save(second);
        repo.select(second.getId());
        reloaded = new PresetRepository(storage);
        assertEquals("동승자", reloaded.getSelected().getName());
        assertEquals("mute", reloaded.getSelected().getString("audio.media", ""));
        assertFalse(reloaded.getSelected().getBoolean("notifications.hide", true));
    }

    @Test public void reorderRenameAndDeletePersistAndKeepSelectionValid() {
        MemoryStorage storage = new MemoryStorage();
        PresetRepository repo = new PresetRepository(storage);
        Preset second = repo.add("업무");
        repo.rename(second.getId(), "가족");
        repo.move(second.getId(), 0);
        repo.select(second.getId());
        PresetRepository reloaded = new PresetRepository(storage);
        assertEquals(second.getId(), reloaded.getPresets().get(0).getId());
        assertEquals("가족", storage.values.get("last_preset"));
        reloaded.delete(second.getId());
        assertEquals(1, reloaded.getPresets().size());
        assertEquals(reloaded.getPresets().get(0).getId(), reloaded.getSelectedId());
        try { reloaded.delete(reloaded.getSelectedId()); fail("last preset must remain"); }
        catch (IllegalStateException expected) { }
        assertEquals(1, new PresetRepository(storage).getPresets().size());
    }

    @Test public void invalidEncodedDataRecoversWithoutOverwritingUntilUserChangesIt() {
        MemoryStorage storage = new MemoryStorage();
        storage.values.put("preset_data_v1", "not valid base64!");
        storage.values.put("last_preset", "가족");
        storage.values.put("request_enabled", "true");

        PresetRepository repo = new PresetRepository(storage);
        assertEquals("기본 프리셋", repo.getSelected().getName());
        assertEquals("not valid base64!", storage.values.get("preset_data_v1"));
        assertEquals("가족", storage.values.get("last_preset"));
        assertEquals("true", storage.values.get("request_enabled"));

        repo.rename(repo.getSelectedId(), "업무");
        assertEquals("업무", new PresetRepository(storage).getSelected().getName());
    }

    @Test public void missingSelectedIdRecoversInsteadOfExposingPartialData() throws IOException {
        MemoryStorage storage = new MemoryStorage();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(1);
        out.writeUTF("missing-id");
        out.writeInt(1);
        out.writeUTF("existing-id");
        out.writeUTF("복구 불가능한 데이터");
        out.writeInt(0);
        String corrupt = Base64.getEncoder().encodeToString(bytes.toByteArray());
        storage.values.put("preset_data_v1", corrupt);

        PresetRepository repo = new PresetRepository(storage);
        assertEquals(1, repo.getPresets().size());
        assertEquals("기본 프리셋", repo.getSelected().getName());
        assertEquals(corrupt, storage.values.get("preset_data_v1"));
    }
}
