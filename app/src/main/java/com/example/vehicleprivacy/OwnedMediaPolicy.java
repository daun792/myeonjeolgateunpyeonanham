package com.example.vehicleprivacy;

import java.util.Objects;

/** User-driven demo of rules for audio owned by this application only. */
public final class OwnedMediaPolicy {
    public enum Event { DOOR, LOCATION }
    public enum Result { DISABLED, PAUSED, MUTED }

    private final boolean doorEnabled;
    private final boolean locationEnabled;
    private final String doorAction;
    private final String locationAction;
    private boolean playing;
    private boolean muted;

    public OwnedMediaPolicy(Preset preset) {
        Objects.requireNonNull(preset);
        doorEnabled = preset.getBoolean("auto_door", false);
        locationEnabled = preset.getBoolean("auto_location", false);
        doorAction = preset.getString("door_action", "pause");
        locationAction = preset.getString("location_action", "pause");
    }

    public boolean isPlaying() { return playing; }
    public boolean isMuted() { return muted; }
    public boolean isEnabled(Event event) {
        return Objects.requireNonNull(event) == Event.DOOR ? doorEnabled : locationEnabled;
    }
    public String actionLabel(Event event) {
        String action = event == Event.DOOR ? doorAction : locationAction;
        return "mute".equals(action) ? "음소거" : "일시정지";
    }
    public void userPlay() { playing = true; }
    public void userPause() { playing = false; }
    public void userSetMuted(boolean value) { muted = value; }

    public Result trigger(Event event) {
        if (!isEnabled(event)) return Result.DISABLED;
        String action = event == Event.DOOR ? doorAction : locationAction;
        if ("mute".equals(action)) {
            muted = true;
            return Result.MUTED;
        }
        playing = false;
        return Result.PAUSED;
    }
}
