package com.example.vehicleprivacy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Objects;

/** Demonstrates the real decision logic using only its own fixed, simulated data. */
public final class DisclosureDemoView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private static final int SELECTED = Color.rgb(24, 59, 76);
    private final Activity activity;
    private final float scale;
    private final Runnable onBack;
    private final DisclosureDemoScenario scenario = new DisclosureDemoScenario();
    private final LinearLayout root;
    private boolean exampleOpen;

    public DisclosureDemoView(Activity activity, float scale, Runnable onBack) {
        this.activity = Objects.requireNonNull(activity);
        this.scale = scale;
        this.onBack = Objects.requireNonNull(onBack);
        root = new LinearLayout(activity) {
            @Override protected void onDetachedFromWindow() {
                close();
                super.onDetachedFromWindow();
            }
        };
        root.setSaveEnabled(false);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(36), dp(22), dp(36), dp(24));
        render();
    }

    public View getView() { return root; }

    /** Close and forget hypothetical state when leaving or backgrounding the view. */
    public void close() {
        scenario.reset();
        exampleOpen = false;
        render();
    }

    private void render() {
        root.removeAllViews();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 홈", false);
        back.setOnClickListener(v -> { close(); onBack.run(); });
        header.addView(back, new LinearLayout.LayoutParams(dp(100), dp(56)));
        TextView title = label("알림 공개 조건 시뮬레이션", 30, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = column();
        body.setPadding(0, dp(18), 0, dp(15));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(label("이 화면은 가상 시나리오입니다. 실제 차량 상태·보호 상태·보관 알림에는 영향을 주지 않습니다.", 17, MUTED, false));
        body.addView(space(18));
        body.addView(label("가상 차량 상태", 22, TEXT, true));
        body.addView(space(8));
        body.addView(options(true), new LinearLayout.LayoutParams(-1, dp(58)));
        body.addView(space(18));
        body.addView(label("가상 보호 적용 상태", 22, TEXT, true));
        body.addView(space(8));
        body.addView(options(false), new LinearLayout.LayoutParams(-1, dp(58)));
        body.addView(space(20));
        body.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(16));
        boolean allowed = scenario.isAllowed();
        TextView outcome = label(allowed ? "예시 알림 열람 가능" : "예시 알림 잠김", 23, allowed ? ACCENT : TEXT, true);
        outcome.setContentDescription(allowed ? "시뮬레이션 결과, 예시 알림 열람 가능" : "시뮬레이션 결과, 예시 알림 잠김");
        body.addView(outcome);
        body.addView(space(8));
        body.addView(label("가상 주차와 보호 해제가 모두 확인된 경우에만 예시를 열 수 있습니다.", 16, MUTED, false));
        body.addView(space(15));
        TextView open = action("가상 알림 내용 보기", false);
        open.setContentDescription("고정된 가상 알림 내용 보기");
        open.setEnabled(allowed);
        open.setAlpha(allowed ? 1f : .45f);
        open.setOnClickListener(v -> {
            if (scenario.isAllowed()) { exampleOpen = true; render(); }
        });
        body.addView(open, new LinearLayout.LayoutParams(dp(230), dp(56)));
        body.addView(space(12));
        if (exampleOpen && scenario.isAllowed()) {
            TextView sample = label("가상 알림: 오늘 오후에 약속이 있습니다.", 20, TEXT, true);
            sample.setContentDescription("가상 알림: 오늘 오후에 약속이 있습니다.");
            body.addView(sample);
        } else {
            body.addView(label("가상 알림 내용이 가려져 있습니다.", 17, MUTED, false));
        }
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
            exampleOpen = false;
            if (parking) scenario.setParked(value); else scenario.setProtectionApplied(value);
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.rightMargin = dp(8);
        row.addView(button, params);
    }

    private TextView action(String text, boolean selected) {
        TextView v = label(text, 19, ACCENT, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
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
