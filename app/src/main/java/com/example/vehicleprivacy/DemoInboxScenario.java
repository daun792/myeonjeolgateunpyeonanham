package com.example.vehicleprivacy;

import java.util.Optional;

/** An isolated demonstration with only fixed synthetic notifications and no repository access. */
public final class DemoInboxScenario {
    public static final int MAX_ADDED = 20;
    public enum IncomingResult { HELD, NOT_HELD, FULL }
    private static final String ADDED_EXAMPLE = "가상 추가 알림: 프리셋 설정에 따라 보관된 예시입니다.";
    private static final String[] EXAMPLES = {
        "가상 일정: 내일 오전에 약속이 있습니다.",
        "가상 메시지: 오늘 오후에 만나요.",
        "가상 배송: 물품이 도착했습니다."
    };
    private final DisclosureDemoScenario conditions = new DisclosureDemoScenario();
    private int selected = -1;
    private boolean opened;
    private int added;

    public int count() { return EXAMPLES.length + added; }
    public int selectedIndex() { return selected; }
    public boolean hasSelection() { return selected >= 0; }
    public Boolean getParked() { return conditions.getParked(); }
    public Boolean getProtectionApplied() { return conditions.getProtectionApplied(); }
    public boolean isAllowed() { return conditions.isAllowed(); }
    public void select(int index) {
        if (index < 0 || index >= count()) {
            selected = -1;
            opened = false;
            throw new IndexOutOfBoundsException();
        }
        selected = index;
        opened = false;
    }
    public void setParked(Boolean value) { opened = false; conditions.setParked(value); }
    public void setProtectionApplied(Boolean value) { opened = false; conditions.setProtectionApplied(value); }
    public boolean openSelected() {
        opened = hasSelection() && conditions.isAllowed();
        return opened;
    }
    /** Receives a synthetic event; never accesses the real inbox or system notifications. */
    public IncomingResult receiveSynthetic(Preset preset, boolean protectionRequested) {
        opened = false;
        if (!DemoHoldDecision.shouldHold(preset, protectionRequested)) return IncomingResult.NOT_HELD;
        if (added >= MAX_ADDED) return IncomingResult.FULL;
        added++;
        return IncomingResult.HELD;
    }
    public void hideDetail() { opened = false; }
    public Optional<String> visibleBody() {
        return opened && hasSelection() && conditions.isAllowed()
                ? Optional.of(selected < EXAMPLES.length ? EXAMPLES[selected] : ADDED_EXAMPLE) : Optional.empty();
    }
    public void reset() {
        opened = false;
        selected = -1;
        added = 0;
        conditions.reset();
    }
}
