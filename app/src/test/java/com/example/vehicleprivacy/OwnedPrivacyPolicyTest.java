package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class OwnedPrivacyPolicyTest {
    private Preset preset() { return new Preset("test", "예시", Collections.emptyMap()); }

    @Test public void protectionOffShowsOnlyFixedExamples() {
        OwnedPrivacyPolicy policy = new OwnedPrivacyPolicy(preset(), false);
        assertFalse(policy.isProtectionApplied());
        assertEquals("오늘 일정: 예시 병원", policy.homeText());
        assertEquals("연락처 이름: 김예시", policy.contactNameText());
        assertEquals("연락처 사진: 예시 사진", policy.contactPhotoText());
        assertEquals("애칭: 소중한 사람", policy.nicknameText());
    }

    @Test public void defaultProtectionMasksEverySensitiveExample() {
        OwnedPrivacyPolicy policy = new OwnedPrivacyPolicy(preset(), true);
        assertTrue(policy.isProtectionApplied());
        assertEquals("기본 화면 민감 정보 가려짐", policy.homeText());
        assertEquals("연락처 이름 가려짐", policy.contactNameText());
        assertEquals("연락처 사진 가려짐", policy.contactPhotoText());
        assertEquals("애칭 가려짐", policy.nicknameText());
        assertTrue(policy.isContactPhotoMasked());
    }

    @Test public void selectedOptionsApplyIndependently() {
        Preset preset = preset();
        preset.setString("screen_mode", "public");
        preset.setBoolean("hide_contact_photo", false);
        preset.setBoolean("hide_contact_nickname", true);
        OwnedPrivacyPolicy policy = new OwnedPrivacyPolicy(preset, true);
        assertEquals("기본 화면: 개인 일정", policy.homeText());
        assertEquals("연락처 이름 가려짐", policy.contactNameText());
        assertEquals("연락처 사진: 예시 사진", policy.contactPhotoText());
        assertFalse(policy.isContactPhotoMasked());
        assertEquals("애칭 가려짐", policy.nicknameText());
    }

    @Test public void disablingCategoriesLeavesExamplesVisibleAndUnknownModeFailsClosed() {
        Preset preset = preset();
        preset.setBoolean("protect_screen", false);
        preset.setBoolean("protect_contact", false);
        OwnedPrivacyPolicy disabled = new OwnedPrivacyPolicy(preset, true);
        assertFalse(disabled.isProtectionApplied());
        assertEquals("오늘 일정: 예시 병원", disabled.homeText());
        assertEquals("연락처 이름: 김예시", disabled.contactNameText());
        preset.setBoolean("protect_screen", true);
        preset.setString("screen_mode", "unknown");
        assertEquals("기본 화면 민감 정보 가려짐", new OwnedPrivacyPolicy(preset, true).homeText());
    }
}
