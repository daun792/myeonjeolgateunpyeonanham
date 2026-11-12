package com.example.vehicleprivacy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Local pattern check only. It does not speak, persist or transmit the input. */
public final class UtteranceCheckView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int DIVIDER = Color.rgb(88, 99, 110);
    private final Activity activity;
    private final float scale;
    private final Runnable onBack;
    private final SensitiveUtteranceGuard guard = new SensitiveUtteranceGuard();
    private final LinearLayout root;
    private EditText input;
    private TextView result;
    private TextView kinds;

    public UtteranceCheckView(Activity activity, float scale, Runnable onBack) {
        this.activity = activity;
        this.scale = scale;
        this.onBack = onBack;
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

    /** The host must also call this on Activity pause, not only on view removal. */
    public void clearSensitiveInput() {
        if (input != null && input.getText() != null) input.getText().clear();
        if (result != null) result.setText("");
        if (kinds != null) kinds.setText("");
    }

    private void build() {
        TextView back = action("‹ 뒤로", 20);
        back.setContentDescription("문장 점검을 닫고 이전 화면으로 돌아가기");
        back.setOnClickListener(v -> { clearSensitiveInput(); onBack.run(); });
        root.addView(back, new LinearLayout.LayoutParams(-2, dp(54)));

        TextView title = label("발화 전 문장 점검", 34, TEXT, true);
        title.setAccessibilityHeading(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(64)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout contents = column();
        contents.setPadding(0, dp(18), 0, dp(20));
        scroll.addView(contents, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView explanation = label("최대 2,000자에서 휴대전화 번호와 이메일의 일부 형식만 확인하는 예시입니다. 모든 개인정보를 탐지하거나 문장의 안전성을 보장하지 않습니다.",
                17, MUTED, false);
        contents.addView(explanation);
        TextView inputLabel = label("검사할 문장", 21, TEXT, true);
        inputLabel.setPadding(0, dp(22), 0, dp(8));
        contents.addView(inputLabel);

        input = new EditText(activity);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setTextSize(19 * scale);
        input.setHint("예시 문장을 입력해 주세요");
        input.setContentDescription("검사할 문장 입력");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SensitiveUtteranceGuard.MAX_INPUT_CHARS) });
        input.setSaveEnabled(false);
        input.setMinHeight(dp(100));
        input.setGravity(Gravity.TOP | Gravity.START);
        contents.addView(input, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = row();
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView clear = action("지우기", 19);
        clear.setOnClickListener(v -> clearSensitiveInput());
        actions.addView(clear, new LinearLayout.LayoutParams(dp(120), dp(56)));
        TextView check = action("문장 검사", 19);
        check.setContentDescription("입력한 문장 검사");
        check.setOnClickListener(v -> showResult(guard.inspect(input.getText().toString())));
        actions.addView(check, new LinearLayout.LayoutParams(dp(150), dp(56)));
        contents.addView(actions);
        contents.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        result = label("", 19, TEXT, true);
        result.setPadding(0, dp(18), 0, dp(6));
        contents.addView(result);
        kinds = label("", 17, ACCENT, false);
        kinds.setPadding(0, dp(4), 0, dp(12));
        contents.addView(kinds);
        TextView footer = label("앱 내부에서만 검사하며 문장을 저장하거나 전송하지 않습니다. AI나 음성 읽기 기능은 연결되지 않았습니다.",
                16, MUTED, false);
        footer.setPadding(0, dp(14), 0, 0);
        contents.addView(footer);
    }

    private void showResult(SensitiveUtteranceGuard.Result checked) {
        result.setText(checked.getPublicSummary());
        if (checked.getStatus() != SensitiveUtteranceGuard.Status.PATTERN_FOUND) {
            kinds.setText("");
            return;
        }
        String matched = checked.hasPhone() && checked.hasEmail() ? "휴대전화 번호, 이메일"
                : checked.hasPhone() ? "휴대전화 번호" : "이메일";
        kinds.setText("확인된 형식: " + matched);
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
