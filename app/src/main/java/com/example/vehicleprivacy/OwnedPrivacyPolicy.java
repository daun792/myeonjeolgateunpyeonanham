package com.example.vehicleprivacy;

import java.util.Objects;

/** Rendering policy for fixed, in-app examples only; never a vehicle-wide protection signal. */
public final class OwnedPrivacyPolicy {
    private final boolean protectHome;
    private final boolean protectContact;
    private final boolean hidePhoto;
    private final boolean hideNickname;
    private final boolean publicWording;

    public OwnedPrivacyPolicy(Preset preset, boolean requested) {
        Objects.requireNonNull(preset, "preset");
        protectHome = requested && preset.getBoolean("protect_screen", true);
        protectContact = requested && preset.getBoolean("protect_contact", true);
        hidePhoto = protectContact && preset.getBoolean("hide_contact_photo", true);
        hideNickname = protectContact && preset.getBoolean("hide_contact_nickname", true);
        publicWording = "public".equals(preset.getString("screen_mode", "hide"));
    }

    public boolean isProtectionApplied() { return protectHome || protectContact; }
    public String homeText() {
        if (!protectHome) return "오늘 일정: 예시 병원";
        return publicWording ? "기본 화면: 개인 일정" : "기본 화면 민감 정보 가려짐";
    }
    public String contactNameText() {
        return protectContact ? "연락처 이름 가려짐" : "연락처 이름: 김예시";
    }
    public String contactPhotoText() {
        return hidePhoto ? "연락처 사진 가려짐" : "연락처 사진: 예시 사진";
    }
    public String nicknameText() {
        return hideNickname ? "애칭 가려짐" : "애칭: 소중한 사람";
    }
    public boolean isContactPhotoMasked() { return hidePhoto; }
}
