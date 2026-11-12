package com.example.vehicleprivacy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

import static org.junit.Assert.*;

public class DisclosureGateTest {
    private static final class Clock implements LongSupplier {
        long now = 1000;
        boolean fail;
        @Override public long getAsLong() {
            if (fail) throw new IllegalStateException("clock unavailable");
            return now;
        }
    }

    @Test public void bothIndependentConfirmedStatesAreRequired() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 500);
        long session = gate.beginSession();
        List<Boolean> changes = new ArrayList<>();
        gate.setListener(changes::add);
        assertFalse(gate.isAllowed());
        gate.updateProtection(session, 1, false, clock.now);
        assertFalse(gate.isAllowed());
        gate.updateParking(session, 1, true, clock.now);
        assertTrue(gate.isAllowed());
        gate.updateProtection(session, 2, true, clock.now);
        assertFalse(gate.isAllowed());
        gate.updateProtection(session, 3, false, clock.now);
        assertTrue(gate.isAllowed());
        gate.updateParking(session, 2, false, clock.now);
        assertFalse(gate.isAllowed());
        assertEquals(Arrays.asList(false, false, true, false, true, false), changes);
    }

    @Test public void expiryTriggersLockAndListenerWhenTimerIsRefreshed() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        List<Boolean> changes = new ArrayList<>();
        gate.setListener(changes::add);
        gate.updateProtection(session, 1, false, clock.now);
        clock.now = 1020;
        gate.updateParking(session, 1, true, clock.now);
        assertTrue(gate.isAllowed());
        assertEquals(80, gate.nextRecheckDelayMillis());
        clock.now = 1100;
        assertFalse(gate.refresh());
        assertEquals(-1, gate.nextRecheckDelayMillis());
        assertEquals(Arrays.asList(false, false, true, false), changes);
        gate.updateProtection(session, 2, false, clock.now);
        assertTrue(gate.isAllowed());
    }

    @Test public void futureAndOldSamplesNeverUnlockWhenClockLaterCatchesUp() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1001);
        gate.updateParking(session, 1, true, 1000);
        assertFalse(gate.isAllowed());
        clock.now = 1001;
        assertFalse(gate.isAllowed());
        gate.updateProtection(session, 2, false, 900);
        assertFalse(gate.isAllowed());
        gate.updateProtection(session, 3, false, 1001);
        assertTrue(gate.isAllowed());
    }

    @Test public void errorsDisconnectAndMonotonicClockRollbackFailClosed() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000);
        gate.updateParking(session, 1, true, 1000);
        assertTrue(gate.isAllowed());
        gate.updateParking(session, 2, null, 1000);
        assertFalse(gate.isAllowed());
        gate.updateParking(session, 3, true, 1000);
        assertTrue(gate.isAllowed());
        clock.now = 999;
        assertFalse(gate.isAllowed());
        clock.now = 1000;
        assertFalse(gate.isAllowed());
        gate.updateProtection(session, 2, false, 1000);
        gate.updateParking(session, 4, true, 1000);
        assertTrue(gate.isAllowed());
        clock.fail = true;
        assertFalse(gate.isAllowed());
        clock.fail = false;
        assertFalse(gate.isAllowed());
    }

    @Test public void oldCallbacksCannotReverseNewerRiskOrCrossReconnect() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long first = gate.beginSession();
        gate.updateProtection(first, 1, false, 1000);
        gate.updateParking(first, 2, true, 1000);
        assertTrue(gate.isAllowed());
        gate.updateParking(first, 4, false, 1000);
        gate.updateParking(first, 3, true, 1000);
        assertFalse(gate.isAllowed());
        gate.updateParking(first, 5, null, 1000);
        gate.updateParking(first, 4, true, 1000);
        assertFalse(gate.isAllowed());
        long second = gate.beginSession();
        gate.updateParking(first, 100, true, 1000);
        gate.updateProtection(first, 100, false, 1000);
        assertFalse(gate.isAllowed());
        gate.updateParking(second, 1, true, 1000);
        assertFalse(gate.isAllowed());
        gate.updateProtection(second, 1, false, 1000);
        assertTrue(gate.isAllowed());
    }

    @Test public void replacementListenerSurvivesOldViewDetachAndEvidenceRefresh() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        List<Boolean> oldCalls = new ArrayList<>();
        List<Boolean> newCalls = new ArrayList<>();
        DisclosureGate.Listener oldListener = oldCalls::add;
        gate.setListener(oldListener);
        gate.setListener(newCalls::add);
        gate.clearListener(oldListener);
        gate.updateProtection(session, 1, false, 1000);
        gate.updateParking(session, 1, true, 1000);
        gate.updateParking(session, 2, true, 1000);
        assertEquals(Arrays.asList(false), oldCalls);
        assertEquals(Arrays.asList(false, false, true, true), newCalls);
        gate.refresh();
        assertEquals(4, newCalls.size());
    }

    @Test public void higherSequenceCannotReverseRiskWithOlderObservationTime() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000);
        gate.updateParking(session, 1, true, 1000);
        assertTrue(gate.isAllowed());
        clock.now = 1001;
        gate.updateParking(session, 2, false, 1001);
        gate.updateParking(session, 3, true, 1000);
        assertFalse(gate.isAllowed());
        gate.updateParking(session, 4, true, 1001);
        assertTrue(gate.isAllowed());
    }

    @Test public void disconnectClosesAccessAndRejectsLateCallbacks() {
        Clock clock = new Clock();
        DisclosureGate gate = new DisclosureGate(clock, 100);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000);
        gate.updateParking(session, 1, true, 1000);
        assertTrue(gate.isAllowed());
        gate.invalidateAll();
        gate.updateProtection(session, 2, false, 1000);
        gate.updateParking(session, 2, true, 1000);
        assertFalse(gate.isAllowed());
    }
}
