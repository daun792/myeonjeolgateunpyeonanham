package com.example.vehicleprivacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic check of one saved preset; it does not verify system protection. */
public final class PrivacyAssessment {
    public enum SettingStatus { CONFIGURED, NEEDS_ATTENTION, UNAVAILABLE }
    public enum ApplicationStatus { UNVERIFIED }

    public static final class Item {
        private final String id;
        private final String title;
        private final String explanation;
        private final SettingStatus status;

        private Item(String id, String title, String explanation, SettingStatus status) {
            this.id = id;
            this.title = title;
            this.explanation = explanation;
            this.status = status;
        }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getExplanation() { return explanation; }
        public SettingStatus getStatus() { return status; }
        public ApplicationStatus getApplicationStatus() { return ApplicationStatus.UNVERIFIED; }
    }

    private final String presetName;
    private final List<Item> items;

    public PrivacyAssessment(Preset preset) {
        Objects.requireNonNull(preset, "preset");
        presetName = preset.getName();
        Map<String, String> values = preset.getValues();
        boolean screen = enabled(values, "protect_screen", true);
        boolean contact = enabled(values, "protect_contact", true);
        boolean notifications = enabled(values, "protect_notifications", true);
        String mode = values.containsKey("screen_mode") ? values.get("screen_mode") : "hide";
        List<Item> result = new ArrayList<>();
        result.add(check("screen_sensitive", "기본 화면 민감 정보",
                screen && ("hide".equals(mode) || "public".equals(mode)),
                "기본 화면 보호와 표시 방식이 설정돼 있습니다.",
                "기본 화면 보호와 민감 정보 표시 방식을 확인해 주세요."));
        result.add(check("notification_preview", "알림 미리보기 가림",
                notifications && enabled(values, "hide_notification_preview", true),
                "알림 보호와 미리보기 가림이 설정돼 있습니다.",
                "알림 보호와 미리보기 가림 설정을 확인해 주세요."));
        result.add(check("notification_hold", "보호 중 알림 보관",
                notifications && enabled(values, "hold_notifications", true),
                "알림 보호와 보관이 설정돼 있습니다.",
                "알림 보호와 보관 설정을 확인해 주세요."));
        result.add(check("contact_photo", "연락처 사진 가림",
                contact && enabled(values, "hide_contact_photo", true),
                "연락처 보호와 사진 가림이 설정돼 있습니다.",
                "연락처 보호와 사진 가림 설정을 확인해 주세요."));
        result.add(check("contact_nickname", "연락처 애칭 가림",
                contact && enabled(values, "hide_contact_nickname", true),
                "연락처 보호와 애칭 가림이 설정돼 있습니다.",
                "연락처 보호와 애칭 가림 설정을 확인해 주세요."));
        result.add(new Item("contact_name", "연락처 이름 가림",
                "이름 가림의 별도 설정은 아직 제공되지 않습니다.", SettingStatus.UNAVAILABLE));
        result.add(new Item("voice_reading", "음성 읽기 보호",
                "음성 읽기 보호 설정은 아직 제공되지 않습니다.", SettingStatus.UNAVAILABLE));
        items = Collections.unmodifiableList(result);
    }

    public String getPresetName() { return presetName; }
    public List<Item> getItems() { return items; }

    private static boolean enabled(Map<String, String> values, String key, boolean defaultValue) {
        if (!values.containsKey(key)) return defaultValue;
        return "true".equals(values.get(key));
    }

    private static Item check(String id, String title, boolean configured,
                              String positive, String negative) {
        return new Item(id, title, configured ? positive : negative,
                configured ? SettingStatus.CONFIGURED : SettingStatus.NEEDS_ATTENTION);
    }
}
