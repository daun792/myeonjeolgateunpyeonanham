package com.example.vehicleprivacy;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Edits a draft only. The caller decides whether the draft is saved or discarded. */
public final class PresetSettingsView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);
    private final Activity activity;
    private final float scale;
    private final Preset preset;
    private final Runnable onSave;
    private final Runnable onCancel;
    private final Runnable onOpenNotifications;
    private final FrameLayout root;
    private int tab;
    private String detail = "";

    public PresetSettingsView(Activity activity, float scale, Preset preset,
                              Runnable onSave, Runnable onCancel, Runnable onOpenNotifications) {
        this.activity = activity;
        this.scale = scale;
        this.preset = preset;
        this.onSave = onSave;
        this.onCancel = onCancel;
        this.onOpenNotifications = onOpenNotifications;
        this.root = new FrameLayout(activity);
        render();
    }

    public View getView() { return root; }
    public int getTab() { return tab; }
    public String getDetail() { return detail; }
    public void restoreLocation(int savedTab, String savedDetail) {
        tab = Math.max(0, Math.min(3, savedTab));
        detail = savedDetail == null ? "" : savedDetail;
        render();
    }
    /** Return true if a detail page consumed Back; caller handles Back otherwise. */
    public boolean handleBack() {
        if (detail.isEmpty()) return false;
        detail = "";
        render();
        return true;
    }

    private void render() {
        root.removeAllViews();
        LinearLayout page = column();
        page.setPadding(dp(32), dp(18), dp(32), dp(20));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action(detail.isEmpty() ? "‹ 프리셋 목록" : "‹ 뒤로", false);
        back.setOnClickListener(v -> { if (!handleBack()) activity.onBackPressed(); });
        header.addView(back);
        TextView title = label(preset.getName() + " 설정", 30, TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(20);
        header.addView(title, titleParams);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(60)));
        page.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));

        if (detail.isEmpty()) {
            LinearLayout tabs = row();
            String[] titles = {"보호 대상", "자동 실행", "오디오 보호", "잠시 보기"};
            for (int i = 0; i < titles.length; i++) {
                final int selected = i;
                TextView item = label(titles[i], 20, tab == i ? ACCENT : TEXT, tab == i);
                item.setGravity(Gravity.CENTER);
                item.setMinHeight(dp(50));
                item.setClickable(true);
                item.setFocusable(true);
                item.setContentDescription(titles[i] + (tab == i ? ", 선택됨" : ""));
                item.setOnClickListener(v -> { tab = selected; render(); });
                tabs.addView(item, new LinearLayout.LayoutParams(0, dp(56), 1));
            }
            page.addView(tabs);
            page.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout contents = column();
        contents.setPadding(0, dp(10), 0, dp(10));
        scroll.addView(contents, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (detail.isEmpty()) {
            switch (tab) {
                case 0: protection(contents); break;
                case 1: automation(contents); break;
                case 2: audio(contents); break;
                default: preview(contents); break;
            }
        } else {
            detail(contents);
        }
        footer(page);
    }

    private void protection(LinearLayout page) {
        setting(page, "기본 화면", "protect_screen", true, "screen");
        setting(page, "연락처", "protect_contact", true, "contact");
        setting(page, "알림 미리보기", "protect_notifications", true, "notification");
    }
    private void automation(LinearLayout page) {
        page.addView(label("조건별 실행 규칙", 17, MUTED, false));
        setting(page, "문 열림", "auto_door", false, "door");
        setting(page, "동승자 탑승", "auto_passenger", false, "passenger");
        setting(page, "장소", "auto_location", false, "location");
    }
    private void audio(LinearLayout page) {
        setting(page, "통화 배경 음성 억제", "audio_call", false, "call");
        setting(page, "미디어 보호", "audio_media", false, "media");
    }
    private void preview(LinearLayout page) {
        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label("잠시 보기 범위", 23, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = action("전체 선택", false);
        all.setOnClickListener(v -> {
            for (String key : PREVIEW_KEYS) preset.setBoolean(key, true);
            render();
        });
        top.addView(all);
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(66)));
        page.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
        check(page, "연락처 이름", PREVIEW_KEYS[0]);
        check(page, "연락처 사진 · 애칭", PREVIEW_KEYS[1]);
        check(page, "알림 미리보기 내용", PREVIEW_KEYS[2]);
        check(page, "기본 화면 사진", PREVIEW_KEYS[3]);
        page.addView(label("공개 요청은 누르는 동안에만 유지됩니다. 실제 화면 연동은 준비 중입니다.", 15, MUTED, false));
    }
    private static final String[] PREVIEW_KEYS = {"preview_contact_name", "preview_contact_photo", "preview_notification", "preview_screen_photo"};

    private void setting(LinearLayout parent, String title, String key, boolean defaultValue, String detailKey) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = label(title, 23, TEXT, true);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = toggle(title, key, defaultValue);
        row.addView(control);
        TextView more = action("세부 설정  ›", false);
        more.setContentDescription(title + " 세부 설정");
        more.setOnClickListener(v -> { detail = detailKey; render(); });
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(-2, -2);
        moreParams.leftMargin = dp(15);
        row.addView(more, moreParams);
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(84)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void detail(LinearLayout page) {
        switch (detail) {
            case "screen":
                heading(page, "기본 화면 보호");
                page.addView(label("민감 정보 표시 방식", 20, TEXT, true));
                radioChoice(page, "screen_mode", "hide", "가리기", "public", "공개 가능한 표현으로 바꾸기");
                unavailable(page, "사진 승인 관리");
                break;
            case "contact":
                heading(page, "연락처 보호");
                simpleSetting(page, "연락처 사진 가리기", "hide_contact_photo", true);
                simpleSetting(page, "애칭 가리기", "hide_contact_nickname", true);
                break;
            case "notification":
                heading(page, "알림 미리보기");
                simpleSetting(page, "알림 미리보기 가리기", "hide_notification_preview", true);
                simpleSetting(page, "보호 중 알림 보관", "hold_notifications", true);
                notificationLink(page);
                break;
            case "door": case "location":
                heading(page, detail.equals("door") ? "문 열림 조건" : "장소 조건");
                page.addView(label("조건을 감지하면 미디어에 요청할 동작", 19, TEXT, true));
                radioChoice(page, detail.equals("door") ? "door_action" : "location_action",
                        "pause", "일시정지", "mute", "음소거");
                page.addView(label("차량 상태 및 장소 감지 연동은 준비 중입니다.", 16, MUTED, false));
                break;
            case "passenger":
                heading(page, "동승자 탑승 조건");
                page.addView(label("동승자 탑승 시 알림 미리보기를 가리도록 요청합니다.", 19, TEXT, false));
                page.addView(label("탑승 감지 및 실제 알림 제어는 준비 중입니다.", 16, MUTED, false));
                break;
            case "call":
                heading(page, "통화 배경 음성 억제");
                page.addView(label("통화 상대에게 전달되는 동승자와 배경 음성의 억제를 요청합니다.", 19, TEXT, false));
                page.addView(label("실제 통화 음성 처리 연동은 준비 중입니다.", 16, MUTED, false));
                break;
            case "media":
                heading(page, "미디어 보호");
                page.addView(label("미디어 보호 설정의 실제 적용은 준비 중입니다.", 19, MUTED, false));
                break;
            default:
                heading(page, "세부 설정");
                page.addView(label("준비 중입니다.", 18, MUTED, false));
        }
        page.addView(label("변경 사항은 프리셋 설정에서 저장합니다.", 15, MUTED, false));
    }

    private void heading(LinearLayout parent, String title) {
        TextView heading = label(title, 27, ACCENT, true);
        parent.addView(heading, new LinearLayout.LayoutParams(-1, dp(58)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void simpleSetting(LinearLayout parent, String title, String key, boolean defaultValue) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label(title, 21, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(toggle(title, key, defaultValue));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(74)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private Switch toggle(String title, String key, boolean defaultValue) {
        Switch sw = new Switch(activity);
        sw.setContentDescription(title + " 설정");
        sw.setMinWidth(dp(64));
        sw.setMinHeight(dp(48));
        sw.setChecked(preset.getBoolean(key, defaultValue));
        sw.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{ACCENT, Color.rgb(78, 88, 96)}));
        sw.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        sw.setOnCheckedChangeListener((button, checked) -> preset.setBoolean(key, checked));
        return sw;
    }
    private void radioChoice(LinearLayout parent, String key, String value1, String title1, String value2, String title2) {
        RadioGroup group = new RadioGroup(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        String chosen = preset.getString(key, value1);
        for (int i = 0; i < 2; i++) {
            final String value = i == 0 ? value1 : value2;
            RadioButton button = new RadioButton(activity);
            button.setText(i == 0 ? title1 : title2);
            button.setTextColor(TEXT);
            button.setTextSize(20 * scale);
            button.setButtonTintList(ColorStateList.valueOf(ACCENT));
            button.setPadding(dp(8), dp(5), dp(8), dp(5));
            button.setMinHeight(dp(58));
            button.setChecked(value.equals(chosen));
            button.setOnClickListener(v -> preset.setString(key, value));
            group.addView(button, new RadioGroup.LayoutParams(-1, -2));
        }
        parent.addView(group);
    }
    private void check(LinearLayout parent, String title, String key) {
        CheckBox box = new CheckBox(activity);
        box.setText(title);
        box.setTextColor(TEXT);
        box.setTextSize(20 * scale);
        box.setButtonTintList(ColorStateList.valueOf(ACCENT));
        box.setMinHeight(dp(56));
        box.setChecked(preset.getBoolean(key, false));
        box.setOnCheckedChangeListener((button, checked) -> preset.setBoolean(key, checked));
        parent.addView(box, new LinearLayout.LayoutParams(-1, dp(62)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void unavailable(LinearLayout parent, String title) {
        TextView item = label(title + " · 준비 중", 19, MUTED, false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(item, new LinearLayout.LayoutParams(-1, dp(60)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void notificationLink(LinearLayout parent) {
        TextView link = label("보관 알림 확인  ›", 19, ACCENT, true);
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setMinHeight(dp(48));
        link.setClickable(true);
        link.setFocusable(true);
        link.setContentDescription("보관 알림 확인");
        link.setOnClickListener(v -> onOpenNotifications.run());
        parent.addView(link, new LinearLayout.LayoutParams(-1, dp(60)));
        parent.addView(line(), new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void footer(LinearLayout page) {
        TextView note = label("설정만 저장됩니다. 실제 보호 기능은 연동 전입니다. 안전 정보는 가리지 않도록 설계합니다.", 14, MUTED, false);
        page.addView(note);
        LinearLayout actions = row();
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        if (detail.isEmpty()) {
            TextView cancel = action("취소", false);
            cancel.setOnClickListener(v -> onCancel.run());
            actions.addView(cancel, new LinearLayout.LayoutParams(dp(120), dp(54)));
            TextView save = action("저장", true);
            save.setOnClickListener(v -> onSave.run());
            LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(150), dp(54));
            saveParams.leftMargin = dp(12);
            actions.addView(save, saveParams);
        } else {
            TextView back = action("뒤로", false);
            back.setOnClickListener(v -> handleBack());
            actions.addView(back, new LinearLayout.LayoutParams(dp(120), dp(54)));
        }
        page.addView(actions, new LinearLayout.LayoutParams(-1, dp(62)));
    }
    private TextView action(String text, boolean filled) {
        TextView view = label(text, 18, filled ? Color.rgb(8, 22, 27) : ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(filled ? ACCENT : Color.TRANSPARENT);
        if (!filled) bg.setStroke(dp(1), ACCENT);
        view.setBackground(bg);
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
    private View line() { View v = new View(activity); v.setBackgroundColor(LINE); return v; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
