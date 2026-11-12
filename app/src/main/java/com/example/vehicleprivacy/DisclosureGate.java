package com.example.vehicleprivacy;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Fail-closed access policy based on two independent, freshly verified states.
 * All methods and callbacks must be used on the same owner thread. Times must
 * use a monotonic clock, such as Android SystemClock.elapsedRealtime().
 */
public final class DisclosureGate {
    public interface Listener { void onAccessChanged(boolean allowed); }

    private final LongSupplier clock;
    private final long ttlMillis;
    private Listener listener;
    private long generation;
    private long protectionSequence = -1;
    private long parkingSequence = -1;
    private Boolean protectionApplied;
    private Boolean parked;
    private long protectionAt;
    private long parkingAt;
    private long latestProtectionAt = -1;
    private long latestParkingAt = -1;
    private boolean lastAllowed;

    public DisclosureGate(LongSupplier clock, long ttlMillis) {
        this.clock = Objects.requireNonNull(clock, "clock");
        if (ttlMillis <= 0) throw new IllegalArgumentException("ttlMillis must be positive");
        this.ttlMillis = ttlMillis;
    }

    /** Starts a new provider session; callbacks from previous sessions are ignored. */
    public long beginSession() {
        advanceGeneration();
        clearStates();
        dispatch(false, true);
        return generation;
    }

    /** The listener receives the current state immediately and later changes synchronously. */
    public void setListener(Listener listener) {
        this.listener = null;
        boolean allowed = refresh();
        this.listener = listener;
        if (listener != null) listener.onAccessChanged(allowed);
    }

    /** A detached view must not clear a listener that replaced its own. */
    public void clearListener(Listener expected) {
        if (listener == expected) listener = null;
    }

    /**
     * Sequences must be assigned before issuing asynchronous requests, separately
     * for each source. Use null for a failed or unavailable reading. Each new
     * reading notifies the listener so an expiry timer can be rescheduled.
     */
    public void updateProtection(long session, long sequence, Boolean value, long observedAt) {
        if (!isNewSequence(session, sequence, protectionSequence)) return;
        protectionSequence = sequence;
        Long now = readClock();
        if (now == null) { failClock(); return; }
        protectionApplied = isFresh(value, observedAt, now) && observedAt >= latestProtectionAt
                ? value : null;
        if (protectionApplied != null) latestProtectionAt = observedAt;
        protectionAt = observedAt;
        dispatch(evaluate(now), true);
    }

    public void updateParking(long session, long sequence, Boolean value, long observedAt) {
        if (!isNewSequence(session, sequence, parkingSequence)) return;
        parkingSequence = sequence;
        Long now = readClock();
        if (now == null) { failClock(); return; }
        parked = isFresh(value, observedAt, now) && observedAt >= latestParkingAt ? value : null;
        if (parked != null) latestParkingAt = observedAt;
        parkingAt = observedAt;
        dispatch(evaluate(now), true);
    }

    /** Invalidates both states and rejects callbacks from the disconnected session. */
    public void invalidateAll() {
        advanceGeneration();
        clearStates();
        dispatch(false, true);
    }

    public boolean isAllowed() { return refresh(); }

    /** Called when evidence changes or an expiry timer fires. */
    public boolean refresh() {
        Long now = readClock();
        if (now == null) { failClock(); return false; }
        boolean allowed = evaluate(now);
        dispatch(allowed, false);
        return allowed;
    }

    /** Schedule refresh() after this many milliseconds while content is displayed. */
    public long nextRecheckDelayMillis() {
        Long now = readClock();
        if (now == null) { failClock(); return -1; }
        boolean allowed = evaluate(now);
        dispatch(allowed, false);
        if (!allowed) return -1;
        return Math.min(ttlMillis - (now - protectionAt), ttlMillis - (now - parkingAt));
    }

    private boolean isNewSequence(long session, long sequence, long previous) {
        return generation != 0 && session == generation && sequence >= 0 && sequence > previous;
    }

    private boolean isFresh(Boolean value, long at, long now) {
        return value != null && at >= 0 && now >= at && now - at < ttlMillis;
    }

    private boolean evaluate(long now) {
        if (!isFresh(protectionApplied, protectionAt, now)) protectionApplied = null;
        if (!isFresh(parked, parkingAt, now)) parked = null;
        return Boolean.FALSE.equals(protectionApplied) && Boolean.TRUE.equals(parked);
    }

    private Long readClock() {
        try {
            long now = clock.getAsLong();
            return now >= 0 ? now : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private void failClock() {
        advanceGeneration();
        clearStates();
        dispatch(false, false);
    }

    private void advanceGeneration() {
        if (generation == Long.MAX_VALUE) {
            clearStates();
            dispatch(false, false);
            throw new IllegalStateException("Disclosure session exhausted");
        }
        generation++;
    }

    private void clearStates() {
        protectionApplied = null;
        parked = null;
        protectionSequence = -1;
        parkingSequence = -1;
        latestProtectionAt = -1;
        latestParkingAt = -1;
    }

    private void dispatch(boolean allowed, boolean evidenceChanged) {
        boolean changed = lastAllowed != allowed;
        lastAllowed = allowed;
        if ((changed || evidenceChanged) && listener != null) listener.onAccessChanged(allowed);
    }
}
