package com.example.vehicleprivacy;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Example-only vehicle home simulation. No real vehicle or notification data is used. */
public final class VehiclePreviewView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int DIVIDER = Color.rgb(88, 99, 110);
    private static final TemporaryReveal.Area[] AREAS = {
            TemporaryReveal.Area.CONTACT_NAME, TemporaryReveal.Area.CONTACT_PHOTO,
            TemporaryReveal.Area.NOTIFICATION, TemporaryReveal.Area.SCREEN_PHOTO
    };
    private static final String[] LABELS = {"연락처 이름", "연락처 사진 · 애칭", "알림 내용", "기본 화면 사진"};
    private static final String[] EXAMPLES = {"예시 이름", "예시 사진 · 애칭", "예시 알림 내용", "예시 사진"};

    private final Activity activity;
    private final float scale;
    private final TemporaryReveal model;
    private final PhotoPreviewPolicy photoPolicy;
    private final Runnable onBack;
    private final TextView[] rows = new TextView[AREAS.length];
    private final TextView[] photoSymbols = new TextView[ExamplePhotoApprovals.Photo.values().length];
    private final TextView[] photoLabels = new TextView[ExamplePhotoApprovals.Photo.values().length];
    private final LinearLayout root;
    private TextView holdButton;
    private boolean gestureCancelled;

    public VehiclePreviewView(Activity activity, float scale, TemporaryReveal model,
                              PhotoPreviewPolicy photoPolicy, Runnable onBack) {
        this.activity = activity;
        this.scale = scale;
        this.model = model;
        this.photoPolicy = java.util.Objects.requireNonNull(photoPolicy, "photoPolicy");
        this.onBack = onBack;
        root = new LinearLayout(activity) {
            @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
                super.onWindowFocusChanged(hasWindowFocus);
                if (!hasWindowFocus) cancelHold();
            }
            @Override protected void onDetachedFromWindow() {
                cancelHold();
                super.onDetachedFromWindow();
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(22), dp(32), dp(24));
        build();
        updateLabels();
    }

    public View getView() { return root; }

    /** Called on lifecycle changes and navigation, as well as cancelled gestures. */
    public void cancelHold() {
        model.cancelHold();
        gestureCancelled = true;
        updateLabels();
    }

    private void build() {
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = label("‹ 홈", 20, ACCENT, true);
        back.setContentDescription("시뮬레이션 종료하고 홈으로 돌아가기");
        back.setGravity(Gravity.CENTER);
        back.setMinWidth(dp(80));
        back.setMinHeight(dp(48));
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> { cancelHold(); onBack.run(); });
        heading.addView(back);
        TextView title = label("차량 기본 화면 시뮬레이션", 30, TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(15);
        heading.addView(title, titleParams);
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(65)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        TextView note = label("예시 화면 · 실제 차량 기본 화면과는 연결되지 않았습니다", 15, MUTED, false);
        note.setPadding(0, dp(9), 0, dp(9));
        root.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = row();
        body.setPadding(0, dp(12), 0, dp(12));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        FrameLayout map = new FrameLayout(activity);
        map.setBackground(rounded(Color.rgb(19, 32, 42), Color.rgb(63, 90, 104)));
        map.setContentDescription("예시 지도, 300미터 앞 우회전. 안전 정보는 항상 표시됩니다.");
        map.setFocusable(true);
        RoadMap mapArtwork = new RoadMap(activity);
        mapArtwork.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        map.addView(mapArtwork, new FrameLayout.LayoutParams(-1, -1));
        TextView turn = label("↱   300 m  우회전\n      안전 정보 예시", 22, TEXT, true);
        turn.setBackground(rounded(Color.rgb(25, 52, 63), Color.rgb(63, 90, 104)));
        turn.setPadding(dp(15), dp(12), dp(15), dp(12));
        turn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams turnParams = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        turnParams.setMargins(dp(12), dp(12), dp(12), 0);
        map.addView(turn, turnParams);
        body.addView(map, new LinearLayout.LayoutParams(0, dp(430), 1));

        LinearLayout right = column();
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1);
        rightParams.leftMargin = dp(24);
        body.addView(right, rightParams);
        holdButton = label("◉   잠시 보기\n      누르고 있는 동안만 공개", 23, TEXT, true);
        holdButton.setGravity(Gravity.CENTER);
        holdButton.setBackground(rounded(Color.TRANSPARENT, ACCENT));
        holdButton.setPadding(dp(12), dp(10), dp(12), dp(10));
        holdButton.setMinHeight(dp(90));
        holdButton.setClickable(true);
        holdButton.setFocusable(true);
        holdButton.setContentDescription("잠시 보기, 선택한 항목은 누르는 동안 공개하고 승인 사진의 상세만 잠시 표시");
        holdButton.setOnTouchListener(this::handleTouch);
        holdButton.setOnKeyListener(this::handleKey);
        holdButton.setOnFocusChangeListener((view, focused) -> { if (!focused) cancelHold(); });
        right.addView(holdButton, new LinearLayout.LayoutParams(-1, dp(104)));
        right.addView(gapHeight(dp(14)));
        for (int i = 0; i < AREAS.length; i++) {
            rows[i] = label("", 19, TEXT, true);
            rows[i].setGravity(Gravity.CENTER_VERTICAL);
            rows[i].setPadding(dp(4), dp(6), dp(4), dp(6));
            right.addView(rows[i], new LinearLayout.LayoutParams(-1, dp(68)));
            right.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        }
        boolean anySelected = false;
        for (TemporaryReveal.Area area : AREAS) anySelected |= model.isSelected(area);
        TextView help = label(anySelected
                ? "선택한 예시는 누르는 동안만 공개됩니다. 사진은 허용된 예시의 상세만 잠시 표시합니다."
                : "선택된 잠시 보기 범위가 없습니다. 사진은 아래 허용 목록을 따릅니다.",
                15, MUTED, false);
        help.setPadding(0, dp(15), 0, 0);
        right.addView(help);

        TextView photoHeading = label("앱의 가상 사진 예시", 20, ACCENT, true);
        photoHeading.setPadding(0, dp(18), 0, dp(8));
        right.addView(photoHeading);
        TextView photoSettings = label("선택 프리셋: " + photoPolicy.getPresetName()
                + " · 보호 요청 " + (photoPolicy.isProtectionRequested() ? "켜짐" : "꺼짐")
                + " · 기본 화면 보호 " + (photoPolicy.isScreenProtectionEnabled() ? "켜짐" : "꺼짐"),
                15, MUTED, false);
        right.addView(photoSettings);
        TextView photoHelp = label(photoPolicy.isProtectScreenRequested()
                ? "기본 화면 보호 요청 중에는 허용한 예시만 표시합니다. 사진 잠시 보기를 선택하면 허용된 사진의 상세를 확인할 수 있습니다."
                : "기본 화면 보호 요청이 꺼져 예시 사진을 표시합니다. 허용 목록에 오류가 있으면 가립니다.",
                15, MUTED, false);
        right.addView(photoHelp);
        right.addView(label("프리셋 설정 저장 후 이 화면에 다시 들어오면 반영됩니다.", 15, MUTED, false));
        for (ExamplePhotoApprovals.Photo photo : ExamplePhotoApprovals.Photo.values()) {
            int index = photo.ordinal();
            LinearLayout item = row();
            item.setGravity(Gravity.CENTER_VERTICAL);
            photoSymbols[index] = label("", 22, TEXT, true);
            photoSymbols[index].setGravity(Gravity.CENTER);
            photoSymbols[index].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(photoSymbols[index], new LinearLayout.LayoutParams(dp(48), dp(48)));
            photoLabels[index] = label("", 18, TEXT, true);
            photoLabels[index].setPadding(dp(12), 0, 0, 0);
            item.addView(photoLabels[index], new LinearLayout.LayoutParams(0, -2, 1));
            right.addView(item, new LinearLayout.LayoutParams(-1, dp(64)));
        }
    }

    private boolean handleTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureCancelled = false;
                view.getParent().requestDisallowInterceptTouchEvent(true);
                model.beginHold();
                updateLabels();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                cancelHold();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() != 1 || event.getX() < 0 || event.getY() < 0
                        || event.getX() >= view.getWidth() || event.getY() >= view.getHeight()) {
                    cancelHold();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!gestureCancelled) model.endHold();
                else model.cancelHold();
                gestureCancelled = false;
                view.getParent().requestDisallowInterceptTouchEvent(false);
                updateLabels();
                return true;
            default:
                return true;
        }
    }

    private boolean handleKey(View view, int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_DPAD_CENTER) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            model.beginHold();
            updateLabels();
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            model.endHold();
            updateLabels();
        }
        return true;
    }

    private void updateLabels() {
        for (int i = 0; i < AREAS.length; i++) {
            if (rows[i] == null) continue;
            String text = AREAS[i] == TemporaryReveal.Area.SCREEN_PHOTO
                    ? (photoPolicy.isProtectScreenRequested()
                        ? "기본 화면 사진: 아래 가상 사진 허용 목록 적용"
                        : "기본 화면 사진: 아래 가상 사진 표시 상태 확인")
                    : model.isRevealed(AREAS[i])
                    ? LABELS[i] + ": " + EXAMPLES[i] + " (예시)"
                    : LABELS[i] + " 가려짐";
            rows[i].setText(text);
            rows[i].setContentDescription(text);
        }
        for (ExamplePhotoApprovals.Photo photo : ExamplePhotoApprovals.Photo.values()) {
            int index = photo.ordinal();
            if (photoSymbols[index] == null || photoLabels[index] == null) continue;
            boolean visible = photoPolicy.isVisible(photo);
            photoSymbols[index].setText(visible ? photoSymbol(photo) : "■");
            photoSymbols[index].setBackground(rounded(visible ? photoColor(photo)
                    : Color.rgb(61, 71, 78), DIVIDER));
            String label = photoPolicy.label(photo, model);
            photoLabels[index].setText(label);
            photoLabels[index].setContentDescription(label);
        }
        if (holdButton != null) holdButton.setText(model.isHolding()
                ? "◉   잠시 보기 중\n      손을 떼면 선택한 예시를 다시 가립니다"
                : "◉   잠시 보기\n      누르고 있는 동안 선택한 예시 공개");
    }

    private static String photoSymbol(ExamplePhotoApprovals.Photo photo) {
        switch (photo) {
            case SKY: return "☀";
            case GARDEN: return "✿";
            default: return "●";
        }
    }

    private static int photoColor(ExamplePhotoApprovals.Photo photo) {
        switch (photo) {
            case SKY: return Color.rgb(51, 117, 145);
            case GARDEN: return Color.rgb(57, 111, 83);
            default: return Color.rgb(120, 85, 70);
        }
    }

    private GradientDrawable rounded(int color, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), stroke);
        return bg;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size * scale);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private View divider() { View v = new View(activity); v.setBackgroundColor(DIVIDER); return v; }
    private View gapHeight(int height) { View v = new View(activity); v.setLayoutParams(new LinearLayout.LayoutParams(1, height)); return v; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }

    private final class RoadMap extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RoadMap(Activity context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(5));
            paint.setColor(Color.rgb(49, 67, 82));
            for (int i = 1; i < 5; i++) {
                canvas.drawLine(0, h * i / 5f, w, h * (i + 0.4f) / 5f, paint);
                canvas.drawLine(w * i / 5f, 0, w * (i - 0.5f) / 5f, h, paint);
            }
            Path route = new Path();
            route.moveTo(w * .45f, h * .9f);
            route.lineTo(w * .45f, h * .55f);
            route.lineTo(w * .78f, h * .35f);
            route.lineTo(w * .88f, h * .05f);
            paint.setColor(ACCENT);
            paint.setStrokeWidth(dp(8));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(route, paint);
        }
    }
}
