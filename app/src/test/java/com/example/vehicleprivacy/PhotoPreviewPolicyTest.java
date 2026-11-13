package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class PhotoPreviewPolicyTest {
    private static final ExamplePhotoApprovals.Photo SKY = ExamplePhotoApprovals.Photo.SKY;
    private static final ExamplePhotoApprovals.Photo PET = ExamplePhotoApprovals.Photo.PET;

    private static final class MemoryStorage implements ExamplePhotoApprovals.Storage {
        String value;
        public String read() { return value; }
        public void save(String value) { this.value = value; }
    }

    private static Preset preset(boolean screenProtected) {
        Preset preset = new Preset("id", "test", Collections.emptyMap());
        preset.setBoolean("protect_screen", screenProtected);
        preset.setBoolean("preview_screen_photo", true);
        return preset;
    }

    @Test public void protectionShowsOnlyApprovedPhotosEvenWhileHolding() {
        Preset preset = preset(true);
        TemporaryReveal reveal = new TemporaryReveal(preset);
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(new MemoryStorage());
        assertTrue(approvals.setApproved(SKY, true));
        PhotoPreviewPolicy policy = new PhotoPreviewPolicy(preset, true, approvals);
        assertTrue(policy.isVisible(SKY));
        assertFalse(policy.isVisible(PET));
        assertTrue(reveal.beginHold());
        assertTrue(reveal.isRevealed(TemporaryReveal.Area.SCREEN_PHOTO));
        assertFalse(policy.isVisible(PET));
        assertFalse(policy.label(PET).contains(PET.getLabel()));
        assertTrue(approvals.setApproved(SKY, false));
        assertFalse(policy.isVisible(SKY));
    }

    @Test public void noRequestedScreenProtectionShowsExamples() {
        ExamplePhotoApprovals approvals = new ExamplePhotoApprovals(new MemoryStorage());
        assertTrue(new PhotoPreviewPolicy(preset(true), false, approvals).isVisible(PET));
        assertTrue(new PhotoPreviewPolicy(preset(false), true, approvals).isVisible(PET));
    }

    @Test public void corruptStorageHidesAllEvenWithoutProtectionRequest() {
        MemoryStorage storage = new MemoryStorage();
        storage.value = "bad";
        PhotoPreviewPolicy policy = new PhotoPreviewPolicy(preset(false), false,
                new ExamplePhotoApprovals(storage));
        assertFalse(policy.isVisible(SKY));
        assertFalse(policy.isVisible(PET));
        assertFalse(policy.label(PET).contains(PET.getLabel()));
    }
}
