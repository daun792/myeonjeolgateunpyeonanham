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

/** An explicit simulation using this app's own generated tone, never external media. */
public final class OwnedMediaDemoView {
    private static final int TEXT = Color.rgb(246, 249, 251);
    private static final int MUTED = Color.rgb(160, 172, 184);
    private static final int ACCENT = Color.rgb(85, 221, 244);
    private static final int LINE = Color.rgb(88, 99, 110);

    private final Activity activity;
    private final float scale;
    private final Runnable onBack;
    private final OwnedMediaPolicy policy;
    private final OwnedAudioPlayer player;
    private final LinearLayout root;
    private TextView status;
    private TextView play;
    private TextView mute;
    private TextView result;
    private boolean closed;

    public OwnedMediaDemoView(Activity activity, float scale, Preset preset, Runnable onBack) {
        this.activity = activity;
        this.scale = scale;
        this.onBack = onBack;
        this.policy = new OwnedMediaPolicy(preset);
        this.player = new OwnedAudioPlayer(activity, () -> {
            if (closed) return;
            policy.userPause();
            result.setText("다른 오디오 요청으로 샘플 재생이 일시정지되었습니다.");
            update();
        });
        root = new LinearLayout(activity) {
            @Override protected void onDetachedFromWindow() {
                close();
                super.onDetachedFromWindow();
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(36), dp(25), dp(36), dp(24));
        build(preset.getName());
        update();
    }

    public View getView() { return root; }

    /** Stops and releases the app-owned sample when the view is left or backgrounded. */
    public void close() {
        if (closed) return;
        closed = true;
        policy.userPause();
        player.close();
        update();
    }

    private void build(String presetName) {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹ 홈");
        back.setContentDescription("미디어 연습을 종료하고 홈으로 돌아가기");
        back.setOnClickListener(v -> { close(); onBack.run(); });
        header.addView(back);
        TextView title = label("앱 자체 미디어 연습", 32, TEXT, true);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        root.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout body = column();
        body.setPadding(0, dp(20), 0, dp(18));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(label("앱에서 생성한 샘플 소리만 제어합니다. 실제 차량이나 다른 앱과 연결되지 않았습니다.",
                17, MUTED, false));
        body.addView(space(dp(20)));
        body.addView(label("선택한 프리셋: " + presetName, 22, TEXT, true));
        body.addView(space(dp(14)));
        status = label("", 22, ACCENT, true);
        body.addView(status);
        body.addView(space(dp(14)));

        LinearLayout controls = row();
        play = action("");
        play.setOnClickListener(v -> togglePlayback());
        controls.addView(play, new LinearLayout.LayoutParams(0, dp(58), 1));
        mute = action("");
        mute.setOnClickListener(v -> toggleMute());
        LinearLayout.LayoutParams muteParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        muteParams.leftMargin = dp(14);
        controls.addView(mute, muteParams);
        body.addView(controls);
        body.addView(space(dp(25)));
        body.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        body.addView(space(dp(20)));
        body.addView(label("모의 이벤트", 24, TEXT, true));
        body.addView(label("버튼을 누를 때만 프리셋 규칙을 시험합니다.", 16, MUTED, false));
        body.addView(space(dp(14)));
        addEvent(body, "모의 문 열림", OwnedMediaPolicy.Event.DOOR);
        body.addView(space(dp(10)));
        addEvent(body, "모의 장소 도착", OwnedMediaPolicy.Event.LOCATION);
        body.addView(space(dp(18)));
        result = label("모의 이벤트를 선택해 주세요.", 17, MUTED, false);
        body.addView(result);
    }

    private void addEvent(LinearLayout parent, String name, OwnedMediaPolicy.Event event) {
        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);
        String rule = policy.isEnabled(event) ? policy.actionLabel(event) : "규칙 꺼짐";
        line.addView(label(rule, 18, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView button = action(name);
        button.setContentDescription(name + " 시험, " + rule);
        button.setOnClickListener(v -> trigger(event, name));
        line.addView(button);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(64)));
        parent.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void togglePlayback() {
        if (closed) return;
        if (policy.isPlaying()) {
            player.pause();
            policy.userPause();
            result.setText("앱 샘플 소리를 일시정지했습니다.");
        } else if (player.play(policy.isMuted())) {
            policy.userPlay();
            result.setText("앱 샘플 소리를 재생하고 있습니다.");
        } else {
            policy.userPause();
            result.setText("샘플 소리를 재생할 수 없습니다. 오디오 상태를 확인해 주세요.");
        }
        update();
    }

    private void toggleMute() {
        if (closed) return;
        boolean next = !policy.isMuted();
        if (player.setMuted(next)) {
            policy.userSetMuted(next);
            result.setText(next ? "앱 샘플 소리만 음소거했습니다." : "앱 샘플 소리의 음소거를 해제했습니다.");
        } else {
            player.pause();
            policy.userPause();
            result.setText("음소거를 변경할 수 없어 샘플 소리를 일시정지했습니다.");
        }
        update();
    }

    private void trigger(OwnedMediaPolicy.Event event, String name) {
        if (closed) return;
        OwnedMediaPolicy.Result outcome = policy.trigger(event);
        if (outcome == OwnedMediaPolicy.Result.DISABLED) {
            result.setText(name + ": 프리셋의 해당 자동 실행 규칙이 꺼져 있습니다.");
        } else if (outcome == OwnedMediaPolicy.Result.PAUSED) {
            player.pause();
            result.setText(name + ": 앱 샘플 소리를 일시정지했습니다.");
        } else if (player.setMuted(true)) {
            result.setText(name + ": 앱 샘플 소리만 음소거했습니다.");
        } else {
            player.pause();
            policy.userPause();
            result.setText(name + ": 음소거를 적용할 수 없어 샘플 소리를 일시정지했습니다.");
        }
        update();
    }

    private void update() {
        if (status == null) return;
        status.setText((policy.isPlaying() ? "샘플 재생 중" : "샘플 일시정지")
                + (policy.isMuted() ? " · 음소거" : " · 소리 켜짐"));
        play.setText(policy.isPlaying() ? "샘플 일시정지" : "샘플 소리 재생");
        play.setContentDescription(policy.isPlaying() ? "앱 샘플 소리 일시정지" : "앱 샘플 소리 재생");
        mute.setText(policy.isMuted() ? "음소거 해제" : "샘플 음소거");
    }

    private TextView action(String text) {
        TextView view = label(text, 20, ACCENT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.TRANSPARENT);
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), ACCENT);
        view.setBackground(shape);
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
    private View space(int height) { View v = new View(activity); v.setLayoutParams(new LinearLayout.LayoutParams(1, height)); return v; }
    private int dp(float value) { return Math.max(1, Math.round(value * scale * activity.getResources().getDisplayMetrics().density)); }
}
