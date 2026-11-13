package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Optional;
import static org.junit.Assert.*;

public class DemoInboxScenarioTest {
    @Test public void selectedExampleOpensOnlyAfterBothHypotheticalConditions() {
        DemoInboxScenario demo = new DemoInboxScenario();
        assertEquals(3, demo.count());
        demo.select(1);
        assertFalse(demo.openSelected());
        demo.setParked(true);
        assertFalse(demo.openSelected());
        demo.setProtectionApplied(false);
        assertTrue(demo.openSelected());
        assertEquals(Optional.of("가상 메시지: 오늘 오후에 만나요."), demo.visibleBody());
    }
    @Test public void stateOrSelectionChangeImmediatelyHidesPreviouslyOpenedDetail() {
        DemoInboxScenario demo = new DemoInboxScenario();
        demo.select(0);
        demo.setParked(true);
        demo.setProtectionApplied(false);
        assertTrue(demo.openSelected());
        demo.setParked(false);
        assertEquals(Optional.empty(), demo.visibleBody());
        demo.setParked(true);
        assertEquals(Optional.empty(), demo.visibleBody());
        assertTrue(demo.openSelected());
        demo.select(2);
        assertEquals(Optional.empty(), demo.visibleBody());
        assertTrue(demo.openSelected());
        assertEquals(Optional.of("가상 배송: 물품이 도착했습니다."), demo.visibleBody());
    }
    @Test public void closeResetsSelectionAndNeverAffectsAnotherGate() {
        DisclosureGate real = new DisclosureGate(() -> 25L, 500L);
        real.beginSession();
        DemoInboxScenario demo = new DemoInboxScenario();
        demo.select(0); demo.setParked(true); demo.setProtectionApplied(false);
        assertTrue(demo.openSelected());
        assertFalse(real.isAllowed());
        demo.reset();
        assertFalse(demo.hasSelection());
        assertEquals(Optional.empty(), demo.visibleBody());
        assertNull(demo.getParked());
        assertNull(demo.getProtectionApplied());
        assertFalse(real.isAllowed());
    }
    @Test public void invalidSelectionDoesNotOpenAnItem() {
        DemoInboxScenario demo = new DemoInboxScenario();
        demo.setParked(true); demo.setProtectionApplied(false);
        assertFalse(demo.openSelected());
        try { demo.select(3); fail("invalid index accepted"); } catch (IndexOutOfBoundsException expected) { }
        assertFalse(demo.hasSelection());
        assertFalse(demo.openSelected());
        demo.select(0);
        assertTrue(demo.openSelected());
        try { demo.select(-1); fail("invalid index accepted"); } catch (IndexOutOfBoundsException expected) { }
        assertFalse(demo.hasSelection());
        assertEquals(Optional.empty(), demo.visibleBody());
    }
}
