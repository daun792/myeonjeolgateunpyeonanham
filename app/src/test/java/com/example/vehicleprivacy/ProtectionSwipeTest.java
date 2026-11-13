package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProtectionSwipeTest {
    @Test public void horizontalDirectionRequestsAStableExplicitState() {
        ProtectionSwipe gesture = new ProtectionSwipe(100f, 50f, 2000L);
        gesture.down(20, 50, 1000, 1);
        gesture.move(70, 52, 1);
        assertEquals(Boolean.TRUE, gesture.up(140, 54, 1500, 1));
        assertNull(gesture.up(140, 54, 1501, 1));
        gesture.down(200, 50, 2000, 1);
        assertEquals(Boolean.FALSE, gesture.up(75, 48, 2400, 1));
    }
    @Test public void tapsVerticalDragsAndSlowSwipesDoNotChangeState() {
        ProtectionSwipe gesture = new ProtectionSwipe(100f, 50f, 2000L);
        gesture.down(20, 30, 10, 1);
        assertNull(gesture.up(80, 31, 400, 1));
        gesture.down(20, 30, 1000, 1);
        gesture.move(25, 100, 1);
        assertNull(gesture.up(160, 31, 1200, 1));
        gesture.down(20, 30, 2000, 1);
        assertNull(gesture.up(180, 30, 4001, 1));
    }
    @Test public void cancellationMultiTouchAndBadValuesDoNotChangeState() {
        ProtectionSwipe gesture = new ProtectionSwipe(100f, 50f, 2000L);
        gesture.down(10, 10, 100, 1);
        gesture.cancel();
        assertNull(gesture.up(140, 10, 200, 1));
        gesture.down(10, 10, 300, 1);
        gesture.move(40, 10, 2);
        assertNull(gesture.up(150, 10, 400, 1));
        gesture.down(Float.NaN, 10, 500, 1);
        assertNull(gesture.up(150, 10, 600, 1));
        gesture.down(10, 10, 700, 1);
        assertNull(gesture.up(150, 10, 699, 1));
    }
}
