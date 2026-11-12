package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class UxRestrictionVetoTest {
    @Test public void unrestrictedOrUnknownNeverEstablishesParking() {
        DisclosureGate gate = new DisclosureGate(() -> 1000L, 5000L);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000L);
        UxRestrictionVeto veto = new UxRestrictionVeto(gate, () -> 1000L);
        long generation = veto.start(session);
        veto.onRestrictions(generation, false);
        assertFalse(gate.isAllowed());
        veto.onRestrictions(generation, null);
        assertFalse(gate.isAllowed());
        veto.onRestrictions(generation, true);
        assertFalse(gate.isAllowed());
    }

    @Test public void oldCallbacksCannotRevokeOrChangeNewSession() {
        DisclosureGate gate = new DisclosureGate(() -> 1000L, 5000L);
        long oldSession = gate.beginSession();
        UxRestrictionVeto veto = new UxRestrictionVeto(gate, () -> 1000L);
        long oldGeneration = veto.start(oldSession);
        veto.stop();
        long newSession = gate.beginSession();
        gate.updateProtection(newSession, 1, false, 1000L);
        gate.updateParking(newSession, 1, true, 1000L);
        assertTrue(gate.isAllowed());
        veto.onRestrictions(oldGeneration, true);
        assertTrue(gate.isAllowed());
    }

    @Test public void stoppedOrFailedProviderKeepsAccessClosed() {
        DisclosureGate gate = new DisclosureGate(() -> 1000L, 5000L);
        long session = gate.beginSession();
        gate.updateProtection(session, 1, false, 1000L);
        UxRestrictionVeto veto = new UxRestrictionVeto(gate, () -> 1000L);
        long generation = veto.start(session);
        veto.onRestrictions(generation, true);
        veto.stop();
        veto.onRestrictions(generation, false);
        assertFalse(gate.isAllowed());
    }
}
