package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shows only masked rows; opening content requires confirmed protection and parking states. */
public final class HeldNotificationsView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int DIVIDER = Color.rgb(88, 99, 110);

    private final Activity activity;
    private final float scale;
    private final List<HeldNotification> items;
    private final Boolean protectionApplied;
    private final Boolean parked;
    private final boolean storageProblem;
    private final Runnable onBack;
    private final Runnable onCreateTest;
    private final Runnable onClearTests;
    private final LinearLayout root;

    public HeldNotificationsView(Activity activity, float scale, List<HeldNotification> items,
                                 Boolean protectionApplied, Boolean parked,
                                 boolean storageProblem, Runnable onBack, Runnable onCreateTest,
                                 Runnable onClearTests) {
        this.activity = activity;
        this.scale = scale;
        this.items = items == null ? Collections.emptyList() : new ArrayList<>(items);
        this.protectionApplied = protectionApplied;
        this.parked = parked;
        this.storageProblem = storageProblem;
        this.onBack = onBack;
        this.onCreateTest = onCreateTest;
        this.onClearTests = onClearTests;
        root = column();
        root.setPadding(dp(42), dp(25), dp(42), dp(30));
        render();
    }

    public View getView() { return root; }

    private void render() {
        TextView back = action("‹ 홈");
        back.setContentDescription("홈으로 돌아가기");
        back.setOnClickListener(v -> onBack.run());
        LinearLayout header = row();
        header.addView(back, new LinearLayout.LayoutParams(-2, dp(55)));
        root.addView(header);

        TextView title = label("보관 알림", 38, TEXT, true);
        title.setAccessibilityHeading(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(70)));
        root.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout summary = row();
        summary.setGravity(Gravity.CENTER_VERTICAL);
        TextView count = label(storageProblem
                ? "앱 테스트 보관함 읽기 오류"
                : "앱 테스트 알림 " + items.size() + "개", 25, storageProblem ? MUTED : TEXT, true);
        count.setPadding(0, dp(15), 0, dp(15));
        summary.addView(count, new LinearLayout.LayoutParams(0, -2, 1));
        TextView create = action("테스트 알림 만들기");
        create.setGravity(Gravity.CENTER);
        create.setContentDescription("앱 테스트 알림 만들기");
        create.setOnClickListener(v -> onCreateTest.run());
        summary.addView(create, new LinearLayout.LayoutParams(-2, dp(56)));
        if (!storageProblem && !items.isEmpty()) {
            TextView clear = action("테스트 모두 삭제");
            clear.setGravity(Gravity.CENTER);
            clear.setContentDescription("앱 테스트 알림 모두 삭제");
            clear.setOnClickListener(v -> confirmClearTests());
            summary.addView(clear, new LinearLayout.LayoutParams(-2, dp(56)));
        }
        root.addView(summary);

        TextView note = label("앱에서 만든 예시만 표시합니다. 실제 시스템 알림 수집과 차량 상태 확인은 연결되지 않았습니다.",
                16, MUTED, false);
        note.setPadding(0, 0, 0, dp(16));
        root.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout list = column();
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (storageProblem) {
            list.addView(label("저장된 테스트 알림을 읽을 수 없습니다.", 23, TEXT, true));
            TextView explain = label("테스트 알림을 새로 만들면 보관함을 다시 시작할 수 있습니다.", 17, MUTED, false);
            explain.setPadding(0, dp(15), 0, dp(15));
            list.addView(explain);
        } else if (items.isEmpty()) {
            TextView empty = label("만들어진 테스트 알림이 없습니다.", 23, MUTED, false);
            empty.setPadding(0, dp(18), 0, dp(18));
            list.addView(empty);
        } else {
            for (int index = 0; index < items.size(); index++) {
                HeldNotification item = items.get(index);
                list.addView(maskedRow(item, index + 1), new LinearLayout.LayoutParams(-1, dp(82)));
                list.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }

        TextView footer = label("보호 해제 후 주차 중 항목을 선택해 확인", 18, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(14), 0, dp(10));
        root.addView(footer);
    }

    private View maskedRow(HeldNotification item, int position) {
        LinearLayout entry = row();
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setMinimumHeight(dp(56));
        entry.setClickable(true);
        entry.setFocusable(true);
        // Generic numbering is deliberate: neither the sender, preview nor timestamp
        // appears in the view hierarchy or screen-reader label while content is hidden.
        entry.setContentDescription("테스트 알림 " + position + ", 내용 가림");
        entry.setOnClickListener(v -> showSelected(item));

        TextView number = label(Integer.toString(position), 19, TEXT, true);
        number.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(46, 76, 86));
        number.setBackground(circle);
        entry.addView(number, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView name = label("테스트 알림 " + position, 24, TEXT, true);
        name.setPadding(dp(18), 0, dp(8), 0);
        entry.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView masked = label("🔒  내용 가림", 20, TEXT, true);
        masked.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        entry.addView(masked);
        TextView arrow = label("  ›", 30, TEXT, false);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        entry.addView(arrow);
        return entry;
    }

    private void showSelected(HeldNotification item) {
        if (storageProblem || !Boolean.FALSE.equals(protectionApplied) || !Boolean.TRUE.equals(parked)) {
            new AlertDialog.Builder(activity).setTitle("내용을 확인할 수 없습니다")
                    .setMessage(blockedReason())
                    .setPositiveButton("확인", null).show();
            return;
        }
        String source = item.getSourceIfAllowed(protectionApplied, parked).orElse(null);
        String body = item.getBodyIfAllowed(protectionApplied, parked).orElse(null);
        if (source == null || body == null) {
            new AlertDialog.Builder(activity).setTitle("내용을 확인할 수 없습니다")
                    .setMessage("알림 내용을 불러올 수 없습니다.")
                    .setPositiveButton("확인", null).show();
            return;
        }
        new AlertDialog.Builder(activity).setTitle("보관 알림")
                .setMessage("출처: " + source + "\n\n" + body)
                .setPositiveButton("닫기", null).show();
    }

    private void confirmClearTests() {
        new AlertDialog.Builder(activity)
                .setTitle("테스트 알림 모두 삭제")
                .setMessage("이 앱에서 만든 테스트 알림만 삭제됩니다. 모두 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> onClearTests.run())
                .show();
    }

    private String blockedReason() {
        if (storageProblem) return "테스트 보관함을 읽을 수 없습니다.";
        if (protectionApplied == null || parked == null) {
            return "실제 보호 상태와 주차 상태를 확인한 뒤 내용을 볼 수 있습니다.";
        }
        if (protectionApplied && !parked) return "보호를 해제하고 주차한 뒤 확인해 주세요.";
        if (protectionApplied) return "보호를 해제한 뒤 주차 상태에서 확인해 주세요.";
        return "주차한 뒤 확인해 주세요.";
    }

    private TextView action(String text) {
        TextView view = label(text, 21, ACCENT, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, dp(16), 0);
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
    private View line() {
        View view = new View(activity);
        view.setBackgroundColor(DIVIDER);
        return view;
    }
    private int dp(float value) {
        return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density));
    }
}
