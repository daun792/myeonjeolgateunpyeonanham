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

/** Fictitious passenger events and in-app notification previews only. */
public final class PassengerPreviewDemoView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private final Activity activity;
    private final float scale;
    private final PassengerPreviewPolicy policy;
    private final LinearLayout root;
    private TextView passengerStatus;
    private TextView preview;
    private TextView explanation;

    public PassengerPreviewDemoView(Activity activity, float scale, Preset preset,
                                    boolean protectionRequested, Runnable onBack) {
        this.activity = Objects.requireNonNull(activity);
        this.scale = scale;
        this.policy = new PassengerPreviewPolicy(preset, protectionRequested);
        Objects.requireNonNull(onBack);
        root = column();
        root.setPadding(dp(36), dp(25), dp(36), dp(24));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 홈");
        back.setContentDescription("동승자 알림 가림 연습을 종료하고 홈으로 돌아가기");
        back.setOnClickListener(v -> onBack.run());
        header.addView(back);
        TextView title = label("동승자 알림 가림 연습", 32, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = column();
        body.setPadding(0, dp(20), 0, dp(18));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(label("이 화면의 동승 상태와 알림은 모두 가상입니다. 실제 차량 센서나 다른 앱의 알림에는 연결되지 않습니다.",
                17, MUTED, false));
        body.addView(space(dp(18)));
        body.addView(label("선택한 프리셋: " + preset.getName(), 22, TEXT, true));
        body.addView(space(dp(10)));
        body.addView(label(policy.isRuleEnabled() ? "동승자 탑승 자동 실행: 켜짐" : "동승자 탑승 자동 실행: 꺼짐",
                18, ACCENT, true));
        body.addView(space(dp(18)));
        passengerStatus = label("", 21, TEXT, true);
        passengerStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(passengerStatus);
        body.addView(space(dp(12)));
        LinearLayout controls = row();
        TextView boarding = action("모의 탑승");
        boarding.setContentDescription("가상 동승자 탑승 시험");
        boarding.setOnClickListener(v -> { policy.setPassengerPresent(true); update(); });
        controls.addView(boarding, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView leaving = action("모의 하차");
        leaving.setContentDescription("가상 동승자 하차 시험");
        leaving.setOnClickListener(v -> { policy.setPassengerPresent(false); update(); });
        LinearLayout.LayoutParams leavingParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        leavingParams.leftMargin = dp(14);
        controls.addView(leaving, leavingParams);
        body.addView(controls);
        body.addView(space(dp(24)));
        body.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(18)));
        body.addView(label("앱 내부 가상 알림 미리보기", 22, TEXT, true));
        body.addView(space(dp(10)));
        preview = label("", 21, ACCENT, true);
        preview.setPadding(dp(14), dp(14), dp(14), dp(14));
        preview.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.rgb(24, 40, 49));
        box.setCornerRadius(dp(8));
        preview.setBackground(box);
        body.addView(preview, new LinearLayout.LayoutParams(-1, -2));
        explanation = label("", 16, MUTED, false);
        explanation.setPadding(0, dp(15), 0, 0);
        body.addView(explanation);
        update();
    }

    public View getView() { return root; }

    private void update() {
        passengerStatus.setText(policy.isPassengerPresent() ? "가상 동승자: 탑승" : "가상 동승자: 없음");
        preview.setText(policy.preview());
        explanation.setText(policy.isMasked()
                ? "예시 미리보기가 가려졌습니다. 하차해도 다른 보호 요청이 있으면 계속 가려집니다."
                : "예시 미리보기가 표시됩니다. 자동 실행은 프리셋 설정에 따라 동작합니다.");
    }
    private TextView action(String text) {
        TextView view = label(text, 20, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.TRANSPARENT);
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), ACCENT);
        view.setBackground(shape);
        return view;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size * scale);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private LinearLayout column() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.HORIZONTAL); return view; }
    private View divider() { View view = new View(activity); view.setBackgroundColor(LINE); return view; }
    private View space(int height) { View view = new View(activity); view.setLayoutParams(new LinearLayout.LayoutParams(1, height)); return view; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
