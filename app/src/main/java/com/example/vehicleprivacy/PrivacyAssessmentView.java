package com.example.vehicleprivacy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Displays a fixed-rule check of saved settings, not a vehicle security scan. */
public final class PrivacyAssessmentView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int WARNING = Color.rgb(255, 205, 111);
    private static final int DIVIDER = Color.rgb(88, 99, 110);

    private final Activity activity;
    private final float scale;
    private final PrivacyAssessment report;
    private final Runnable onBack;
    private final Runnable onOpenPresetSettings;
    private final LinearLayout root;

    public PrivacyAssessmentView(Activity activity, float scale, Preset preset,
                                 Runnable onBack, Runnable onOpenPresetSettings) {
        this.activity = activity;
        this.scale = scale;
        this.report = new PrivacyAssessment(preset);
        this.onBack = onBack;
        this.onOpenPresetSettings = onOpenPresetSettings;
        root = column();
        root.setPadding(dp(42), dp(25), dp(42), dp(30));
        build();
    }

    public View getView() { return root; }

    private void build() {
        TextView back = action("‹ 뒤로", 20);
        back.setContentDescription("점검을 닫고 이전 화면으로 돌아가기");
        back.setOnClickListener(v -> onBack.run());
        root.addView(back, new LinearLayout.LayoutParams(-2, dp(54)));

        TextView title = label("프라이버시 점검", 34, TEXT, true);
        title.setAccessibilityHeading(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(64)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        TextView preset = label("점검 프리셋: " + report.getPresetName(), 22, ACCENT, true);
        preset.setSingleLine(true);
        preset.setEllipsize(TextUtils.TruncateAt.END);
        preset.setPadding(0, dp(18), 0, dp(8));
        root.addView(preset);
        TextView note = label("저장된 앱 설정만 고정 규칙으로 확인합니다. 설정 양호는 실제 보호 적용을 뜻하지 않습니다.",
                16, MUTED, false);
        note.setPadding(0, 0, 0, dp(16));
        root.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout list = column();
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        for (PrivacyAssessment.Item item : report.getItems()) {
            list.addView(itemRow(item));
            list.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        }
        TextView footer = label("차량 시스템과 다른 앱의 취약점을 진단하지 않습니다.", 15, MUTED, false);
        footer.setPadding(0, dp(16), 0, 0);
        root.addView(footer);
    }

    private View itemRow(PrivacyAssessment.Item item) {
        LinearLayout outer = column();
        outer.setPadding(0, dp(14), 0, dp(14));
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label(item.getTitle(), 21, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = label(statusText(item.getStatus()), 17,
                item.getStatus() == PrivacyAssessment.SettingStatus.CONFIGURED ? ACCENT : WARNING, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), 0, dp(12), 0);
        heading.addView(status);
        if (item.getStatus() != PrivacyAssessment.SettingStatus.UNAVAILABLE) {
            TextView open = action("설정  ›", 17);
            open.setContentDescription(item.getTitle() + " 관련 프리셋 설정 열기");
            open.setOnClickListener(v -> onOpenPresetSettings.run());
            heading.addView(open);
        }
        outer.addView(heading, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView detail = label(item.getExplanation(), 16, MUTED, false);
        detail.setPadding(0, dp(4), 0, dp(4));
        outer.addView(detail);
        outer.addView(label("실제 적용: 미확인", 15, MUTED, false));
        return outer;
    }

    private String statusText(PrivacyAssessment.SettingStatus status) {
        switch (status) {
            case CONFIGURED: return "설정 양호";
            case NEEDS_ATTENTION: return "설정 확인 필요";
            default: return "설정 미지원";
        }
    }

    private TextView action(String text, int size) {
        TextView view = label(text, size, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
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

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }

    private View divider() {
        View view = new View(activity);
        view.setBackgroundColor(DIVIDER);
        return view;
    }

    private int dp(float value) {
        return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density));
    }
}
