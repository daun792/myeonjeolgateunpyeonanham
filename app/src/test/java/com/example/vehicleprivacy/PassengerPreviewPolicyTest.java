package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class PassengerPreviewPolicyTest {
    private Preset preset() { return new Preset("sample", "시연", Collections.emptyMap()); }

    @Test public void boardingMasksWhenPassengerRuleEnabled() {
        Preset preset = preset();
        preset.setBoolean("auto_passenger", true);
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset, false);
        assertFalse(policy.isMasked());
        policy.setPassengerPresent(true);
        assertTrue(policy.isMasked());
        assertFalse(policy.preview().contains("예시 친구"));
    }

    @Test public void boardingDoesNotMaskWhenPassengerRuleDisabled() {
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset(), false);
        policy.setPassengerPresent(true);
        assertFalse(policy.isMasked());
        assertTrue(policy.preview().contains("예시 친구"));
    }

    @Test public void leavingRestoresPreviewOnlyWhenNoOtherProtectionApplies() {
        Preset preset = preset();
        preset.setBoolean("auto_passenger", true);
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset, false);
        policy.setPassengerPresent(true);
        policy.setPassengerPresent(false);
        assertFalse(policy.isMasked());
    }

    @Test public void leavingNeverOverridesRequestedNotificationProtection() {
        Preset preset = preset();
        preset.setBoolean("auto_passenger", true);
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset, true);
        policy.setPassengerPresent(true);
        policy.setPassengerPresent(false);
        assertTrue(policy.isMasked());
        assertFalse(policy.preview().contains("예시 친구"));
    }

    @Test public void disabledNotificationPreviewProtectionAllowsSimulatedRuleToDecide() {
        Preset preset = preset();
        preset.setBoolean("hide_notification_preview", false);
        preset.setBoolean("auto_passenger", true);
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset, true);
        assertFalse(policy.isMasked());
        policy.setPassengerPresent(true);
        assertTrue(policy.isMasked());
    }

    @Test public void storedPresetIsASnapshotDuringDemo() {
        Preset preset = preset();
        preset.setBoolean("auto_passenger", true);
        PassengerPreviewPolicy policy = new PassengerPreviewPolicy(preset, false);
        preset.setBoolean("auto_passenger", false);
        policy.setPassengerPresent(true);
        assertTrue(policy.isMasked());
        assertTrue(policy.isRuleEnabled());
    }
}
