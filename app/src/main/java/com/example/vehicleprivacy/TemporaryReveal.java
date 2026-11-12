package com.example.vehicleprivacy;

import java.util.EnumSet;
import java.util.Objects;

/** State for an in-app demonstration; it does not control any vehicle system. */
public final class TemporaryReveal {
    public enum Area { CONTACT_NAME, CONTACT_PHOTO, NOTIFICATION, SCREEN_PHOTO }

    private final EnumSet<Area> selected = EnumSet.noneOf(Area.class);
    private boolean holding;

    public TemporaryReveal(Preset preset) {
        Objects.requireNonNull(preset, "preset");
        if (preset.getBoolean("preview_contact_name", false)) selected.add(Area.CONTACT_NAME);
        if (preset.getBoolean("preview_contact_photo", false)) selected.add(Area.CONTACT_PHOTO);
        if (preset.getBoolean("preview_notification", false)) selected.add(Area.NOTIFICATION);
        if (preset.getBoolean("preview_screen_photo", false)) selected.add(Area.SCREEN_PHOTO);
    }

    /** Starts a simulated reveal only if the preset specifies a reveal scope. */
    public boolean beginHold() {
        holding = !selected.isEmpty();
        return holding;
    }

    public void endHold() { holding = false; }
    public void cancelHold() { holding = false; }
    public boolean isHolding() { return holding; }
    public boolean isSelected(Area area) { return selected.contains(Objects.requireNonNull(area)); }
    public boolean isRevealed(Area area) { return holding && isSelected(area); }
    public boolean isSafetyInfoVisible() { return true; }
}
