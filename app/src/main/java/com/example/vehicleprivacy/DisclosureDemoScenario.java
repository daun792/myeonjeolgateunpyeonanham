package com.example.vehicleprivacy;

/** A self-contained demonstration. Values are hypothetical, never vehicle evidence. */
public final class DisclosureDemoScenario {
    private static final long SAMPLE_TIME = 10L;
    private final DisclosureGate gate = new DisclosureGate(() -> SAMPLE_TIME, 1000L);
    private long session;
    private long parkingSequence;
    private long protectionSequence;
    private Boolean parked;
    private Boolean protectionApplied;

    public DisclosureDemoScenario() { reset(); }

    public void setParked(Boolean value) {
        parked = value;
        gate.updateParking(session, ++parkingSequence, value, SAMPLE_TIME);
    }
    public void setProtectionApplied(Boolean value) {
        protectionApplied = value;
        gate.updateProtection(session, ++protectionSequence, value, SAMPLE_TIME);
    }
    public Boolean getParked() { return parked; }
    public Boolean getProtectionApplied() { return protectionApplied; }
    public boolean isAllowed() { return gate.isAllowed(); }
    public void reset() {
        session = gate.beginSession();
        parkingSequence = 0;
        protectionSequence = 0;
        parked = null;
        protectionApplied = null;
    }
}
