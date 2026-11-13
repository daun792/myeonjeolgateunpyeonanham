package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisclosureDemoScenarioTest {
    @Test public void sampleOpensOnlyWhenParkedAndProtectionIsOff() {
        DisclosureDemoScenario scenario = new DisclosureDemoScenario();
        assertFalse(scenario.isAllowed());
        scenario.setParked(true);
        assertFalse(scenario.isAllowed());
        scenario.setProtectionApplied(false);
        assertTrue(scenario.isAllowed());
        scenario.setProtectionApplied(true);
        assertFalse(scenario.isAllowed());
        scenario.setProtectionApplied(false);
        assertTrue(scenario.isAllowed());
        scenario.setParked(false);
        assertFalse(scenario.isAllowed());
        scenario.setParked(null);
        assertFalse(scenario.isAllowed());
    }

    @Test public void simulatedEvidenceNeverChangesAnIndependentRealGate() {
        DisclosureGate real = new DisclosureGate(() -> 100L, 500L);
        real.beginSession();
        DisclosureDemoScenario scenario = new DisclosureDemoScenario();
        scenario.setParked(true);
        scenario.setProtectionApplied(false);
        assertTrue(scenario.isAllowed());
        assertFalse(real.isAllowed());
        scenario.reset();
        assertFalse(scenario.isAllowed());
        assertFalse(real.isAllowed());
    }
}
