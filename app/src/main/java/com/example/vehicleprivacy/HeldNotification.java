package com.example.vehicleprivacy;

import java.util.Objects;
import java.util.Optional;

/** A held notification whose list representation never reveals private fields. */
public final class HeldNotification {
    private final String id;
    private final long receivedAtMillis;
    private final String source;
    private final String body;

    public HeldNotification(String id, long receivedAtMillis, String source, String body) {
        this.id = Objects.requireNonNull(id, "id");
        this.receivedAtMillis = receivedAtMillis;
        this.source = Objects.requireNonNull(source, "source");
        this.body = Objects.requireNonNull(body, "body");
    }

    public String getId() { return id; }
    public long getReceivedAtMillis() { return receivedAtMillis; }
    public String getListText() { return "보관된 알림"; }
    public String getAccessibilityText() { return "보관된 알림, 내용 숨김"; }

    /** Unknown protection or parking status must not unlock private content. */
    public boolean canReveal(Boolean protectionApplied, Boolean parked) {
        return Boolean.FALSE.equals(protectionApplied) && Boolean.TRUE.equals(parked);
    }

    public Optional<String> getBodyIfAllowed(Boolean protectionApplied, Boolean parked) {
        return canReveal(protectionApplied, parked) ? Optional.of(body) : Optional.empty();
    }

    public Optional<String> getSourceIfAllowed(Boolean protectionApplied, Boolean parked) {
        return canReveal(protectionApplied, parked) ? Optional.of(source) : Optional.empty();
    }
}
