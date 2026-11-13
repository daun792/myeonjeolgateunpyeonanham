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

    /** A hold adds detail only for a visible, explicitly approved fixed example. */
    public String label(ExamplePhotoApprovals.Photo photo, TemporaryReveal reveal) {
        Objects.requireNonNull(reveal, "reveal");
        String baseline = label(photo);
        if (!isVisible(photo) || !approvals.isApproved(photo)
                || !reveal.isRevealed(TemporaryReveal.Area.SCREEN_PHOTO)) return baseline;
        String detail;
        switch (photo) {
            case SKY: detail = "맑은 하늘 일러스트"; break;
            case GARDEN: detail = "꽃이 핀 정원 일러스트"; break;
            default: detail = "작은 동물 일러스트";
        }
        return baseline + " · 잠시 보기: " + detail;
    }

    public boolean isProtectScreenRequested() { return protectScreen; }
    public boolean isProtectionRequested() { return protectionRequested; }
    public boolean isScreenProtectionEnabled() { return screenProtectionEnabled; }
    public String getPresetName() { return presetName; }
}
