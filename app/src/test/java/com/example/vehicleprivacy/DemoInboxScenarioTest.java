package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Optional;
import java.util.HashMap;
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

    @Test public void newSyntheticEventsAreAddedOnlyForRequestedEnabledPreset() {
        DemoInboxScenario demo = new DemoInboxScenario();
        Preset preset = new Preset("test", "가상", new HashMap<>());
        assertEquals(DemoInboxScenario.IncomingResult.NOT_HELD, demo.receiveSynthetic(preset, false));
        assertEquals(3, demo.count());
        preset.setBoolean("protect_notifications", false);
        assertEquals(DemoInboxScenario.IncomingResult.NOT_HELD, demo.receiveSynthetic(preset, true));
        preset.setBoolean("protect_notifications", true);
        preset.setBoolean("hold_notifications", false);
        assertEquals(DemoInboxScenario.IncomingResult.NOT_HELD, demo.receiveSynthetic(preset, true));
        assertEquals(3, demo.count());
        preset.setBoolean("hold_notifications", true);
        assertEquals(DemoInboxScenario.IncomingResult.HELD, demo.receiveSynthetic(preset, true));
        assertEquals(4, demo.count());
        demo.select(3);
        demo.setParked(true);
        demo.setProtectionApplied(false);
        assertTrue(demo.openSelected());
        assertEquals(Optional.of("가상 추가 알림: 프리셋 설정에 따라 보관된 예시입니다."), demo.visibleBody());
        demo.reset();
        assertEquals(3, demo.count());
        assertEquals(Optional.empty(), demo.visibleBody());
    }

    @Test public void capsEphemeralDemoItemsAndKeepsPriorItemsOnFailure() {
        DemoInboxScenario demo = new DemoInboxScenario();
        Preset preset = new Preset("test", "가상", new HashMap<>());
        for (int i = 0; i < DemoInboxScenario.MAX_ADDED; i++) {
            assertEquals(DemoInboxScenario.IncomingResult.HELD, demo.receiveSynthetic(preset, true));
        }
        assertEquals(DemoInboxScenario.IncomingResult.FULL, demo.receiveSynthetic(preset, true));
        assertEquals(3 + DemoInboxScenario.MAX_ADDED, demo.count());
        assertEquals(DemoInboxScenario.IncomingResult.NOT_HELD, demo.receiveSynthetic(preset, false));
        assertEquals(3 + DemoInboxScenario.MAX_ADDED, demo.count());
    }
}
