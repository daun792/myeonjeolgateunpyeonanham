package com.example.vehicleprivacy;

import java.util.Objects;

/** Applies a preset to a fictitious in-app notification, never to system notifications. */
public final class PassengerPreviewPolicy {
    private final boolean passengerRule;
    private final boolean requestedPreviewProtection;
    private boolean passengerPresent;

    public PassengerPreviewPolicy(Preset preset, boolean requested) {
        Objects.requireNonNull(preset, "preset");
        passengerRule = preset.getBoolean("auto_passenger", false);
        requestedPreviewProtection = requested && preset.getBoolean("protect_notifications", true)
                && preset.getBoolean("hide_notification_preview", true);
    }

    public boolean isRuleEnabled() { return passengerRule; }
    public boolean isPassengerPresent() { return passengerPresent; }
    public void setPassengerPresent(boolean present) { passengerPresent = present; }
    public boolean isMasked() { return requestedPreviewProtection || (passengerRule && passengerPresent); }
    public String preview() {
        return isMasked() ? "예시 알림 내용 가려짐" : "예시 친구: 약속 장소에서 만나요";
    }
}
