package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class TemporaryRevealTest {
    private Preset preset(boolean name, boolean photo, boolean notification, boolean screenPhoto) {
        Preset preset = new Preset("id", "test", Collections.emptyMap());
        preset.setBoolean("preview_contact_name", name);
        preset.setBoolean("preview_contact_photo", photo);
        preset.setBoolean("preview_notification", notification);
        preset.setBoolean("preview_screen_photo", screenPhoto);
        return preset;
    }

    @Test public void onlySelectedAreasRevealDuringHoldAndHideOnRelease() {
        TemporaryReveal model = new TemporaryReveal(preset(true, false, true, false));
        assertFalse(model.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        assertTrue(model.beginHold());
        assertTrue(model.isHolding());
        assertTrue(model.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        assertFalse(model.isRevealed(TemporaryReveal.Area.CONTACT_PHOTO));
        assertTrue(model.isRevealed(TemporaryReveal.Area.NOTIFICATION));
        assertFalse(model.isRevealed(TemporaryReveal.Area.SCREEN_PHOTO));
        model.endHold();
        assertFalse(model.isHolding());
        assertFalse(model.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        assertFalse(model.isRevealed(TemporaryReveal.Area.NOTIFICATION));
    }

    @Test public void cancellationImmediatelyReprotectsAndCanStartAgain() {
        TemporaryReveal model = new TemporaryReveal(preset(false, true, false, true));
        assertTrue(model.beginHold());
        model.cancelHold();
        assertFalse(model.isHolding());
        assertFalse(model.isRevealed(TemporaryReveal.Area.CONTACT_PHOTO));
        assertFalse(model.isRevealed(TemporaryReveal.Area.SCREEN_PHOTO));
        assertTrue(model.beginHold());
        assertTrue(model.isRevealed(TemporaryReveal.Area.CONTACT_PHOTO));
    }

    @Test public void noSelectionDoesNotStartHoldAndSafetyInfoAlwaysVisible() {
        TemporaryReveal model = new TemporaryReveal(preset(false, false, false, false));
        assertTrue(model.isSafetyInfoVisible());
        assertFalse(model.beginHold());
        assertFalse(model.isHolding());
        for (TemporaryReveal.Area area : TemporaryReveal.Area.values()) {
            assertFalse(model.isSelected(area));
            assertFalse(model.isRevealed(area));
        }
        model.cancelHold();
        assertTrue(model.isSafetyInfoVisible());
    }
}
