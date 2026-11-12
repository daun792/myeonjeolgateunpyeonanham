package com.example.vehicleprivacy;

public final class ProtectionController {
    private boolean requested;
    private final String presetName;

    public ProtectionController(boolean requested, String lastPreset) {
        this.requested = requested;
        this.presetName = lastPreset == null || lastPreset.trim().isEmpty()
                ? "기본 프리셋" : lastPreset;
    }

    public boolean isRequested() { return requested; }

    public void setRequested(boolean requested) { this.requested = requested; }

    public String getPresetName() { return presetName; }
}
