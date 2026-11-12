package com.example.vehicleprivacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;
import java.util.Objects;

/** Edits a caller-owned draft only after confirmation; saving belongs to the host. */
public final class PrivacyRuleDraftView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int DIVIDER = Color.rgb(88, 99, 110);

    private final Activity activity;
    private final float scale;
    private final Preset draft;
    private final Runnable onSave;
    private final Runnable onCancel;
    private final LinearLayout root;
    private EditText input;
    private TextView status;
    private LinearLayout changes;
    private TextView confirm;
    private PrivacyRuleProposal.Result pending;
    private AlertDialog confirmationDialog;

    public PrivacyRuleDraftView(Activity activity, float scale, Preset preset,
                                Runnable onSave, Runnable onCancel) {
        this.activity = Objects.requireNonNull(activity);
        this.scale = scale;
        this.draft = Objects.requireNonNull(preset);
        this.onSave = Objects.requireNonNull(onSave);
        this.onCancel = Objects.requireNonNull(onCancel);
        root = new LinearLayout(activity) {
            @Override protected void onDetachedFromWindow() {
                clearSensitiveInput();
                super.onDetachedFromWindow();
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(25), dp(42), dp(30));
        root.setSaveEnabled(false);
        build();
    }

    public View getView() { return root; }

    /** The host must also call this on Activity pause. It never saves the draft. */
    public void clearSensitiveInput() {
        pending = null;
        if (confirmationDialog != null) {
            AlertDialog dialog = confirmationDialog;
            confirmationDialog = null;
            dialog.dismiss();
        }
        if (input != null && input.getText() != null) input.getText().clear();
        if (status != null) status.setText("");
        if (changes != null) changes.removeAllViews();
        if (confirm != null) confirm.setVisibility(View.GONE);
    }

    private void build() {
        TextView back = action("‹ 취소", 20);
        back.setContentDescription("규칙 제안을 취소하고 이전 화면으로 돌아가기");
        back.setOnClickListener(v -> { clearSensitiveInput(); onCancel.run(); });
        root.addView(back, new LinearLayout.LayoutParams(-2, dp(54)));

        TextView title = label("문장으로 설정 제안", 34, TEXT, true);
        title.setAccessibilityHeading(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(64)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout contents = column();
        contents.setPadding(0, dp(18), 0, dp(20));
        scroll.addView(contents, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        contents.addView(label("정해진 문장만 인식합니다. 음성 인식과 AI는 사용하지 않으며, 제안 확인 후 저장해야 설정이 보관됩니다.",
                17, MUTED, false));
        TextView examples = label("사용 가능한 문장\n• 알림 미리보기를 가려줘\n• 알림을 보관해줘\n• 연락처 사진을 가려줘\n• 연락처 애칭을 가려줘\n• 기본 화면을 가려줘",
                16, MUTED, false);
        examples.setPadding(0, dp(15), 0, dp(10));
        contents.addView(examples);

        input = new EditText(activity);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setTextSize(19 * scale);
        input.setHint("위 문장 중 하나를 입력해 주세요");
        input.setContentDescription("설정 제안 문장 입력");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(PrivacyRuleProposal.MAX_INPUT_CHARS) });
        input.setSaveEnabled(false);
        input.setMinHeight(dp(56));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable value) {
                pending = null;
                if (changes != null) changes.removeAllViews();
                if (confirm != null) confirm.setVisibility(View.GONE);
                if (status != null) status.setText("");
            }
        });
        contents.addView(input, new LinearLayout.LayoutParams(-1, -2));

        TextView propose = action("제안 만들기", 19);
        propose.setContentDescription("입력 문장의 설정 변경 제안 만들기");
        propose.setOnClickListener(v -> showProposal(PrivacyRuleProposal.parse(input.getText().toString())));
        contents.addView(propose, new LinearLayout.LayoutParams(-2, dp(58)));
        contents.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        status = label("", 19, TEXT, true);
        status.setPadding(0, dp(16), 0, dp(10));
        contents.addView(status);
        changes = column();
        contents.addView(changes);
        confirm = action("제안 확인", 19);
        confirm.setContentDescription("표시된 제안 확인 후 편집 중인 프리셋에 반영");
        confirm.setVisibility(View.GONE);
        confirm.setOnClickListener(v -> confirmProposal());
        contents.addView(confirm, new LinearLayout.LayoutParams(-2, dp(58)));
        TextView note = label("설정값만 편집합니다. 실제 차량 보호 기능·음성 인식은 연동되지 않았습니다. 입력한 문장은 앱에 저장하거나 전송하지 않습니다.",
                16, MUTED, false);
        note.setPadding(0, dp(15), 0, dp(15));
        contents.addView(note);

        LinearLayout buttons = row();
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView cancel = action("취소", 19);
        cancel.setOnClickListener(v -> { clearSensitiveInput(); onCancel.run(); });
        buttons.addView(cancel, new LinearLayout.LayoutParams(dp(120), dp(56)));
        TextView save = action("저장", 19);
        save.setContentDescription("확인된 설정을 저장");
        save.setOnClickListener(v -> {
            if (pending != null || input.getText().length() > 0) {
                status.setText("제안을 확인하거나 문장을 지운 뒤 저장해 주세요.");
                return;
            }
            clearSensitiveInput();
            onSave.run();
        });
        buttons.addView(save, new LinearLayout.LayoutParams(dp(150), dp(56)));
        root.addView(buttons, new LinearLayout.LayoutParams(-1, dp(62)));
    }

    private void showProposal(PrivacyRuleProposal.Result result) {
        pending = null;
        changes.removeAllViews();
        confirm.setVisibility(View.GONE);
        status.setText(result.getDescription());
        if (result.getStatus() != PrivacyRuleProposal.Status.RECOGNIZED) return;
        pending = result;
        for (Map.Entry<String, String> entry : result.getChanges().entrySet()) {
            String key = entry.getKey();
            String current = draft.getString(key, defaultValue(key));
            String text = settingLabel(key) + ": " + displayValue(key, current)
                    + " → " + displayValue(key, entry.getValue());
            TextView row = label(text, 17, TEXT, false);
            row.setPadding(0, dp(6), 0, dp(6));
            changes.addView(row);
        }
        confirm.setVisibility(View.VISIBLE);
    }

    private void confirmProposal() {
        PrivacyRuleProposal.Result candidate = pending;
        if (candidate == null) return;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("설정 제안 확인")
                .setMessage("표시된 설정 변경을 편집 중인 프리셋에 반영할까요? 이후 저장을 눌러야 보관됩니다.")
                .setNegativeButton("계속 검토", null)
                .setPositiveButton("반영", (ignored, which) -> {
                    if (pending != candidate || !root.isAttachedToWindow()) return;
                    candidate.applyTo(draft);
                    pending = null;
                    changes.removeAllViews();
                    confirm.setVisibility(View.GONE);
                    input.getText().clear();
                    status.setText("제안을 편집 중인 프리셋에 반영했습니다. 저장을 눌러 보관해 주세요.");
                }).create();
        confirmationDialog = dialog;
        dialog.setOnDismissListener(ignored -> { if (confirmationDialog == dialog) confirmationDialog = null; });
        dialog.show();
    }

    private String defaultValue(String key) { return "screen_mode".equals(key) ? "hide" : "true"; }
    private String displayValue(String key, String value) {
        if ("screen_mode".equals(key)) {
            if ("hide".equals(value)) return "가리기";
            if ("public".equals(value)) return "공개 가능한 표현";
            return "알 수 없음";
        }
        if ("true".equals(value)) return "켜짐";
        if ("false".equals(value)) return "꺼짐";
        return "알 수 없음";
    }
    private String settingLabel(String key) {
        switch (key) {
            case "protect_notifications": return "알림 보호";
            case "hide_notification_preview": return "알림 미리보기 가림";
            case "hold_notifications": return "보호 중 알림 보관";
            case "protect_contact": return "연락처 보호";
            case "hide_contact_photo": return "연락처 사진 가림";
            case "hide_contact_nickname": return "연락처 애칭 가림";
            case "protect_screen": return "기본 화면 보호";
            case "screen_mode": return "민감 정보 표시 방식";
            default: return "설정";
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
    private LinearLayout column() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.HORIZONTAL); return view; }
    private View divider() { View view = new View(activity); view.setBackgroundColor(DIVIDER); return view; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
