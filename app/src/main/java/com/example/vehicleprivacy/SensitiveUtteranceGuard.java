package com.example.vehicleprivacy;

import java.util.regex.Pattern;

/** A narrow, local pattern check. It never approves or emits text for speech. */
public final class SensitiveUtteranceGuard {
    public static final int MAX_INPUT_CHARS = 2000;
    private static final Pattern MOBILE = Pattern.compile(
            "(?<![0-9])(?:\\+82[\\s.\\-]?)?0?1[016789](?:[\\s.\\-]?[0-9]){7,8}(?![0-9])");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![a-z0-9._%+\\-])[a-z0-9._%+\\-]{1,64}@[a-z0-9.\\-]{1,253}\\.[a-z]{2,63}(?![a-z0-9.\\-])");

    public enum Status { EMPTY, TOO_LONG, PATTERN_FOUND, UNVERIFIED }

    /** Contains no input or substrings from it. */
    public static final class Result {
        private final Status status;
        private final boolean phone;
        private final boolean email;

        private Result(Status status, boolean phone, boolean email) {
            this.status = status;
            this.phone = phone;
            this.email = email;
        }
        public Status getStatus() { return status; }
        public boolean hasPhone() { return phone; }
        public boolean hasEmail() { return email; }
        public boolean canSpeak() { return false; }
        public String getPublicSummary() {
            switch (status) {
                case EMPTY: return "검사할 문장을 입력해 주세요.";
                case TOO_LONG: return "문장이 너무 길어 검사하지 않았습니다. 발화는 허용되지 않습니다.";
                case PATTERN_FOUND: return "민감할 수 있는 패턴이 확인됐습니다. 원문 발화는 허용되지 않습니다.";
                default: return "정해진 패턴은 발견되지 않았지만 문장의 안전성은 확인되지 않았습니다. 원문 발화는 허용되지 않습니다.";
            }
        }
    }

    public Result inspect(String text) {
        if (text == null || text.trim().isEmpty()) return new Result(Status.EMPTY, false, false);
        if (text.length() > MAX_INPUT_CHARS) return new Result(Status.TOO_LONG, false, false);
        boolean phone = MOBILE.matcher(text).find();
        boolean email = EMAIL.matcher(text).find();
        return new Result(phone || email ? Status.PATTERN_FOUND : Status.UNVERIFIED, phone, email);
    }
}
