package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import java.util.Objects;

/** Approval controls for three fixed images drawn by this app, never device photos. */
public final class ApprovedExamplePhotosView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private final Activity activity;
    private final float scale;
    private final ExamplePhotoApprovals approvals;
    private final boolean protectionOn;
    private final Runnable onBack;
    private final LinearLayout root;

    public ApprovedExamplePhotosView(Activity activity, float scale, ExamplePhotoApprovals approvals,
                                     boolean protectionOn, Runnable onBack) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.scale = scale;
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.protectionOn = protectionOn;
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        root = column();
        root.setPadding(dp(36), dp(22), dp(36), dp(24));
        render();
    }

    public View getView() { return root; }

    private void render() {
        root.removeAllViews();
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 뒤로");
        back.setContentDescription("예시 사진 관리에서 뒤로");
        back.setOnClickListener(v -> onBack.run());
        heading.addView(back);
        TextView title = label("예시 사진 허용 목록", 30, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(15);
        heading.addView(title, titleParams);
        if (approvals.hasStorageError() || !approvals.getApproved().isEmpty()) {
            TextView reset = action("목록 초기화");
            reset.setContentDescription("이 앱의 예시 사진 허용 목록 초기화");
            reset.setOnClickListener(v -> confirmReset());
            heading.addView(reset);
        }
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(65)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        TextView note = label("앱 공통 허용 목록이며 변경 즉시 저장됩니다. 이 앱에 그린 가상 예시만 관리합니다. 실제 사진·외부 앨범이나 차량 기본 화면은 연결되지 않았습니다.",
                16, MUTED, false);
        note.setPadding(0, dp(14), 0, dp(10));
        root.addView(note);
        String message = approvals.hasStorageError()
                ? "허용 목록을 읽거나 저장하지 못해 예시를 모두 가립니다."
                : protectionOn ? "보호 요청 중에는 허용한 예시만 표시합니다."
                : "현재 보호 요청이 꺼져 예시 전체가 표시됩니다.";
        TextView status = label(message, 18, approvals.hasStorageError() ? MUTED : ACCENT, true);
        status.setPadding(0, 0, 0, dp(10));
        root.addView(status);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout items = column();
        scroll.addView(items, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        for (ExamplePhotoApprovals.Photo photo : ExamplePhotoApprovals.Photo.values()) {
            items.addView(exampleRow(photo), new LinearLayout.LayoutParams(-1, dp(96)));
            items.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private View exampleRow(ExamplePhotoApprovals.Photo photo) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        boolean visible = approvals.isVisible(photo, protectionOn);
        TextView illustration = label(visible ? symbol(photo) : "■", 24, TEXT, true);
        illustration.setGravity(Gravity.CENTER);
        GradientDrawable tile = new GradientDrawable();
        tile.setCornerRadius(dp(8));
        tile.setColor(visible ? color(photo) : Color.rgb(61, 71, 78));
        illustration.setBackground(tile);
        illustration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(illustration, new LinearLayout.LayoutParams(dp(62), dp(62)));
        LinearLayout words = column();
        words.setPadding(dp(15), 0, dp(12), 0);
        TextView name = label(photo.getLabel(), 21, TEXT, true);
        words.addView(name);
        TextView state = label(visible ? "이 앱의 예시 표시 중" : "이 앱의 예시 가려짐", 15, MUTED, false);
        words.addView(state);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(activity);
        control.setMinWidth(dp(64));
        control.setMinHeight(dp(48));
        control.setContentDescription(photo.getLabel() + " 허용");
        control.setEnabled(!approvals.hasStorageError());
        control.setChecked(approvals.isApproved(photo));
        control.setTrackTintList(new ColorStateList(new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {ACCENT, Color.rgb(78, 88, 96)}));
        control.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        control.setOnCheckedChangeListener((button, checked) -> {
            if (!approvals.setApproved(photo, checked)) {
                Toast.makeText(activity, "예시 사진 설정을 저장하지 못했습니다.", Toast.LENGTH_LONG).show();
            }
            render();
        });
        row.addView(control);
        return row;
    }

    private void confirmReset() {
        new AlertDialog.Builder(activity).setTitle("예시 사진 허용 목록 초기화")
                .setMessage("이 앱의 가상 예시 사진 허용 목록만 초기화합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("초기화", (dialog, which) -> {
                    if (!approvals.clear()) Toast.makeText(activity, "목록을 초기화하지 못했습니다.", Toast.LENGTH_LONG).show();
                    render();
                }).show();
    }

    private static String symbol(ExamplePhotoApprovals.Photo photo) {
        switch (photo) {
            case SKY: return "☀";
            case GARDEN: return "✿";
            default: return "●";
        }
    }
    private static int color(ExamplePhotoApprovals.Photo photo) {
        switch (photo) {
            case SKY: return Color.rgb(51, 117, 145);
            case GARDEN: return Color.rgb(57, 111, 83);
            default: return Color.rgb(120, 85, 70);
        }
    }
    private TextView action(String value) {
        TextView view = label(value, 18, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), 0, dp(10), 0);
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
    private LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private View divider() { View v = new View(activity); v.setBackgroundColor(LINE); return v; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
