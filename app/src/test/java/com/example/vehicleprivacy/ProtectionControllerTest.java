package com.example.vehicleprivacy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProtectionControllerTest {
    @Test
    public void requestCanBeEnabledAndDisabledWithoutChangingLastPreset() {
        ProtectionController controller = new ProtectionController(false, "가족");

        controller.setRequested(true);
        assertTrue(controller.isRequested());
        assertEquals("가족", controller.getPresetName());

        controller.setRequested(false);
        assertFalse(controller.isRequested());
        assertEquals("가족", controller.getPresetName());
    }

    @Test
    public void restoredRequestKeepsPreviouslySelectedPreset() {
        ProtectionController controller = new ProtectionController(true, "업무");

        assertTrue(controller.isRequested());
        assertEquals("업무", controller.getPresetName());
    }

    @Test
    public void missingOrBlankPresetUsesDefault() {
        assertEquals("기본 프리셋", new ProtectionController(false, null).getPresetName());
        assertEquals("기본 프리셋", new ProtectionController(false, "  ").getPresetName());
    }
}
