package com.example.vehicleprivacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Manages ordered preset data; persistence is supplied independently of Android. */
public final class PresetRepository {
    public interface Storage {
        String get(String key);
        void put(String data, String selectedName);
    }

    private static final String DATA_KEY = "preset_data_v1";
    private static final String LEGACY_KEY = "last_preset";
    private static final String DEFAULT_NAME = "기본 프리셋";
    private final Storage storage;
    private final List<Preset> presets = new ArrayList<>();
    private String selectedId;

    public PresetRepository(Storage storage) {
        this.storage = storage;
        String data = storage.get(DATA_KEY);
        if (data == null) {
            String legacyName = storage.get(LEGACY_KEY);
            Preset initial = new Preset(UUID.randomUUID().toString(),
                    legacyName == null || legacyName.trim().isEmpty() ? DEFAULT_NAME : legacyName,
                    Collections.emptyMap());
            presets.add(initial);
            selectedId = initial.getId();
            persist();
        } else {
            try {
                decode(data);
            } catch (IllegalStateException invalidData) {
                // Keep the original bytes for recovery. Only a later user edit persists
                // the in-memory fallback and replaces the damaged stored data.
                presets.clear();
                Preset fallback = new Preset(UUID.randomUUID().toString(), DEFAULT_NAME,
                        Collections.emptyMap());
                presets.add(fallback);
                selectedId = fallback.getId();
            }
        }
    }

    public List<Preset> getPresets() {
        List<Preset> result = new ArrayList<>();
        for (Preset preset : presets) result.add(preset.copy());
        return result;
    }
    public String getSelectedId() { return selectedId; }
    public Preset getSelected() { return find(selectedId).copy(); }
    public Preset add(String name) {
        Preset preset = new Preset(UUID.randomUUID().toString(), validName(name), Collections.emptyMap());
        presets.add(preset);
        persist();
        return preset.copy();
    }
    public void select(String id) { find(id); selectedId = id; persist(); }
    public void save(Preset preset) {
        Preset existing = find(preset.getId());
        existing.replaceValues(preset.getValues());
        persist();
    }
    public void rename(String id, String name) { find(id).setName(validName(name)); persist(); }
    public void move(String id, int newIndex) {
        if (newIndex < 0 || newIndex >= presets.size()) throw new IndexOutOfBoundsException("preset index");
        Preset preset = find(id);
        presets.remove(preset);
        presets.add(newIndex, preset);
        persist();
    }
    public void delete(String id) {
        Preset preset = find(id);
        if (presets.size() == 1) throw new IllegalStateException("At least one preset is required");
        presets.remove(preset);
        if (id.equals(selectedId)) selectedId = presets.get(0).getId();
        persist();
    }
    private Preset find(String id) {
        for (Preset preset : presets) if (preset.getId().equals(id)) return preset;
        throw new IllegalArgumentException("Unknown preset");
    }
    private static String validName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Preset name is empty");
        return name.trim();
    }
    private void persist() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(1);
            out.writeUTF(selectedId);
            out.writeInt(presets.size());
            for (Preset preset : presets) {
                out.writeUTF(preset.getId());
                out.writeUTF(preset.getName());
                Map<String, String> values = preset.getValues();
                out.writeInt(values.size());
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
            }
            out.flush();
            storage.put(Base64.getEncoder().encodeToString(bytes.toByteArray()), find(selectedId).getName());
        } catch (IOException e) { throw new IllegalStateException("Could not encode preset data", e); }
    }
    private void decode(String data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
            if (in.readInt() != 1) throw new IOException("Unsupported preset version");
            selectedId = in.readUTF();
            int count = in.readInt();
            if (count < 1 || count > 10000) throw new IOException("Invalid preset count");
            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                String name = in.readUTF();
                int size = in.readInt();
                if (size < 0 || size > 10000) throw new IOException("Invalid setting count");
                Map<String, String> values = new HashMap<>();
                for (int j = 0; j < size; j++) values.put(in.readUTF(), in.readUTF());
                presets.add(new Preset(id, name, values));
            }
            find(selectedId);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid preset data", e);
        }
    }
}
