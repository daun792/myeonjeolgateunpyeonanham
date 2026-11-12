package com.example.vehicleprivacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Stores only IDs and timestamps for examples generated inside this app. */
public final class TestNotificationRepository {
    public interface Storage {
        String read();
        /** If corruptData is non-null, retain it before replacing the stored data. */
        void save(String data, String corruptData);
    }

    public static final int MAX_NOTIFICATIONS = 100;
    private static final String SOURCE = "앱 테스트 알림";
    private static final String BODY = "개인정보가 포함되지 않은 앱 내부 테스트 메시지입니다.";
    private static final int VERSION = 1;
    private static final int MAX_ENCODED_LENGTH = 16000;

    private static final class Entry {
        final String id;
        final long timestamp;
        Entry(String id, long timestamp) { this.id = id; this.timestamp = timestamp; }
        HeldNotification asNotification() { return new HeldNotification(id, timestamp, SOURCE, BODY); }
    }

    private final Storage storage;
    private List<Entry> entries = new ArrayList<>();
    private String damagedData;

    public TestNotificationRepository(Storage storage) {
        this.storage = Objects.requireNonNull(storage);
        String stored = storage.read();
        if (stored == null) return;
        try {
            entries = decode(stored);
        } catch (IOException | IllegalArgumentException invalid) {
            // Preserve the original value until the user explicitly changes the test items.
            damagedData = stored;
        }
    }

    public List<HeldNotification> getNotifications() {
        List<HeldNotification> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) result.add(entry.asNotification());
        return result;
    }

    public boolean hasCorruptData() { return damagedData != null; }

    public HeldNotification addTestNotification(long receivedAtMillis) {
        if (receivedAtMillis < 0) throw new IllegalArgumentException("알림 시각을 확인할 수 없습니다.");
        if (entries.size() >= MAX_NOTIFICATIONS) {
            throw new IllegalStateException("테스트 알림은 최대 100개까지 보관할 수 있습니다.");
        }
        Entry entry = new Entry(UUID.randomUUID().toString(), receivedAtMillis);
        List<Entry> next = new ArrayList<>(entries);
        next.add(entry);
        persist(next);
        return entry.asNotification();
    }

    /** Removes only test examples, after an explicit user request. */
    public void clearTestNotifications() { persist(new ArrayList<>()); }

    private void persist(List<Entry> next) {
        storage.save(encode(next), damagedData);
        entries = next;
        damagedData = null;
    }

    private static String encode(List<Entry> items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(VERSION);
            out.writeInt(items.size());
            for (Entry item : items) {
                out.writeUTF(item.id);
                out.writeLong(item.timestamp);
            }
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException("테스트 알림을 저장할 수 없습니다.", impossible);
        }
    }

    private static List<Entry> decode(String data) throws IOException {
        if (data.length() > MAX_ENCODED_LENGTH) throw new IOException("Archive too large");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
        if (in.readInt() != VERSION) throw new IOException("Unsupported version");
        int count = in.readInt();
        if (count < 0 || count > MAX_NOTIFICATIONS) throw new IOException("Invalid item count");
        List<Entry> result = new ArrayList<>(count);
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String id = in.readUTF();
            if (!UUID.fromString(id).toString().equals(id) || !ids.add(id)) throw new IOException("Invalid ID");
            long timestamp = in.readLong();
            if (timestamp < 0) throw new IOException("Invalid timestamp");
            result.add(new Entry(id, timestamp));
        }
        if (in.available() != 0) throw new IOException("Unexpected trailing data");
        return result;
    }
}
