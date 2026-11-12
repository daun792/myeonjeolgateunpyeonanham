package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class OwnedMediaPolicyTest {
    private Preset preset() { return new Preset("sample", "시험", Collections.emptyMap()); }

    @Test public void demoNeverStartsUntilUserChoosesPlayback() {
        OwnedMediaPolicy policy = new OwnedMediaPolicy(preset());
        assertFalse(policy.isPlaying());
        assertFalse(policy.isMuted());
        policy.userPlay();
        assertTrue(policy.isPlaying());
        policy.userPause();
        assertFalse(policy.isPlaying());
    }

    @Test public void disabledRuleDoesNotChangeUserPlayback() {
        OwnedMediaPolicy policy = new OwnedMediaPolicy(preset());
        policy.userPlay();
        assertEquals(OwnedMediaPolicy.Result.DISABLED, policy.trigger(OwnedMediaPolicy.Event.DOOR));
        assertTrue(policy.isPlaying());
        assertFalse(policy.isMuted());
    }

    @Test public void doorPauseRuleOnlyPausesOwnedPlayback() {
        Preset preset = preset();
        preset.setBoolean("auto_door", true);
        preset.setString("door_action", "pause");
        OwnedMediaPolicy policy = new OwnedMediaPolicy(preset);
        policy.userPlay();
        assertEquals(OwnedMediaPolicy.Result.PAUSED, policy.trigger(OwnedMediaPolicy.Event.DOOR));
        assertFalse(policy.isPlaying());
        assertFalse(policy.isMuted());
    }

    @Test public void locationMuteRulePreservesPlaybackAndCanBeReversedByUser() {
        Preset preset = preset();
        preset.setBoolean("auto_location", true);
        preset.setString("location_action", "mute");
        OwnedMediaPolicy policy = new OwnedMediaPolicy(preset);
        policy.userPlay();
        assertEquals(OwnedMediaPolicy.Result.MUTED, policy.trigger(OwnedMediaPolicy.Event.LOCATION));
        assertTrue(policy.isPlaying());
        assertTrue(policy.isMuted());
        policy.userSetMuted(false);
        assertTrue(policy.isPlaying());
        assertFalse(policy.isMuted());
    }

    @Test public void corruptActionFallsBackToPauseAndPresetIsASnapshot() {
        Preset preset = preset();
        preset.setBoolean("auto_door", true);
        preset.setString("door_action", "unexpected");
        OwnedMediaPolicy policy = new OwnedMediaPolicy(preset);
        preset.setBoolean("auto_door", false);
        policy.userPlay();
        assertEquals(OwnedMediaPolicy.Result.PAUSED, policy.trigger(OwnedMediaPolicy.Event.DOOR));
        assertFalse(policy.isPlaying());
    }
}
