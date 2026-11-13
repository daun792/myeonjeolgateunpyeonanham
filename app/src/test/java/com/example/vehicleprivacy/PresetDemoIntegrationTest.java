package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Checks saved preset selection across independent app-owned demonstration policies. */
public class PresetDemoIntegrationTest {
    private static final class MemoryStorage implements PresetRepository.Storage {
        private final Map<String, String> values = new HashMap<>();
        @Override public String get(String key) { return values.get(key); }
        @Override public void put(String encoded, String selectedName) {
            values.put("preset_data_v1", encoded);
            values.put("last_preset", selectedName);
        }
    }

    @Test public void newPresetStartsWithAutomationAndTemporaryRevealDisabled() {
        PresetRepository repository = new PresetRepository(new MemoryStorage());
        Preset selected = repository.getSelected();
        TemporaryReveal temporaryReveal = new TemporaryReveal(selected);
        assertFalse(temporaryReveal.beginHold());
        assertTrue(temporaryReveal.isSafetyInfoVisible());

        OwnedMediaPolicy media = new OwnedMediaPolicy(selected);
        media.userPlay();
        assertEquals(OwnedMediaPolicy.Result.DISABLED, media.trigger(OwnedMediaPolicy.Event.DOOR));
        assertEquals(OwnedMediaPolicy.Result.DISABLED, media.trigger(OwnedMediaPolicy.Event.LOCATION));
        assertTrue(media.isPlaying());

        PassengerPreviewPolicy passenger = new PassengerPreviewPolicy(selected, false);
        passenger.setPassengerPresent(true);
        assertFalse(passenger.isRuleEnabled());
        assertFalse(passenger.isMasked());
    }

    @Test public void savedSelectedPresetSurvivesReloadAndSwitchesAllDemonstrations() {
        MemoryStorage storage = new MemoryStorage();
        PresetRepository repository = new PresetRepository(storage);
        Preset soloDraft = repository.getSelected();
        soloDraft.setBoolean("preview_notification", true);
        soloDraft.setBoolean("auto_door", true);
        soloDraft.setString("door_action", "pause");
        repository.save(soloDraft);
        Preset passengerDraft = repository.add("동승자 시연");
        passengerDraft.setBoolean("preview_contact_name", true);
        passengerDraft.setBoolean("auto_passenger", true);
        passengerDraft.setBoolean("auto_location", true);
        passengerDraft.setString("location_action", "mute");

        // Editing a copy without saving must never alter what an opened demo reads.
        repository.select(passengerDraft.getId());
        Preset beforeSave = new PresetRepository(storage).getSelected();
        assertFalse(beforeSave.getBoolean("auto_passenger", false));
        assertFalse(new TemporaryReveal(beforeSave).beginHold());
        repository.save(passengerDraft);

        PresetRepository reopened = new PresetRepository(storage);
        Preset selectedPassenger = reopened.getSelected();
        assertEquals(passengerDraft.getId(), selectedPassenger.getId());
        TemporaryReveal passengerReveal = new TemporaryReveal(selectedPassenger);
        assertTrue(passengerReveal.beginHold());
        assertTrue(passengerReveal.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        assertFalse(passengerReveal.isRevealed(TemporaryReveal.Area.NOTIFICATION));
        passengerReveal.endHold();
        assertFalse(passengerReveal.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        PassengerPreviewPolicy passenger = new PassengerPreviewPolicy(selectedPassenger, false);
        passenger.setPassengerPresent(true);
        assertTrue(passenger.isMasked());
        OwnedMediaPolicy passengerMedia = new OwnedMediaPolicy(selectedPassenger);
        passengerMedia.userPlay();
        assertEquals(OwnedMediaPolicy.Result.DISABLED, passengerMedia.trigger(OwnedMediaPolicy.Event.DOOR));
        assertEquals(OwnedMediaPolicy.Result.MUTED, passengerMedia.trigger(OwnedMediaPolicy.Event.LOCATION));
        assertTrue(passengerMedia.isPlaying());
        assertTrue(passengerMedia.isMuted());

        reopened.select(soloDraft.getId());
        Preset selectedSolo = new PresetRepository(storage).getSelected();
        assertEquals(soloDraft.getId(), selectedSolo.getId());
        TemporaryReveal soloReveal = new TemporaryReveal(selectedSolo);
        assertTrue(soloReveal.beginHold());
        assertTrue(soloReveal.isRevealed(TemporaryReveal.Area.NOTIFICATION));
        assertFalse(soloReveal.isRevealed(TemporaryReveal.Area.CONTACT_NAME));
        PassengerPreviewPolicy soloPassenger = new PassengerPreviewPolicy(selectedSolo, false);
        soloPassenger.setPassengerPresent(true);
        assertFalse(soloPassenger.isMasked());
        OwnedMediaPolicy soloMedia = new OwnedMediaPolicy(selectedSolo);
        soloMedia.userPlay();
        assertEquals(OwnedMediaPolicy.Result.DISABLED, soloMedia.trigger(OwnedMediaPolicy.Event.LOCATION));
        assertEquals(OwnedMediaPolicy.Result.PAUSED, soloMedia.trigger(OwnedMediaPolicy.Event.DOOR));
        assertFalse(soloMedia.isPlaying());
    }
}
