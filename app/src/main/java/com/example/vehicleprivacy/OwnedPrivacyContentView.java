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

/** A real masking demonstration for fixed examples inside this app's own window. */
public final class OwnedPrivacyContentView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int PANEL = Color.rgb(19, 32, 42);
    private static final int LINE = Color.rgb(70, 91, 103);

    private final Activity activity;
    private final float scale;
    private final OwnedPrivacyPolicy policy;
    private final boolean requested;
    private final LinearLayout root;

    public OwnedPrivacyContentView(Activity activity, float scale, Preset preset,
                                   boolean protectionRequested, Runnable onBack) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.scale = scale;
        this.policy = new OwnedPrivacyPolicy(preset, protectionRequested);
        this.requested = protectionRequested;
        Objects.requireNonNull(onBack, "onBack");
        root = column();
        root.setPadding(dp(32), dp(24), dp(32), dp(24));
        build(onBack);
    }

    public View getView() { return root; }

    /**
     * Whether protection is visibly applied in this app-owned example screen.
     * False also covers unknown visibility; use getProtectionState() to distinguish it.
     * Neither method describes the vehicle launcher, other apps, audio or system notifications.
     */
    public boolean isProtectionApplied() { return Boolean.TRUE.equals(getProtectionState()); }

    /** Returns null if the example screen is not currently visible and focused. */
    public Boolean getProtectionState() {
        if (!root.isAttachedToWindow() || !root.isShown() || !activity.hasWindowFocus()
                || activity.isFinishing()) return null;
        return policy.isProtectionApplied();
    }

    private void build(Runnable onBack) {
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 홈");
        back.setContentDescription("앱 내부 예시를 닫고 홈으로 돌아가기");
        back.setOnClickListener(v -> onBack.run());
        heading.addView(back);
        TextView title = label("앱 내부 예시 보호", 30, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(16);
        heading.addView(title, titleParams);
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(65)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        TextView note = label("앱 안의 가상 예시만 보호합니다. 차량 기본 화면과 다른 앱은 연결되지 않았습니다.",
                16, MUTED, false);
        note.setPadding(0, dp(14), 0, dp(14));
        root.addView(note);
        String status = policy.isProtectionApplied() ? "이 화면의 선택 항목 가림 적용"
                : requested ? "이 화면에 선택된 보호 대상이 없습니다" : "이 화면의 보호 요청 꺼짐";
        TextView statusView = label(status, 21, policy.isProtectionApplied() ? ACCENT : MUTED, true);
        statusView.setContentDescription("앱 내부 예시: " + status);
        root.addView(statusView);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout items = column();
        items.setPadding(0, dp(20), 0, dp(15));
        scroll.addView(items, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        items.addView(sectionTitle("기본 화면 예시"));
        items.addView(contentRow(policy.homeText()));
        items.addView(gap(dp(18)));
        items.addView(sectionTitle("연락처 예시"));
        items.addView(contentRow(policy.contactNameText()));
        LinearLayout photoRow = row();
        photoRow.setGravity(Gravity.CENTER_VERTICAL);
        photoRow.setPadding(dp(16), dp(10), dp(16), dp(10));
        photoRow.setBackground(panel());
        TextView photo = label(policy.isContactPhotoMasked() ? "■" : "가", 26, TEXT, true);
        photo.setGravity(Gravity.CENTER);
        photo.setBackground(photoBackground(policy.isContactPhotoMasked()));
        photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        photoRow.addView(photo, new LinearLayout.LayoutParams(dp(54), dp(54)));
        TextView photoDescription = label(policy.contactPhotoText(), 21, TEXT, true);
        photoDescription.setContentDescription(policy.contactPhotoText());
        LinearLayout.LayoutParams photoTextParams = new LinearLayout.LayoutParams(0, -2, 1);
        photoTextParams.leftMargin = dp(16);
        photoRow.addView(photoDescription, photoTextParams);
        items.addView(gap(dp(8)));
        items.addView(photoRow, new LinearLayout.LayoutParams(-1, dp(76)));
        items.addView(gap(dp(8)));
        items.addView(contentRow(policy.nicknameText()));
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text, 20, MUTED, true);
        view.setPadding(0, 0, 0, dp(10));
        return view;
    }

    private TextView contentRow(String text) {
        TextView view = label(text, 21, TEXT, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(16), dp(10), dp(16), dp(10));
        view.setMinHeight(dp(68));
        view.setBackground(panel());
        view.setContentDescription(text);
        return view;
    }

    private TextView action(String text) {
        TextView view = label(text, 20, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setMinWidth(dp(70));
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private GradientDrawable panel() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(PANEL);
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), LINE);
        return shape;
    }

    private GradientDrawable photoBackground(boolean masked) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(masked ? Color.rgb(61, 71, 78) : Color.rgb(39, 99, 119));
        return shape;
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
    private View divider() { View view = new View(activity); view.setBackgroundColor(LINE); return view; }
    private View gap(int height) {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return view;
    }
    private int dp(float value) {
        return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density));
    }
}
