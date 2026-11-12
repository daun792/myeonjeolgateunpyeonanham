package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Base64;
import java.util.List;
import java.util.function.LongSupplier;
import static org.junit.Assert.*;

public class PrivacyAuditLogTest {
    private static final class Clock implements LongSupplier {
        long now = 1_000_000_000L;
        public long getAsLong() { return now; }
    }
    private static final class MemoryStorage implements PrivacyAuditLog.Storage {
        String data;
        boolean fail;
        public String read() { return data; }
        public void save(String value) {
            if (fail) throw new IllegalStateException("unavailable");
            data = value;
        }
    }

    @Test public void recordsOnlyTimeAndDecisionAndRestoresAcrossRestart() {
        Clock clock = new Clock();
        MemoryStorage storage = new MemoryStorage();
        PrivacyAuditLog log = new PrivacyAuditLog(storage, clock);
        assertTrue(log.record(false));
        clock.now++;
        assertTrue(log.record(true));
        List<PrivacyAuditLog.Entry> entries = new PrivacyAuditLog(storage, clock).getEntries();
        assertEquals(2, entries.size());
        assertEquals(clock.now, entries.get(0).getOccurredAtMillis());
        assertTrue(entries.get(0).wasAllowed());
        assertFalse(entries.get(1).wasAllowed());
        byte[] raw = Base64.getDecoder().decode(storage.data);
        assertEquals("version + count + two timestamps and two booleans only", 4 + 4 + 2 * 9, raw.length);
    }

    @Test public void retentionPrunesExpiredRecordsOnAccess() {
        Clock clock = new Clock();
        MemoryStorage storage = new MemoryStorage();
        PrivacyAuditLog log = new PrivacyAuditLog(storage, clock);
        assertTrue(log.record(false));
        clock.now += PrivacyAuditLog.RETENTION_MILLIS;
        assertTrue(log.record(true));
        assertEquals(1, log.getEntries().size());
        assertTrue(log.getEntries().get(0).wasAllowed());
        assertEquals(1, new PrivacyAuditLog(storage, clock).getEntries().size());
    }

    @Test public void keepsMostRecentHundredEvents() {
        Clock clock = new Clock();
        MemoryStorage storage = new MemoryStorage();
        PrivacyAuditLog log = new PrivacyAuditLog(storage, clock);
        for (int i = 0; i < 105; i++) { assertTrue(log.record(i % 2 == 0)); clock.now++; }
        List<PrivacyAuditLog.Entry> entries = log.getEntries();
        assertEquals(100, entries.size());
        assertEquals(1_000_000_104L, entries.get(0).getOccurredAtMillis());
        assertEquals(1_000_000_005L, entries.get(99).getOccurredAtMillis());
    }

    @Test public void corruptDataIsPreservedUntilExplicitClear() {
        Clock clock = new Clock();
        MemoryStorage storage = new MemoryStorage();
        storage.data = "broken";
        PrivacyAuditLog log = new PrivacyAuditLog(storage, clock);
        assertTrue(log.hasStorageError());
        assertFalse(log.record(true));
        assertEquals("broken", storage.data);
        assertTrue(log.clear());
        assertFalse(log.hasStorageError());
        assertNull(storage.data);
        assertTrue(log.record(false));
    }

    @Test public void failedWritesDoNotClaimRecordOrClearSucceeded() {
        Clock clock = new Clock();
        MemoryStorage storage = new MemoryStorage();
        PrivacyAuditLog log = new PrivacyAuditLog(storage, clock);
        assertTrue(log.record(false));
        String original = storage.data;
        storage.fail = true;
        assertFalse(log.record(true));
        assertTrue(log.hasStorageError());
        assertEquals(original, storage.data);
        assertFalse(log.clear());
        assertEquals(original, storage.data);
    }
}
