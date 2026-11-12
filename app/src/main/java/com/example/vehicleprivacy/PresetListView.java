package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.WeakHashMap;

/** Preset list; persistent state and rendering after edits belong to the activity. */
public final class PresetListView {
    public interface Listener {
        void onSelect(String id);
        void onSettings(String id);
        void onAdd(String name);
        void onRename(String id, String name);
        void onMove(String id, int newIndex);
        void onDelete(String id);
    }

    // Activity re-creates this view after a repository update. Keep edit mode across that redraw.
    private static final WeakHashMap<Activity, Boolean> EDIT_MODE = new WeakHashMap<>();
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int DIVIDER = Color.rgb(88, 99, 110);

    private final Activity activity;
    private final float scale;
    private final List<Preset> presets;
    private final String selectedId;
    private final Listener listener;
    private final LinearLayout root;

    public PresetListView(Activity activity, float scale, List<Preset> presets,
                          String selectedId, Listener listener) {
        this.activity = activity;
        this.scale = scale;
        this.presets = presets;
        this.selectedId = selectedId;
        this.listener = listener;
        root = column();
        root.setPadding(dp(42), dp(35), dp(42), dp(30));
        render();
    }

    public View getView() { return root; }

    private boolean isEditing() { return Boolean.TRUE.equals(EDIT_MODE.get(activity)); }

    private void render() {
        root.removeAllViews();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(label("프리셋", 40, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView edit = action(isEditing() ? "편집 완료" : "목록 편집");
        edit.setContentDescription(isEditing() ? "프리셋 목록 편집 완료" : "프리셋 목록 편집");
        edit.setOnClickListener(v -> { EDIT_MODE.put(activity, !isEditing()); render(); });
        header.addView(edit);
        TextView add = action("+ 추가");
        add.setContentDescription("새 프리셋 추가");
        add.setOnClickListener(v -> showNameDialog("새 프리셋", "", listener::onAdd));
        header.addView(add);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(78)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout entries = column();
        scroll.addView(entries, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (presets.isEmpty()) {
            TextView empty = label("프리셋을 추가해 주세요.", 22, MUTED, false);
            empty.setPadding(dp(18), dp(35), 0, 0);
            entries.addView(empty);
        }
        for (int i = 0; i < presets.size(); i++) {
            Preset preset = presets.get(i);
            entries.addView(presetRow(preset, i), new LinearLayout.LayoutParams(-1, dp(105)));
            entries.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private View presetRow(Preset preset, int index) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        boolean selected = preset.getId().equals(selectedId);
        TextView name = label((selected ? "✓  " : "") + preset.getName()
                + (selected ? "   선택됨" : ""), 27, selected ? ACCENT : TEXT, true);
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(dp(14), 0, dp(8), 0);
        name.setMinHeight(dp(48));
        name.setClickable(true);
        name.setFocusable(true);
        name.setContentDescription(preset.getName() + (selected ? ", 선택됨" : ", 선택"));
        name.setOnClickListener(v -> listener.onSelect(preset.getId()));
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));
        if (!isEditing()) {
            TextView settings = action("설정  ›");
            settings.setContentDescription(preset.getName() + " 설정");
            settings.setOnClickListener(v -> listener.onSettings(preset.getId()));
            row.addView(settings);
        } else {
            TextView rename = action("이름 변경");
            rename.setContentDescription(preset.getName() + " 이름 변경");
            rename.setOnClickListener(v -> showNameDialog("프리셋 이름 변경", preset.getName(),
                    value -> listener.onRename(preset.getId(), value)));
            row.addView(rename);
            TextView up = action("↑");
            up.setContentDescription(preset.getName() + " 위로 이동");
            up.setEnabled(index > 0);
            up.setTextColor(index > 0 ? ACCENT : MUTED);
            up.setOnClickListener(v -> listener.onMove(preset.getId(), index - 1));
            row.addView(up);
            TextView down = action("↓");
            down.setContentDescription(preset.getName() + " 아래로 이동");
            down.setEnabled(index < presets.size() - 1);
            down.setTextColor(index < presets.size() - 1 ? ACCENT : MUTED);
            down.setOnClickListener(v -> listener.onMove(preset.getId(), index + 1));
            row.addView(down);
            TextView delete = action("삭제");
            delete.setContentDescription(preset.getName() + " 삭제");
            delete.setOnClickListener(v -> confirmDelete(preset));
            row.addView(delete);
        }
        return row;
    }

    private void confirmDelete(Preset preset) {
        if (presets.size() <= 1) {
            new AlertDialog.Builder(activity).setTitle("프리셋 삭제")
                    .setMessage("프리셋은 하나 이상 남겨야 합니다.")
                    .setPositiveButton("확인", null).show();
            return;
        }
        new AlertDialog.Builder(activity).setTitle("프리셋 삭제")
                .setMessage("‘" + preset.getName() + "’ 프리셋을 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> listener.onDelete(preset.getId())).show();
    }

    private interface NameCallback { void accept(String name); }

    private void showNameDialog(String title, String initial, NameCallback callback) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(60) });
        input.setText(initial);
        input.setSelectAllOnFocus(true);
        input.setHint("프리셋 이름");
        input.setContentDescription("프리셋 이름 입력");
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title).setView(input)
                .setNegativeButton("취소", null).setPositiveButton("확인", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("이름을 입력해 주세요.");
                    } else {
                        callback.accept(name);
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private TextView action(String value) {
        TextView view = label(value, 21, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setMinHeight(dp(48));
        view.setMinWidth(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size * scale);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.VERTICAL);
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
