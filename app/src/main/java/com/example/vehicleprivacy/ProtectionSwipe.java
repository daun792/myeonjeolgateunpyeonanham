package com.example.vehicleprivacy;

/** State machine for a deliberate one-finger touch swipe in the app's own UI. */
public final class ProtectionSwipe {
    private final float minX;
    private final float maxY;
    private final long maxMillis;
    private float startX;
    private float startY;
    private long startedAt;
    private boolean tracking;

    public ProtectionSwipe(float minX, float maxY, long maxMillis) {
        if (!Float.isFinite(minX) || !Float.isFinite(maxY) || minX <= 0 || maxY <= 0 || maxMillis <= 0) {
            throw new IllegalArgumentException("Invalid gesture thresholds");
        }
        this.minX = minX;
        this.maxY = maxY;
        this.maxMillis = maxMillis;
    }
    public void down(float x, float y, long time, int pointers) {
        cancel();
        if (pointers != 1 || !Float.isFinite(x) || !Float.isFinite(y) || time < 0) return;
        startX = x;
        startY = y;
        startedAt = time;
        tracking = true;
    }
    public void move(float x, float y, int pointers) {
        if (!tracking) return;
        if (pointers != 1 || !Float.isFinite(x) || !Float.isFinite(y)
                || Math.abs(y - startY) > maxY) cancel();
    }
    /** Returns an explicit request only on a valid completed horizontal swipe. */
    public Boolean up(float x, float y, long time, int pointers) {
        if (!tracking) return null;
        boolean valid = pointers == 1 && Float.isFinite(x) && Float.isFinite(y)
                && time >= startedAt && time - startedAt <= maxMillis;
        float dx = x - startX;
        float dy = y - startY;
        cancel();
        if (!valid || Math.abs(dx) < minX || Math.abs(dy) > maxY
                || Math.abs(dx) < 2f * Math.abs(dy)) return null;
        return dx > 0f;
    }
    public void cancel() { tracking = false; }
}
