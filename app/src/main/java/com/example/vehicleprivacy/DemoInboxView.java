package com.example.vehicleprivacy;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Objects;

/** A self-contained demo inbox that never receives real notifications or real gate state. */
public final class DemoInboxView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private static final int SELECTED = Color.rgb(24, 59, 76);
    private final Activity activity;
    private final float scale;
    private final Runnable onBack;
    private final DemoInboxScenario scenario = new DemoInboxScenario();
    private final Preset demoPreset;
    private boolean simulatedRequest;
    private String incomingMessage = "";
    private final LinearLayout root;
    private ScrollView scroll;

    public DemoInboxView(Activity activity, float scale, Preset preset, Runnable onBack) {
        this.activity = Objects.requireNonNull(activity);
        this.scale = scale;
        this.onBack = Objects.requireNonNull(onBack);
        this.demoPreset = Objects.requireNonNull(preset, "preset").copy();
        root = new LinearLayout(activity) {
            @Override protected void onDetachedFromWindow() {
                close();
                super.onDetachedFromWindow();
            }
        };
        root.setSaveEnabled(false);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(36), dp(22), dp(36), dp(24));
        render(false);
    }

    public View getView() { return root; }

    /** Reset all simulated state and hide details on background, detach, or navigation. */
    public void close() {
        scenario.reset();
        simulatedRequest = false;
        incomingMessage = "";
        render(false);
    }

    private void render(boolean preserveScroll) {
        int scrollY = preserveScroll && scroll != null ? scroll.getScrollY() : 0;
        root.removeAllViews();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 보관 알림", false);
        back.setContentDescription("가상 보관함 시연을 닫고 보관 알림으로 돌아가기");
        back.setOnClickListener(v -> { close(); onBack.run(); });
        header.addView(back, new LinearLayout.LayoutParams(dp(150), dp(56)));
        TextView title = label("가상 보관 알림 시연", 30, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout body = column();
        body.setPadding(0, dp(15), 0, dp(18));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(label("가상 예시 알림과 상태만 사용합니다. 실제 보관함, 차량 상태와 보호 적용 상태에는 영향을 주지 않습니다.", 16, MUTED, false));
        body.addView(space(8));
        body.addView(label("선택한 프리셋: " + demoPreset.getName(), 18, TEXT, true));
        body.addView(label("프리셋 설정을 변경하면 저장 후 이 화면을 다시 열어 주세요.", 15, MUTED, false));
        LinearLayout requestRow = row();
        requestRow.setGravity(Gravity.CENTER_VERTICAL);
        requestRow.addView(label("가상 보호 요청", 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        Switch request = new Switch(activity);
        request.setContentDescription("가상 알림 보관용 보호 요청");
        request.setMinWidth(dp(64));
        request.setMinHeight(dp(48));
        request.setChecked(simulatedRequest);
        request.setTrackTintList(new ColorStateList(new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {ACCENT, Color.rgb(78, 88, 96)}));
        request.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        request.setOnCheckedChangeListener((button, checked) -> {
            simulatedRequest = checked;
            incomingMessage = "";
            scenario.hideDetail();
            render(true);
        });
        requestRow.addView(request);
        body.addView(requestRow, new LinearLayout.LayoutParams(-1, dp(54)));
        body.addView(label("가상 보호 요청과 프리셋 알림 보호·보관 설정이 켜진 경우에만 새 예시를 보관합니다.", 15, MUTED, false));
        TextView incoming = action("새 가상 알림 발생", false);
        incoming.setContentDescription("새 가상 알림 발생");
        incoming.setOnClickListener(v -> {
            DemoInboxScenario.IncomingResult result = scenario.receiveSynthetic(demoPreset, simulatedRequest);
            switch (result) {
                case HELD: incomingMessage = "새 가상 알림을 보관했습니다."; break;
                case FULL: incomingMessage = "가상 추가 알림은 최대 20개입니다."; break;
                default: incomingMessage = "설정 조건이 맞지 않아 새 가상 알림을 보관하지 않았습니다.";
            }
            render(true);
        });
        body.addView(incoming, new LinearLayout.LayoutParams(dp(245), dp(52)));
        if (!incomingMessage.isEmpty()) {
            TextView result = label(incomingMessage, 16, ACCENT, true);
            result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            body.addView(result);
        }
        body.addView(space(12));
        body.addView(label("가상 보관 알림 목록", 22, TEXT, true));
        body.addView(label("기본 예시 3개는 미리 보관되어 있습니다. 항목을 고른 뒤 아래에서 가상 조건을 바꾸어 확인하세요.", 15, MUTED, false));
        body.addView(space(8));
        for (int i = 0; i < scenario.count(); i++) addItem(body, i);
        body.addView(space(15));
        body.addView(label("가상 차량 상태", 20, TEXT, true));
        body.addView(space(6));
        body.addView(options(true), new LinearLayout.LayoutParams(-1, dp(53)));
        body.addView(space(12));
        body.addView(label("가상 보호 적용 상태", 20, TEXT, true));
        body.addView(space(6));
        body.addView(options(false), new LinearLayout.LayoutParams(-1, dp(53)));
        body.addView(space(15));
        body.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(12));
        String status = !scenario.hasSelection() ? "목록에서 가상 알림을 선택하세요."
                : scenario.isAllowed() ? "선택한 예시 알림 열람 가능" : "선택한 예시 알림 잠김";
        TextView outcome = label(status, 21, scenario.hasSelection() && scenario.isAllowed() ? ACCENT : TEXT, true);
        outcome.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(outcome);
        body.addView(label("가상 주차와 가상 보호 해제가 모두 확인되어야 열 수 있습니다.", 15, MUTED, false));
        body.addView(space(9));
        TextView open = action("선택한 가상 알림 내용 보기", false);
        open.setContentDescription("선택한 가상 알림 내용 보기");
        boolean enabled = scenario.hasSelection() && scenario.isAllowed();
        open.setEnabled(enabled);
        open.setAlpha(enabled ? 1f : .45f);
        open.setOnClickListener(v -> { scenario.openSelected(); render(true); });
        body.addView(open, new LinearLayout.LayoutParams(dp(300), dp(52)));
        body.addView(space(10));
        TextView detail = label(scenario.visibleBody().orElse("가상 알림 내용이 가려져 있습니다."), 19, TEXT, true);
        detail.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(detail);
        ScrollView current = scroll;
        current.post(() -> { if (scroll == current) current.scrollTo(0, scrollY); });
    }

    private void addItem(LinearLayout parent, int index) {
        boolean selected = scenario.selectedIndex() == index;
        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setPadding(dp(10), dp(4), dp(10), dp(4));
        item.setContentDescription("가상 보관 알림 " + (index + 1) + (selected ? ", 선택됨" : ", 내용 가림"));
        item.setOnClickListener(v -> { scenario.select(index); render(true); });
        if (selected) item.setBackgroundColor(SELECTED);
        item.addView(label("가상 알림 " + (index + 1), 19, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(label(selected ? "선택됨 · 내용 가림" : "내용 가림  ›", 17, MUTED, false));
        parent.addView(item, new LinearLayout.LayoutParams(-1, dp(56)));
        parent.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private LinearLayout options(boolean parking) {
        LinearLayout choices = row();
        choice(choices, "미확인", null, parking);
        choice(choices, parking ? "주차" : "적용 중", true, parking);
        choice(choices, parking ? "주행 중" : "해제", false, parking);
        return choices;
    }

    private void choice(LinearLayout row, String text, Boolean value, boolean parking) {
        Boolean current = parking ? scenario.getParked() : scenario.getProtectionApplied();
        boolean selected = Objects.equals(current, value);
        TextView button = action(text, selected);
        button.setContentDescription((parking ? "가상 차량 상태 " : "가상 보호 상태 ") + text + (selected ? ", 선택됨" : ""));
        button.setOnClickListener(v -> {
            if (parking) scenario.setParked(value); else scenario.setProtectionApplied(value);
            render(true);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.rightMargin = dp(8);
        row.addView(button, params);
    }

    private TextView action(String text, boolean selected) {
        TextView v = label(text, 18, ACCENT, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(8), dp(6), dp(8), dp(6));
        v.setMinHeight(dp(48));
        v.setClickable(true);
        v.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? SELECTED : Color.TRANSPARENT);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), ACCENT);
        v.setBackground(bg);
        return v;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(text); v.setTextColor(color); v.setTextSize(size * scale);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private View divider() { View v = new View(activity); v.setBackgroundColor(LINE); return v; }
    private View space(int height) { View v = new View(activity); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
