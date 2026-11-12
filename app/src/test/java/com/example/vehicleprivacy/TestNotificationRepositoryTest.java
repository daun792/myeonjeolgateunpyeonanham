package com.example.vehicleprivacy;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.*;

public class TestNotificationRepositoryTest {
    private DisclosureGate verifiedGate() {
        DisclosureGate gate = new DisclosureGate(() -> 1000L, 5000L);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000L);
        gate.updateParking(session, 1, true, 1000L);
        return gate;
    }
    static final class MemoryStorage implements TestNotificationRepository.Storage {
        String data;
        String backup;
        boolean failWrite;

        @Override public String read() { return data; }
        @Override public void save(String newData, String corruptData) {
            if (failWrite) throw new IllegalStateException("저장 실패");
            if (corruptData != null && backup == null) backup = corruptData;
            data = newData;
        }
    }

    @Test public void explicitSamplesSurviveReloadWithDistinctIdsAndOriginalTimestamps() {
        MemoryStorage storage = new MemoryStorage();
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        assertTrue(repository.getNotifications().isEmpty());
        HeldNotification first = repository.addTestNotification(1000);
        HeldNotification second = repository.addTestNotification(2000);

        List<HeldNotification> restored = new TestNotificationRepository(storage).getNotifications();
        assertEquals(2, restored.size());
        assertEquals(first.getId(), restored.get(0).getId());
        assertEquals(second.getId(), restored.get(1).getId());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(1000, restored.get(0).getReceivedAtMillis());
        assertEquals(2000, restored.get(1).getReceivedAtMillis());
        assertEquals("앱 테스트 알림", restored.get(0).getSourceIfAllowed(verifiedGate()).orElse(null));
        assertFalse(restored.get(0).getBodyIfAllowed(null).isPresent());
        restored.clear();
        assertEquals(2, repository.getNotifications().size());
    }

    @Test public void persistedDataContainsOnlySampleIdentityAndTime() {
        MemoryStorage storage = new MemoryStorage();
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        HeldNotification notification = repository.addTestNotification(1000);
        String raw = new String(Base64.getDecoder().decode(storage.data), StandardCharsets.UTF_8);
        assertTrue(raw.contains(notification.getId()));
        assertFalse(raw.contains("앱 테스트 알림"));
        assertFalse(raw.contains(notification.getBodyIfAllowed(verifiedGate()).get()));
    }

    @Test public void damagedDataIsNotOverwrittenUntilExplicitCreationAndIsBackedUp() {
        MemoryStorage storage = new MemoryStorage();
        storage.data = "not a valid archive";
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        assertTrue(repository.hasCorruptData());
        assertTrue(repository.getNotifications().isEmpty());
        assertEquals("not a valid archive", storage.data);
        assertNull(storage.backup);

        repository.addTestNotification(1000);
        assertEquals("not a valid archive", storage.backup);
        assertFalse(repository.hasCorruptData());
        assertEquals(1, new TestNotificationRepository(storage).getNotifications().size());
    }

    @Test public void failedSaveDoesNotChangeVisibleCollectionOrEraseDamagedData() {
        MemoryStorage storage = new MemoryStorage();
        storage.data = "broken";
        storage.failWrite = true;
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        try {
            repository.addTestNotification(1234);
            fail("save failure must be reported");
        } catch (IllegalStateException expected) { }
        assertTrue(repository.getNotifications().isEmpty());
        assertTrue(repository.hasCorruptData());
        assertEquals("broken", storage.data);
    }

    @Test public void limitDoesNotSilentlyDeleteExistingExamples() {
        MemoryStorage storage = new MemoryStorage();
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        for (int i = 0; i < TestNotificationRepository.MAX_NOTIFICATIONS; i++) {
            repository.addTestNotification(i);
        }
        String originalData = storage.data;
        try {
            repository.addTestNotification(999);
            fail("limit must be enforced");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("100"));
        }
        assertEquals(TestNotificationRepository.MAX_NOTIFICATIONS, repository.getNotifications().size());
        assertEquals(originalData, storage.data);
        assertEquals(TestNotificationRepository.MAX_NOTIFICATIONS,
                new TestNotificationRepository(storage).getNotifications().size());
    }

    @Test public void explicitlyClearingExamplesPersistsEmptyList() {
        MemoryStorage storage = new MemoryStorage();
        TestNotificationRepository repository = new TestNotificationRepository(storage);
        repository.addTestNotification(1000);
        repository.addTestNotification(2000);
        repository.clearTestNotifications();
        assertTrue(repository.getNotifications().isEmpty());
        assertTrue(new TestNotificationRepository(storage).getNotifications().isEmpty());
    }
}
