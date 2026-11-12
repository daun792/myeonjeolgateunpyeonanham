package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The first UI slice only records a request. It does not control vehicle systems. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(11, 17, 25);
    private static final int SIDEBAR = Color.rgb(13, 20, 29);
    private static final int SELECTED = Color.rgb(24, 59, 76);
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int DIVIDER = Color.rgb(88, 99, 110);
    private static final String PREFS = "privacy_settings";
    private static final String REQUEST_KEY = "request_enabled";
    private static final String PRESET_KEY = "last_preset";
    private static final String DEFAULT_PRESET = "기본 프리셋";

    private SharedPreferences preferences;
    private ProtectionController controller;
    private PresetRepository presets;
    private Preset editingPreset;
    private PresetSettingsView settingsView;
    private VehiclePreviewView vehiclePreview;
    private int restoredSettingsTab;
    private String restoredSettingsDetail;
    private String currentScreen = "home";
    // This repository contains app-generated examples only, not system notifications.
    private TestNotificationRepository testNotifications;
    private boolean detailsOpen;
    private float scale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        presets = new PresetRepository(new SharedPreferencesPresetStorage(preferences));
        testNotifications = new TestNotificationRepository(
                new SharedPreferencesTestNotificationStorage(preferences));
        String storedPreset = preferences.getString(PRESET_KEY, DEFAULT_PRESET);
        if (storedPreset == null || storedPreset.trim().isEmpty()) {
            storedPreset = DEFAULT_PRESET;
        }
        controller = new ProtectionController(preferences.getBoolean(REQUEST_KEY, false), storedPreset);
        detailsOpen = savedInstanceState != null && savedInstanceState.getBoolean("details_open", false)
                && controller.isRequested();
        if (savedInstanceState != null) {
            String restoredScreen = savedInstanceState.getString("current_screen", "home");
            if ("presets".equals(restoredScreen)) currentScreen = "presets";
            if ("notifications".equals(restoredScreen)) currentScreen = "notifications";
            if ("simulation".equals(restoredScreen)) currentScreen = "simulation";
            if ("settings".equals(restoredScreen)) {
                String editId = savedInstanceState.getString("editing_id");
                for (Preset preset : presets.getPresets()) {
                    if (preset.getId().equals(editId)) {
                        editingPreset = preset;
                        Bundle draft = savedInstanceState.getBundle("editing_values");
                        if (draft != null) {
                            Map<String, String> values = new HashMap<>();
                            for (String key : draft.keySet()) values.put(key, draft.getString(key));
                            editingPreset.replaceValues(values);
                        }
                        currentScreen = "settings";
                        restoredSettingsTab = savedInstanceState.getInt("settings_tab", 0);
                        restoredSettingsDetail = savedInstanceState.getString("settings_detail");
                        break;
                    }
                }
            }
        }
        float heightDp = getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density;
        scale = Math.max(0.70f, Math.min(1.45f, heightDp / 1000f));
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("details_open", detailsOpen);
        state.putString("current_screen", currentScreen);
        if ("settings".equals(currentScreen) && editingPreset != null) {
            state.putString("editing_id", editingPreset.getId());
            Bundle draft = new Bundle();
            for (Map.Entry<String, String> entry : editingPreset.getValues().entrySet()) {
                draft.putString(entry.getKey(), entry.getValue());
            }
            state.putBundle("editing_values", draft);
            if (settingsView != null) {
                state.putInt("settings_tab", settingsView.getTab());
                state.putString("settings_detail", settingsView.getDetail());
            }
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if ("simulation".equals(currentScreen)) {
            navigateTo("home");
            return;
        }
        if ("notifications".equals(currentScreen)) {
            currentScreen = "home";
            render();
            return;
        }
        if ("settings".equals(currentScreen)) {
            if (settingsView != null && settingsView.handleBack()) return;
            confirmDiscard(() -> { currentScreen = "presets"; render(); });
            return;
        }
        if ("presets".equals(currentScreen)) {
            currentScreen = "home";
            render();
            return;
        }
        if (detailsOpen) {
            detailsOpen = false;
            render();
        } else {
            super.onBackPressed();
        }
    }

    private void render() {
        if ("simulation".equals(currentScreen)) {
            vehiclePreview = new VehiclePreviewView(this, scale,
                    new TemporaryReveal(presets.getSelected()), () -> navigateTo("home"));
            setContentView(vehiclePreview.getView());
            return;
        }
        if (vehiclePreview != null) {
            vehiclePreview.cancelHold();
            vehiclePreview = null;
        }
        LinearLayout root = row();
        root.setBackgroundColor(BACKGROUND);
        int sidebarWidth = Math.max(dp(150), Math.round(getResources().getDisplayMetrics().widthPixels * 0.18f));
        root.addView(sidebar(), new LinearLayout.LayoutParams(sidebarWidth, -1));
        root.addView(divider(true), new LinearLayout.LayoutParams(dp(1), -1));

        if ("presets".equals(currentScreen)) {
            root.addView(presetList(), new LinearLayout.LayoutParams(0, -1, 1f));
        } else if ("notifications".equals(currentScreen)) {
            root.addView(new HeldNotificationsView(this, scale, testNotifications.getNotifications(),
                    null, null, testNotifications.hasCorruptData(), () -> navigateTo("home"),
                    this::createTestNotification, this::clearTestNotifications).getView(),
                    new LinearLayout.LayoutParams(0, -1, 1f));
        } else if ("settings".equals(currentScreen)) {
            settingsView = new PresetSettingsView(this, scale, editingPreset,
                    () -> { presets.save(editingPreset); currentScreen = "presets"; render(); },
                    () -> { currentScreen = "presets"; render(); },
                    () -> navigateTo("notifications"));
            settingsView.restoreLocation(restoredSettingsTab, restoredSettingsDetail);
            restoredSettingsTab = 0;
            restoredSettingsDetail = null;
            root.addView(settingsView.getView(), new LinearLayout.LayoutParams(0, -1, 1f));
        } else if (detailsOpen) {
            LinearLayout summary = homeContent(true);
            root.addView(summary, new LinearLayout.LayoutParams(0, -1, 1f));
            root.addView(divider(true), new LinearLayout.LayoutParams(dp(1), -1));
            root.addView(detailsPanel(), new LinearLayout.LayoutParams(0, -1, 1.05f));
        } else {
            root.addView(homeContent(false), new LinearLayout.LayoutParams(0, -1, 1f));
        }
        setContentView(root);
    }

    private LinearLayout sidebar() {
        LinearLayout side = column();
        side.setBackgroundColor(SIDEBAR);
        side.addView(space(dp(90)));

        LinearLayout home = row();
        home.setGravity(Gravity.CENTER_VERTICAL);
        boolean onHome = "home".equals(currentScreen) || "notifications".equals(currentScreen);
        if (onHome) home.setBackgroundColor(SELECTED);
        home.addView(onHome ? colorBar() : gap(dp(7)), new LinearLayout.LayoutParams(dp(7), -1));
        home.addView(gap(dp(24)));
        home.addView(icon(R.drawable.ic_home), new LinearLayout.LayoutParams(dp(30), dp(30)));
        home.addView(gap(dp(20)));
        home.addView(label("홈", 27, onHome ? ACCENT : MUTED, true));
        home.setContentDescription(onHome ? "홈, 선택됨" : "홈");
        home.setOnClickListener(v -> navigateTo("home"));
        home.setFocusable(true);
        side.addView(home, new LinearLayout.LayoutParams(-1, dp(78)));

        LinearLayout presets = row();
        presets.setGravity(Gravity.CENTER_VERTICAL);
        boolean onPresets = !onHome;
        if (onPresets) presets.setBackgroundColor(SELECTED);
        presets.addView(onPresets ? colorBar() : gap(dp(7)), new LinearLayout.LayoutParams(dp(7), -1));
        presets.addView(gap(dp(24)));
        presets.addView(icon(R.drawable.ic_tune), new LinearLayout.LayoutParams(dp(30), dp(30)));
        presets.addView(gap(dp(20)));
        LinearLayout presetLabel = column();
        presetLabel.addView(label("프리셋", 24, onPresets ? ACCENT : MUTED, true));
        presets.addView(presetLabel);
        presets.setContentDescription(onPresets ? "프리셋, 선택됨" : "프리셋");
        presets.setOnClickListener(v -> navigateTo("presets"));
        presets.setFocusable(true);
        side.addView(presets, new LinearLayout.LayoutParams(-1, dp(86)));
        return side;
    }

    private View presetList() {
        return new PresetListView(this, scale, presets.getPresets(), presets.getSelectedId(),
                new PresetListView.Listener() {
                    @Override public void onSelect(String id) {
                        updatePresets(() -> presets.select(id));
                    }
                    @Override public void onSettings(String id) {
                        for (Preset preset : presets.getPresets()) {
                            if (preset.getId().equals(id)) {
                                editingPreset = preset;
                                currentScreen = "settings";
                                render();
                                return;
                            }
                        }
                    }
                    @Override public void onAdd(String name) {
                        updatePresets(() -> presets.add(name));
                    }
                    @Override public void onRename(String id, String name) {
                        updatePresets(() -> presets.rename(id, name));
                    }
                    @Override public void onMove(String id, int index) {
                        updatePresets(() -> presets.move(id, index));
                    }
                    @Override public void onDelete(String id) {
                        updatePresets(() -> presets.delete(id));
                    }
                }).getView();
    }

    private void updatePresets(Runnable operation) {
        try {
            operation.run();
            render();
        } catch (IllegalArgumentException | IllegalStateException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createTestNotification() {
        try {
            testNotifications.addTestNotification(System.currentTimeMillis());
            render();
        } catch (IllegalStateException error) {
            String message = testNotifications.getNotifications().size()
                    >= TestNotificationRepository.MAX_NOTIFICATIONS
                    ? "테스트 알림은 최대 100개까지 보관합니다. 삭제 후 다시 시도해 주세요."
                    : "테스트 알림을 저장하지 못했습니다.";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void clearTestNotifications() {
        try {
            testNotifications.clearTestNotifications();
            render();
        } catch (IllegalStateException error) {
            Toast.makeText(this, "테스트 알림을 삭제하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateTo(String target) {
        if (target.equals(currentScreen)) return;
        Runnable change = () -> { currentScreen = target; detailsOpen = false; render(); };
        if ("settings".equals(currentScreen) && hasUnsavedPresetChanges()) confirmDiscard(change);
        else change.run();
    }

    private boolean hasUnsavedPresetChanges() {
        if (editingPreset == null) return false;
        for (Preset saved : presets.getPresets()) {
            if (saved.getId().equals(editingPreset.getId())) {
                return !saved.getValues().equals(editingPreset.getValues());
            }
        }
        return true;
    }

    private void confirmDiscard(Runnable onDiscard) {
        new AlertDialog.Builder(this).setMessage("저장하지 않은 설정을 버릴까요?")
                .setNegativeButton("계속 편집", null)
                .setPositiveButton("버리기", (dialog, which) -> onDiscard.run()).show();
    }

    @Override
    protected void onPause() {
        if (vehiclePreview != null) vehiclePreview.cancelHold();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && vehiclePreview != null) vehiclePreview.cancelHold();
    }

    private LinearLayout homeContent(boolean compact) {
        LinearLayout container = column();
        int padding = dp(compact ? 30 : 42);
        container.setPadding(padding, dp(35), padding, dp(30));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("프라이버시 보호", compact ? 34 : 43, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        if (!compact) {
            TextView simulation = action("차량 화면 시뮬레이션", 16);
            simulation.setContentDescription("차량 기본 화면 시뮬레이션 열기");
            simulation.setOnClickListener(v -> navigateTo("simulation"));
            header.addView(simulation);
            LinearLayout notification = row();
            notification.setGravity(Gravity.CENTER_VERTICAL);
            notification.addView(icon(R.drawable.ic_bell), new LinearLayout.LayoutParams(dp(30), dp(30)));
            notification.addView(gap(dp(8)));
            notification.addView(label("보관 알림", 16, TEXT, false));
            int testCount = testNotifications.getNotifications().size();
            if (!testNotifications.hasCorruptData() && testCount > 0) {
                notification.addView(gap(dp(8)));
                notification.addView(label("테스트 " + testCount, 16, ACCENT, true));
            }
            notification.setContentDescription(testNotifications.hasCorruptData()
                    ? "보관 알림 목록 열기, 테스트 보관함 읽기 오류"
                    : testCount > 0 ? "보관 알림 목록 열기, 테스트 " + testCount + "개"
                    : "보관 알림 목록 열기");
            notification.setClickable(true);
            notification.setFocusable(true);
            notification.setPadding(dp(12), dp(10), dp(12), dp(10));
            notification.setMinimumHeight(dp(48));
            notification.setOnClickListener(v -> navigateTo("notifications"));
            header.addView(notification);
        }
        container.addView(header, new LinearLayout.LayoutParams(-1, dp(67)));
        container.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout body = column();
        body.setPadding(0, dp(26), 0, dp(16));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        container.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (compact) {
            addCompactSummary(body);
        } else {
            addMainSummary(body);
        }
        return container;
    }

    private void addMainSummary(LinearLayout body) {
        LinearLayout modeRow = row();
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView modeTitle = label("보호 모드", 38, TEXT, true);
        modeRow.addView(modeTitle, new LinearLayout.LayoutParams(0, -2, 1));
        if (!controller.isRequested()) {
            TextView off = label("요청 꺼짐", 24, MUTED, false);
            off.setId(R.id.request_status);
            modeRow.addView(off);
            modeRow.addView(gap(dp(18)));
        }
        modeRow.addView(protectionSwitch());
        body.addView(modeRow, new LinearLayout.LayoutParams(-1, dp(72)));

        if (!controller.isRequested()) {
            body.addView(space(dp(28)));
            body.addView(label("보호 요청을 켜면 마지막 프리셋을 사용합니다.", 19, MUTED, false));
            return;
        }

        TextView requested = label("보호 요청됨", 47, TEXT, true);
        requested.setId(R.id.request_status);
        body.addView(requested);
        body.addView(space(dp(25)));
        body.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(25)));
        body.addView(label("선택한 프리셋", 22, MUTED, false));
        body.addView(space(dp(12)));
        LinearLayout preset = row();
        preset.setGravity(Gravity.CENTER_VERTICAL);
        preset.addView(label("✓", 32, ACCENT, true));
        preset.addView(gap(dp(15)));
        TextView presetName = label(presets.getSelected().getName(), 33, TEXT, true);
        presetName.setId(R.id.preset_name);
        preset.addView(presetName);
        body.addView(preset);
        body.addView(space(dp(27)));
        body.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(25)));
        body.addView(label("보호 적용 현황", 23, TEXT, false));
        body.addView(space(dp(14)));
        LinearLayout status = row();
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.addView(label(targetSummary(), 24, MUTED, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView details = action("상세 보기  ›", 23);
        details.setId(R.id.status_details);
        details.setOnClickListener(v -> { detailsOpen = true; render(); });
        status.addView(details);
        body.addView(status);
        body.addView(space(dp(18)));
        body.addView(label("요청 상태만 저장됩니다. 실제 보호 기능은 아직 연동되지 않았습니다.",
                16, MUTED, false));
    }

    private void addCompactSummary(LinearLayout body) {
        LinearLayout mode = row();
        mode.setGravity(Gravity.CENTER_VERTICAL);
        TextView requested = label("보호 요청됨", 26, TEXT, true);
        requested.setId(R.id.request_status);
        mode.addView(requested, new LinearLayout.LayoutParams(0, -2, 1));
        mode.addView(protectionSwitch());
        body.addView(mode, new LinearLayout.LayoutParams(-1, dp(70)));
        body.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(28)));
        body.addView(label("선택한 프리셋", 20, MUTED, false));
        body.addView(space(dp(10)));
        TextView preset = label("✓  " + presets.getSelected().getName(), 30, ACCENT, true);
        preset.setId(R.id.preset_name);
        body.addView(preset);
        body.addView(space(dp(28)));
        body.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(28)));
        body.addView(label("요약", 20, MUTED, false));
        body.addView(space(dp(10)));
        body.addView(label(targetSummary(), 23, TEXT, true));
        body.addView(space(dp(18)));
        body.addView(label("요청 상태만 저장됩니다.", 16, MUTED, false));
    }

    private LinearLayout detailsPanel() {
        LinearLayout panel = column();
        panel.setId(R.id.details_panel);
        panel.setPadding(dp(30), dp(35), dp(30), dp(30));
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(label("보호 적용 현황", 34, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = action("닫기", 21);
        close.setId(R.id.close_details);
        close.setOnClickListener(v -> { detailsOpen = false; render(); });
        header.addView(close);
        panel.addView(header, new LinearLayout.LayoutParams(-1, dp(67)));
        panel.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout table = column();
        table.setPadding(0, dp(15), 0, 0);
        scroll.addView(table, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout headings = row();
        headings.setGravity(Gravity.CENTER_VERTICAL);
        headings.addView(label("보호 대상", 19, MUTED, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        headings.addView(label("상태", 19, MUTED, false));
        table.addView(headings, new LinearLayout.LayoutParams(-1, dp(55)));
        table.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
        List<String> targets = requestedTargets();
        if (targets.isEmpty()) {
            table.addView(label("선택한 보호 대상이 없습니다.", 21, MUTED, false));
        }
        for (String target : targets) addStatusRow(table, target);
        table.addView(space(dp(24)));
        table.addView(label("실제 보호 기능과 차량 시스템은 아직 연동되지 않았습니다.",
                16, MUTED, false));
        table.addView(space(dp(18)));
        table.addView(label("프라이버시 점검 준비 중", 17, MUTED, false));
        return panel;
    }

    private void addStatusRow(LinearLayout parent, String target) {
        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(label(target, 23, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(label("연동 전", 21, MUTED, true));
        parent.addView(item, new LinearLayout.LayoutParams(-1, dp(75)));
        parent.addView(divider(false), new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private List<String> requestedTargets() {
        Preset selected = presets.getSelected();
        List<String> targets = new ArrayList<>();
        if (selected.getBoolean("protect_screen", true)) targets.add("기본 화면");
        if (selected.getBoolean("protect_contact", true)) targets.add("연락처");
        if (selected.getBoolean("protect_notifications", true)) targets.add("알림 미리보기");
        if (selected.getBoolean("audio_call", false)) targets.add("통화 배경 음성");
        if (selected.getBoolean("audio_media", false)) targets.add("미디어 보호");
        return targets;
    }

    private String targetSummary() {
        int count = requestedTargets().size();
        return count == 0 ? "선택한 보호 대상 없음" : count + "개 대상 연동 전";
    }

    private Switch protectionSwitch() {
        Switch control = new Switch(this);
        control.setId(R.id.protection_switch);
        control.setContentDescription("보호 모드 요청");
        control.setMinWidth(dp(64));
        control.setMinHeight(dp(48));
        control.setChecked(controller.isRequested());
        int[][] states = new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}};
        control.setTrackTintList(new ColorStateList(states, new int[] {ACCENT, Color.rgb(78, 88, 96)}));
        control.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        control.setOnCheckedChangeListener((button, checked) -> {
            controller.setRequested(checked);
            preferences.edit().putBoolean(REQUEST_KEY, checked).apply();
            if (!checked) detailsOpen = false;
            render();
        });
        return control;
    }

    private TextView action(String text, int size) {
        TextView view = label(text, size, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size * scale);
        view.setIncludeFontPadding(true);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }

    private ImageView icon(int resource) {
        ImageView view = new ImageView(this);
        view.setImageResource(resource);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private View divider(boolean vertical) {
        View view = new View(this);
        view.setBackgroundColor(DIVIDER);
        return view;
    }

    private View colorBar() {
        View view = new View(this);
        view.setBackgroundColor(ACCENT);
        return view;
    }

    private View space(int height) {
        Space view = new Space(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return view;
    }

    private View gap(int width) {
        Space view = new Space(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return view;
    }

    private int dp(float value) {
        return Math.max(1, Math.round(value * scale * getResources().getDisplayMetrics().density));
    }
}
