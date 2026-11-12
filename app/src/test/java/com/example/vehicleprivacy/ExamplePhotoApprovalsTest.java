package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class ExamplePhotoApprovalsTest {
    static final class MemoryStorage implements ExamplePhotoApprovals.Storage {
        String data;
        boolean fail;
        public String read() { return data; }
        public void save(String value) {
            if (fail) throw new IllegalStateException("unavailable");
            data = value;
        }
    }

    @Test public void onlyApprovedExamplePhotosAppearWhenProtectionIsOn() {
        MemoryStorage storage = new MemoryStorage();
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(storage);
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertTrue(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, false));
        assertTrue(approvals.setApproved(ExamplePhotoApprovals.Photo.SKY, true));
        assertTrue(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.PET, true));
        assertEquals("1:1", storage.data);
    }

    @Test public void approvalAndRemovalPersistAndReturnDefensiveCopy() {
        MemoryStorage storage = new MemoryStorage();
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(storage);
        assertTrue(approvals.setApproved(ExamplePhotoApprovals.Photo.PET, true));
        ExamplePhotoApprovals restored = new ExamplePhotoApprovals(storage);
        assertTrue(restored.isApproved(ExamplePhotoApprovals.Photo.PET));
        Set<ExamplePhotoApprovals.Photo> copy = restored.getApproved();
        copy.clear();
        assertTrue(restored.isApproved(ExamplePhotoApprovals.Photo.PET));
        assertTrue(restored.setApproved(ExamplePhotoApprovals.Photo.PET, false));
        assertFalse(new ExamplePhotoApprovals(storage).isApproved(ExamplePhotoApprovals.Photo.PET));
    }

    @Test public void corruptArchiveIsPreservedAndNeverApprovesDuringProtection() {
        MemoryStorage storage = new MemoryStorage();
        storage.data = "invalid";
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(storage);
        assertTrue(approvals.hasStorageError());
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertFalse(approvals.setApproved(ExamplePhotoApprovals.Photo.SKY, true));
        assertEquals("invalid", storage.data);
        assertTrue(approvals.clear());
        assertFalse(approvals.hasStorageError());
        assertNull(storage.data);
    }

    @Test public void failedSaveDoesNotApproveAndPreservesPreviousArchive() {
        MemoryStorage storage = new MemoryStorage();
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(storage);
        assertTrue(approvals.setApproved(ExamplePhotoApprovals.Photo.SKY, true));
        String original = storage.data;
        storage.fail = true;
        assertFalse(approvals.setApproved(ExamplePhotoApprovals.Photo.PET, true));
        assertTrue(approvals.hasStorageError());
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.SKY, true));
        assertFalse(approvals.isVisible(ExamplePhotoApprovals.Photo.PET, true));
        assertEquals(original, storage.data);
    }
}
