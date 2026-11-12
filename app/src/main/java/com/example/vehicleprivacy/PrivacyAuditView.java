package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Displays only the app's own time-and-decision audit events. */
public final class PrivacyAuditView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private final Activity activity;
    private final float scale;
    private final PrivacyAuditLog log;
    private final Runnable onBack;
    private final Runnable onClear;
    private final LinearLayout root;

    public PrivacyAuditView(Activity activity, float scale, PrivacyAuditLog log,
                            Runnable onBack, Runnable onClear) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.scale = scale;
        this.log = Objects.requireNonNull(log, "log");
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        this.onClear = Objects.requireNonNull(onClear, "onClear");
        root = column();
        root.setPadding(dp(42), dp(25), dp(42), dp(30));
        build();
    }

    public View getView() { return root; }

    private void build() {
        TextView back = action("‹ 홈");
        back.setContentDescription("접근 기록을 닫고 홈으로 돌아가기");
        back.setOnClickListener(v -> onBack.run());
        LinearLayout header = row();
        header.addView(back, new LinearLayout.LayoutParams(-2, dp(55)));
        root.addView(header);

        LinearLayout titleRow = row();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("앱 접근 확인 기록", 34, TEXT, true);
        title.setAccessibilityHeading(true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        List<PrivacyAuditLog.Entry> entries = log.getEntries();
        boolean failed = log.hasStorageError();
        if (failed || !entries.isEmpty()) {
            TextView clear = action("기록 삭제");
            clear.setContentDescription("이 앱의 접근 확인 기록 삭제");
            clear.setOnClickListener(v -> confirmClear());
            titleRow.addView(clear);
        }
        root.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(70)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        TextView note = label("보호 요청 중 이 앱에서 관찰한 상세 확인 시도만 기록합니다. 차량 전체 기록은 아닙니다.\n"
                + "시각과 허용·차단 결과만 최대 100건, 7일 보관하며 앱 실행·확인 때 오래된 기록을 정리합니다.",
                16, MUTED, false);
        note.setPadding(0, dp(15), 0, dp(15));
        root.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout list = column();
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (failed) {
            list.addView(label("기록을 읽거나 저장하지 못했습니다.", 22, TEXT, true));
            TextView explain = label("저장 상태를 확인해 주세요. 삭제를 선택하면 이 앱의 기록만 지워집니다.",
                    16, MUTED, false);
            explain.setPadding(0, dp(12), 0, 0);
            list.addView(explain);
        } else if (entries.isEmpty()) {
            TextView empty = label("기록된 확인 시도가 없습니다.", 22, MUTED, false);
            empty.setPadding(0, dp(15), 0, dp(15));
            list.addView(empty);
        } else {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.KOREA);
            for (PrivacyAuditLog.Entry entry : entries) {
                String time = formatter.format(new Date(entry.getOccurredAtMillis()));
                String decision = entry.wasAllowed() ? "허용" : "차단";
                LinearLayout item = row();
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setContentDescription(time + ", 상세 확인 " + decision);
                item.addView(label(time, 20, TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
                item.addView(label(decision, 20, entry.wasAllowed() ? ACCENT : MUTED, true));
                list.addView(item, new LinearLayout.LayoutParams(-1, dp(64)));
                list.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(activity).setTitle("앱 접근 기록 삭제")
                .setMessage("이 앱의 확인 시각과 허용·차단 기록만 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> onClear.run()).show();
    }

    private TextView action(String text) {
        TextView view = label(text, 20, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
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
