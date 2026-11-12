package com.example.vehicleprivacy;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

/** Plays only an in-memory example tone; no other app or system volume is controlled. */
final class OwnedAudioPlayer {
    private static final int SAMPLE_RATE = 16000;
    private final AudioManager audioManager;
    private final AudioFocusRequest focusRequest;
    private final Runnable onInterrupted;
    private AudioTrack track;
    private boolean hasFocus;
    private boolean closed;

    OwnedAudioPlayer(Context context, Runnable onInterrupted) {
        this.onInterrupted = onInterrupted;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(change -> {
                    if (!closed && change < 0) {
                        pause();
                        this.onInterrupted.run();
                    }
                }, new Handler(Looper.getMainLooper())).build();
    }

    boolean play(boolean muted) {
        if (closed || audioManager == null) return false;
        try {
            if (!hasFocus) {
                if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    return false;
                }
                hasFocus = true;
            }
            if (track == null) track = createTone();
            if (track == null || track.setVolume(muted ? 0f : 1f) != AudioTrack.SUCCESS) {
                abandonFocus();
                releaseTrack();
                return false;
            }
            track.play();
            return true;
        } catch (RuntimeException error) {
            abandonFocus();
            releaseTrack();
            return false;
        }
    }

    boolean setMuted(boolean muted) {
        if (closed) return false;
        if (track == null) return true;
        try { return track.setVolume(muted ? 0f : 1f) == AudioTrack.SUCCESS; }
        catch (RuntimeException error) { return false; }
    }

    void pause() {
        try {
            if (track != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) track.pause();
        } catch (RuntimeException ignored) { releaseTrack(); }
        abandonFocus();
    }

    void close() {
        if (closed) return;
        closed = true;
        pause();
        releaseTrack();
    }

    private void abandonFocus() {
        if (!hasFocus || audioManager == null) return;
        try { audioManager.abandonAudioFocusRequest(focusRequest); }
        catch (RuntimeException ignored) { }
        hasFocus = false;
    }

    private void releaseTrack() {
        AudioTrack previous = track;
        track = null;
        if (previous != null) {
            try { previous.release(); } catch (RuntimeException ignored) { }
        }
    }

    private AudioTrack createTone() {
        // A short soft beep followed by silence makes the demo distinguishable from real media.
        short[] pcm = new short[SAMPLE_RATE];
        for (int i = 0; i < SAMPLE_RATE / 3; i++) {
            double envelope = Math.sin(Math.PI * i / (SAMPLE_RATE / 3.0));
            pcm[i] = (short) (3000 * envelope * Math.sin(2 * Math.PI * 440 * i / SAMPLE_RATE));
        }
        AudioTrack created = null;
        try {
            created = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
            if (created.getState() != AudioTrack.STATE_INITIALIZED
                    || created.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING) != pcm.length
                    || created.setLoopPoints(0, pcm.length, -1) != AudioTrack.SUCCESS) {
                created.release();
                return null;
            }
            return created;
        } catch (RuntimeException error) {
            if (created != null) created.release();
            return null;
        }
    }
}
