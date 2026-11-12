package com.example.vehicleprivacy;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Only three fixed, app-owned example images; never stores real media or URIs. */
public final class ExamplePhotoApprovals {
    public interface Storage { String read(); void save(String value); }
    public enum Photo {
        SKY(1, "예시 하늘"), GARDEN(2, "예시 정원"), PET(4, "예시 동물");
        private final int mask;
        private final String label;
        Photo(int mask, String label) { this.mask = mask; this.label = label; }
        public String getLabel() { return label; }
    }
    private final Storage storage;
    private int approvals;
    private boolean storageError;

    public ExamplePhotoApprovals(Storage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
        try {
            String value = storage.read();
            if (value != null) {
                if (!value.matches("1:[0-7]")) throw new IllegalArgumentException("Invalid approvals");
                approvals = value.charAt(2) - '0';
            }
        } catch (RuntimeException error) {
            // Unknown data is preserved until an explicit reset; no photo is approved.
            storageError = true;
            approvals = 0;
        }
    }

    public synchronized boolean isVisible(Photo photo, boolean protectionOn) {
        Objects.requireNonNull(photo, "photo");
        return !storageError && (!protectionOn || (approvals & photo.mask) != 0);
    }
    public synchronized boolean isApproved(Photo photo) {
        Objects.requireNonNull(photo, "photo");
        return !storageError && (approvals & photo.mask) != 0;
    }
    public synchronized boolean setApproved(Photo photo, boolean approved) {
        Objects.requireNonNull(photo, "photo");
        if (storageError) return false;
        int next = approved ? approvals | photo.mask : approvals & ~photo.mask;
        if (next == approvals) return true;
        try {
            storage.save("1:" + next);
            approvals = next;
            return true;
        } catch (RuntimeException error) {
            storageError = true;
            return false;
        }
    }
    public synchronized Set<Photo> getApproved() {
        EnumSet<Photo> result = EnumSet.noneOf(Photo.class);
        if (!storageError) for (Photo photo : Photo.values()) if ((approvals & photo.mask) != 0) result.add(photo);
        return result;
    }
    public synchronized boolean hasStorageError() { return storageError; }
    public synchronized boolean clear() {
        try {
            storage.save(null);
            approvals = 0;
            storageError = false;
            return true;
        } catch (RuntimeException error) {
            storageError = true;
            return false;
        }
    }
}
