package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeldNotificationTest {
    private final HeldNotification notification =
            new HeldNotification("n1", 1234L, "메신저 개인 계정", "민감한 대화 내용");

    @Test public void onlyConfirmedReleaseAndParkingAllowsDetails() {
        assertTrue(notification.canReveal(false, true));
        assertEquals("민감한 대화 내용", notification.getBodyIfAllowed(false, true).orElse(null));
        assertEquals("메신저 개인 계정", notification.getSourceIfAllowed(false, true).orElse(null));

        for (Boolean applied : new Boolean[] {null, false, true}) {
            for (Boolean parked : new Boolean[] {null, false, true}) {
                if (Boolean.FALSE.equals(applied) && Boolean.TRUE.equals(parked)) continue;
                assertFalse("applied=" + applied + ", parked=" + parked,
                        notification.canReveal(applied, parked));
                assertFalse(notification.getBodyIfAllowed(applied, parked).isPresent());
                assertFalse(notification.getSourceIfAllowed(applied, parked).isPresent());
            }
        }
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
