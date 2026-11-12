package com.example.vehicleprivacy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Exact local command matching; no AI, speech processing or automatic save. */
public final class PrivacyRuleProposal {
    public static final int MAX_INPUT_CHARS = 200;
    public enum Status { EMPTY, TOO_LONG, RECOGNIZED, UNRECOGNIZED }

    /** Contains only predefined labels and values, never the input text. */
    public static final class Result {
        private final Status status;
        private final String description;
        private final Map<String, String> changes;

        private Result(Status status, String description, Map<String, String> changes) {
            this.status = status;
            this.description = description;
            this.changes = Collections.unmodifiableMap(new LinkedHashMap<>(changes));
        }
        public Status getStatus() { return status; }
        public String getDescription() { return description; }
        public Map<String, String> getChanges() { return changes; }

        /** Apply only to an edit draft after the user explicitly confirms. */
        public void applyTo(Preset draft) {
            if (status != Status.RECOGNIZED) return;
            Objects.requireNonNull(draft, "draft");
            for (Map.Entry<String, String> entry : changes.entrySet()) {
                draft.setString(entry.getKey(), entry.getValue());
            }
        }
    }

    private PrivacyRuleProposal() { }

    public static Result parse(String input) {
        if (input == null || input.trim().isEmpty()) return message(Status.EMPTY, "문장을 입력해 주세요.");
        if (input.length() > MAX_INPUT_CHARS) return message(Status.TOO_LONG, "문장이 너무 깁니다.");
        String command = input.trim().replaceAll("\\s+", " ");
        switch (command) {
            case "알림 미리보기를 가려줘":
            case "알림 미리보기 가려줘":
                return proposal("알림 미리보기 가림", "protect_notifications", "true", "hide_notification_preview", "true");
            case "알림을 보관해줘":
            case "알림 보관해줘":
                return proposal("보호 중 알림 보관", "protect_notifications", "true", "hold_notifications", "true");
            case "연락처 사진을 가려줘":
            case "연락처 사진 가려줘":
                return proposal("연락처 사진 가림", "protect_contact", "true", "hide_contact_photo", "true");
            case "연락처 애칭을 가려줘":
            case "연락처 애칭 가려줘":
                return proposal("연락처 애칭 가림", "protect_contact", "true", "hide_contact_nickname", "true");
            case "기본 화면을 가려줘":
            case "기본 화면 가려줘":
                return proposal("기본 화면 민감 정보 가림", "protect_screen", "true", "screen_mode", "hide");
            default:
                return message(Status.UNRECOGNIZED, "지원하는 문장과 일치하지 않아 변경하지 않습니다.");
        }
    }

    private static Result message(Status status, String text) {
        return new Result(status, text, Collections.emptyMap());
    }

    private static Result proposal(String description, String key1, String value1, String key2, String value2) {
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put(key1, value1);
        changes.put(key2, value2);
        return new Result(Status.RECOGNIZED, description, changes);
    }
}
