package com.example.vehicleprivacy;

import java.util.Objects;

/** Policy for fixed, app-owned pictures only; never reads device photos. */
public final class PhotoPreviewPolicy {
    private final ExamplePhotoApprovals approvals;
    private final String presetName;
    private final boolean protectionRequested;
    private final boolean screenProtectionEnabled;
    private final boolean protectScreen;

    public PhotoPreviewPolicy(Preset preset, boolean protectionRequested, ExamplePhotoApprovals approvals) {
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        Preset selected = Objects.requireNonNull(preset, "preset");
        this.presetName = selected.getName();
        this.protectionRequested = protectionRequested;
        this.screenProtectionEnabled = selected.getBoolean("protect_screen", true);
        this.protectScreen = protectionRequested && screenProtectionEnabled;
    }

    /** The temporary reveal gesture never expands the approved-photo list. */
    public boolean isVisible(ExamplePhotoApprovals.Photo photo) {
        return approvals.isVisible(Objects.requireNonNull(photo, "photo"), protectScreen);
    }

    /** Hidden labels must not expose the example's name to accessibility services. */
    public String label(ExamplePhotoApprovals.Photo photo) {
        return isVisible(photo) ? photo.getLabel() : "사진 가려짐";
    }

    public boolean isProtectScreenRequested() { return protectScreen; }
    public boolean isProtectionRequested() { return protectionRequested; }
    public boolean isScreenProtectionEnabled() { return screenProtectionEnabled; }
    public String getPresetName() { return presetName; }
}
