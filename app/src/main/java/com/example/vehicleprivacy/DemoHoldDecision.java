package com.example.vehicleprivacy;

import java.util.Objects;

/** Policy for a synthetic event; it never reads or writes a real notification. */
public final class DemoHoldDecision {
    private DemoHoldDecision() { }

    public static boolean shouldHold(Preset preset, boolean protectionRequested) {
        Objects.requireNonNull(preset, "preset");
        return protectionRequested
                && preset.getBoolean("protect_notifications", true)
                && preset.getBoolean("hold_notifications", true);
    }
}
