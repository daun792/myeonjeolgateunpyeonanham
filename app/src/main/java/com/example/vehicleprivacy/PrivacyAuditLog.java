package com.example.vehicleprivacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Local-only audit of this app's own disclosure attempts: time and decision only. */
public final class PrivacyAuditLog {
    public interface Storage { String read(); void save(String value); }
    public static final long RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000;
    public static final int MAX_ENTRIES = 100;
    private static final int VERSION = 1;
    private static final int MAX_ENCODED_LENGTH = 2048;

    public static final class Entry {
        private final long occurredAtMillis;
        private final boolean allowed;
        private Entry(long occurredAtMillis, boolean allowed) {
            this.occurredAtMillis = occurredAtMillis;
            this.allowed = allowed;
        }
        public long getOccurredAtMillis() { return occurredAtMillis; }
        public boolean wasAllowed() { return allowed; }
    }

    private final Storage storage;
    private final LongSupplier clock;
    private List<Entry> entries = new ArrayList<>();
    private boolean storageError;

    public PrivacyAuditLog(Storage storage, LongSupplier clock) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.clock = Objects.requireNonNull(clock, "clock");
        try {
            String data = storage.read();
            if (data != null) entries = decode(data);
            prune();
        } catch (RuntimeException | IOException error) {
            // Do not overwrite an unreadable archive without explicit deletion.
            storageError = true;
            entries = new ArrayList<>();
        }
    }

    /** Returns false if this event could not be saved; never records any content or identifier. */
    public synchronized boolean record(boolean allowed) {
        if (storageError) return false;
        try {
            long now = now();
            List<Entry> next = retained(entries, now);
            next.add(new Entry(now, allowed));
            while (next.size() > MAX_ENTRIES) next.remove(0);
            storage.save(encode(next));
            entries = next;
            return true;
        } catch (RuntimeException error) {
            storageError = true;
            return false;
        }
    }

    /** Entries are returned newest first; expired records are deleted on access. */
    public synchronized List<Entry> getEntries() {
        if (storageError || !prune()) return Collections.emptyList();
        List<Entry> result = new ArrayList<>(entries);
        Collections.reverse(result);
        return result;
    }

    /** Called only after explicit user confirmation; affects only the audit storage key. */
    public synchronized boolean clear() {
        try {
            storage.save(null);
            entries = new ArrayList<>();
            storageError = false;
            return true;
        } catch (RuntimeException error) {
            storageError = true;
            return false;
        }
    }

    public synchronized boolean hasStorageError() { return storageError; }

    private boolean prune() {
        if (storageError) return false;
        try {
            List<Entry> retained = retained(entries, now());
            if (retained.size() != entries.size()) {
                storage.save(encode(retained));
                entries = retained;
            }
            return true;
        } catch (RuntimeException error) {
            storageError = true;
            return false;
        }
    }

    private long now() {
        long value = clock.getAsLong();
        if (value < 0) throw new IllegalStateException("Clock unavailable");
        return value;
    }

    private static List<Entry> retained(List<Entry> input, long now) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : input) {
            // Future timestamps can result from a changed system clock and are discarded.
            if (entry.occurredAtMillis >= 0 && entry.occurredAtMillis <= now
                    && now - entry.occurredAtMillis < RETENTION_MILLIS) result.add(entry);
        }
        return result;
    }

    private static String encode(List<Entry> values) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(VERSION);
            out.writeInt(values.size());
            for (Entry entry : values) { out.writeLong(entry.occurredAtMillis); out.writeBoolean(entry.allowed); }
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException error) {
            throw new IllegalStateException("Audit encoding failed", error);
        }
    }

    private static List<Entry> decode(String data) throws IOException {
        if (data.length() > MAX_ENCODED_LENGTH) throw new IOException("Audit archive too large");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
        if (in.readInt() != VERSION) throw new IOException("Unsupported audit version");
        int count = in.readInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IOException("Invalid audit count");
        List<Entry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long time = in.readLong();
            int decision = in.readUnsignedByte();
            if (time < 0 || decision > 1) throw new IOException("Invalid audit entry");
            result.add(new Entry(time, decision == 1));
        }
        if (in.available() != 0) throw new IOException("Unexpected audit data");
        return result;
    }
}
