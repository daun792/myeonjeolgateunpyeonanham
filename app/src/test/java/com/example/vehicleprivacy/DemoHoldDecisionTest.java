package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.HashMap;
import static org.junit.Assert.*;

public class DemoHoldDecisionTest {
    private static Preset preset(boolean protect, boolean hold) {
        Preset p = new Preset("demo", "예시", new HashMap<>());
        p.setBoolean("protect_notifications", protect);
        p.setBoolean("hold_notifications", hold);
        return p;
    }

    @Test public void holdsOnlyWhenProtectionRequestedAndBothPresetOptionsEnabled() {
        for (boolean request : new boolean[]{false, true}) {
            for (boolean protect : new boolean[]{false, true}) {
                for (boolean hold : new boolean[]{false, true}) {
                    boolean actual = DemoHoldDecision.shouldHold(preset(protect, hold), request);
                    assertEquals("request=" + request + ", protect=" + protect + ", hold=" + hold,
                            request && protect && hold, actual);
                }
            }
        }
    }

    @Test public void usesExistingPresetDefaultValuesForAbsentOptions() {
        Preset defaults = new Preset("demo", "예시", new HashMap<>());
        assertTrue(DemoHoldDecision.shouldHold(defaults, true));
        assertFalse(DemoHoldDecision.shouldHold(defaults, false));
    }

    @Test public void readsTheSelectedPresetOnEachDecisionWithoutModifyingIt() {
        Preset first = preset(true, true);
        Preset second = preset(true, false);
        assertTrue(DemoHoldDecision.shouldHold(first, true));
        assertFalse(DemoHoldDecision.shouldHold(second, true));
        first.setBoolean("hold_notifications", false);
        assertFalse(DemoHoldDecision.shouldHold(first, true));
        assertTrue(first.getBoolean("protect_notifications", false));
    }

    @Test(expected = NullPointerException.class)
    public void missingPresetCannotBeTreatedAsPermissionToHold() {
        DemoHoldDecision.shouldHold(null, true);
    }
}
