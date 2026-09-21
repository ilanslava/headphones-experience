package com.example.headphonesexperience;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.audiofx.BassBoost;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.os.IBinder;

public class AudioService extends Service {
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_EFFECT = "EFFECT";
    public static final String EXTRA_EFFECT = "effect";

    private static final int NOTIFICATION_ID = 7;
    private static final String CHANNEL_ID = "live_audio";

    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private PresetReverb reverb;
    private LoudnessEnhancer loudness;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            releaseEffects();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_EFFECT.equals(action)) {
            applyEffect(intent.getIntExtra(EXTRA_EFFECT, 0));
            return START_STICKY;
        }

        if (ACTION_START.equals(action)) {
            startForeground(NOTIFICATION_ID, notification());
            if (bassBoost == null) createEffects();
        }

        return START_STICKY;
    }

    private void createEffects() {
        try {
            // Session 0 is the Android global output mix. Android documents this
            // route as deprecated for insert effects, but it is still exposed by
            // the public AudioEffect API and is worth testing on the target Galaxy.
            final int GLOBAL_OUTPUT_SESSION = 0;

            bassBoost = new BassBoost(1000, GLOBAL_OUTPUT_SESSION);
            virtualizer = new Virtualizer(1000, GLOBAL_OUTPUT_SESSION);
            reverb = new PresetReverb(1000, GLOBAL_OUTPUT_SESSION);
            loudness = new LoudnessEnhancer(GLOBAL_OUTPUT_SESSION);

            disableAll();
        } catch (Throwable t) {
            releaseEffects();
        }
    }

    private void applyEffect(int effect) {
        if (bassBoost == null) createEffects();
        if (bassBoost == null) return;

        try {
            disableAll();

            switch (effect) {
                case 1: // SPATIAL
                    virtualizer.setStrength((short) 800);
                    virtualizer.setEnabled(true);
                    break;

                case 2: // REVERB
                    reverb.setPreset(PresetReverb.PRESET_LARGEHALL);
                    reverb.setEnabled(true);
                    break;

                case 3: // 8D approximation using the platform spatializer
                    virtualizer.setStrength((short) 1000);
                    virtualizer.setEnabled(true);
                    break;

                case 4: // LOUD / CLEAN
                    loudness.setTargetGain(450);
                    loudness.setEnabled(true);
                    break;

                default: // ORIGINAL
                    break;
            }
        } catch (Throwable ignored) {
        }
    }

    private void disableAll() {
        try { if (bassBoost != null) bassBoost.setEnabled(false); } catch (Throwable ignored) {}
        try { if (virtualizer != null) virtualizer.setEnabled(false); } catch (Throwable ignored) {}
        try { if (reverb != null) reverb.setEnabled(false); } catch (Throwable ignored) {}
        try { if (loudness != null) loudness.setEnabled(false); } catch (Throwable ignored) {}
    }

    private void releaseEffects() {
        disableAll();
        try { if (bassBoost != null) bassBoost.release(); } catch (Throwable ignored) {}
        try { if (virtualizer != null) virtualizer.release(); } catch (Throwable ignored) {}
        try { if (reverb != null) reverb.release(); } catch (Throwable ignored) {}
        try { if (loudness != null) loudness.release(); } catch (Throwable ignored) {}
        bassBoost = null;
        virtualizer = null;
        reverb = null;
        loudness = null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Live Audio", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification notification() {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Experience")
                    .setContentText("Audio effects are active")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("Experience")
                .setContentText("Audio effects are active")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        releaseEffects();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
