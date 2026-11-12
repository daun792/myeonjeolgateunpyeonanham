package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class PrivacyAssessmentTest {
    private Preset preset() { return new Preset("id", "가족", Collections.emptyMap()); }

    private PrivacyAssessment.Item item(PrivacyAssessment report, String id) {
        return report.getItems().stream().filter(i -> id.equals(i.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("Missing check: " + id));
    }

    @Test public void configuredDefaultsOnlyDescribeSettingsNotActualProtection() {
        PrivacyAssessment report = new PrivacyAssessment(preset());
        assertEquals(PrivacyAssessment.SettingStatus.CONFIGURED, item(report, "notification_preview").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.CONFIGURED, item(report, "notification_hold").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.CONFIGURED, item(report, "contact_nickname").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.UNAVAILABLE, item(report, "voice_reading").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.UNAVAILABLE, item(report, "contact_name").getStatus());
        for (PrivacyAssessment.Item result : report.getItems()) {
            assertEquals(PrivacyAssessment.ApplicationStatus.UNVERIFIED, result.getApplicationStatus());
        }
    }

    @Test public void parentOrChildDisabledRequiresAttention() {
        Preset preset = preset();
        preset.setBoolean("protect_notifications", false);
        preset.setBoolean("hide_contact_nickname", false);
        preset.setBoolean("protect_screen", false);
        PrivacyAssessment report = new PrivacyAssessment(preset);
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "notification_preview").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "notification_hold").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "contact_nickname").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "screen_sensitive").getStatus());
    }

    @Test public void invalidValuesNeverProduceAConfiguredResult() {
        Preset preset = preset();
        preset.setString("hide_notification_preview", "invalid");
        preset.setString("screen_mode", "unexpected");
        PrivacyAssessment report = new PrivacyAssessment(preset);
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "notification_preview").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION, item(report, "screen_sensitive").getStatus());
    }

    @Test public void resultsAreAnImmutableSnapshotAndRecomputeAfterEdits() {
        Preset preset = preset();
        PrivacyAssessment before = new PrivacyAssessment(preset);
        List<PrivacyAssessment.Item> items = before.getItems();
        try { items.clear(); fail("results must be immutable"); }
        catch (UnsupportedOperationException expected) { }
        preset.setBoolean("hold_notifications", false);
        assertEquals(PrivacyAssessment.SettingStatus.CONFIGURED, item(before, "notification_hold").getStatus());
        assertEquals(PrivacyAssessment.SettingStatus.NEEDS_ATTENTION,
                item(new PrivacyAssessment(preset), "notification_hold").getStatus());
    }
}
