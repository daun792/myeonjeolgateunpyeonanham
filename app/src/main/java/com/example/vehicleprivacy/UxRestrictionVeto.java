package com.example.vehicleprivacy;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Maps UX restrictions to negative/unknown evidence only; it never proves parking. */
public final class UxRestrictionVeto {
    private final DisclosureGate gate;
    private final LongSupplier clock;
    private long generation;
    private long session;
    private long sequence;
    private boolean active;

    public UxRestrictionVeto(DisclosureGate gate, LongSupplier clock) {
        this.gate = Objects.requireNonNull(gate);
        this.clock = Objects.requireNonNull(clock);
    }

    /** All methods must be invoked on the same thread as the gate. */
    public long start(long gateSession) {
        stop();
        if (generation == Long.MAX_VALUE) throw new IllegalStateException("UX session exhausted");
        generation++;
        session = gateSession;
        sequence = 0;
        active = true;
        publish(null);
        return generation;
    }

    public void onRestrictions(long sourceGeneration, Boolean requiresOptimization) {
        if (!active || sourceGeneration != generation) return;
        publish(Boolean.TRUE.equals(requiresOptimization) ? Boolean.FALSE : null);
    }

    public void stop() {
        if (!active) return;
        publish(null);
        active = false;
    }

    private void publish(Boolean parked) {
        long observedAt;
        try { observedAt = clock.getAsLong(); }
        catch (RuntimeException unavailable) { observedAt = -1L; parked = null; }
        gate.updateParking(session, sequence++, parked, observedAt);
    }
}
