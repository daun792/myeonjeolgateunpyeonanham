package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeldNotificationTest {
    private final HeldNotification notification =
            new HeldNotification("n1", 1234L, "메신저 개인 계정", "민감한 대화 내용");

    @Test public void onlyConfirmedReleaseAndParkingAllowsDetails() {
        for (Boolean applied : new Boolean[] {null, false, true}) {
            for (Boolean parked : new Boolean[] {null, false, true}) {
                DisclosureGate gate = new DisclosureGate(() -> 1000L, 5000L);
                long session = gate.beginSession();
                gate.updateProtection(session, 1, applied, 1000L);
                gate.updateParking(session, 1, parked, 1000L);
                boolean allowed = Boolean.FALSE.equals(applied) && Boolean.TRUE.equals(parked);
                assertEquals("applied=" + applied + ", parked=" + parked,
                        allowed, notification.canReveal(gate));
                assertEquals(allowed, notification.getBodyIfAllowed(gate).isPresent());
                assertEquals(allowed, notification.getSourceIfAllowed(gate).isPresent());
            }
        }
        assertFalse(notification.getBodyIfAllowed(null).isPresent());
    }

    @Test public void listAndAccessibilityNeverIncludePrivateFields() {
        assertEquals("n1", notification.getId());
        assertEquals(1234L, notification.getReceivedAtMillis());
        assertEquals("보관된 알림", notification.getListText());
        assertEquals("보관된 알림, 내용 숨김", notification.getAccessibilityText());
        assertFalse(notification.getListText().contains("민감한 대화 내용"));
        assertFalse(notification.getAccessibilityText().contains("민감한 대화 내용"));
        assertFalse(notification.getAccessibilityText().contains("메신저 개인 계정"));
    }
}
