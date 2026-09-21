package com.example.headphonesexperience;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

public class AudioService extends Service {
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_EFFECT = "EFFECT";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_EFFECT = "effect";

    private static final int NOTIFICATION_ID = 7;
    private static final String CHANNEL_ID = "live_audio";
    private static final int SAMPLE_RATE = 48000;

    private MediaProjection projection;
    private AudioRecord recorder;
    private AudioTrack player;
    private Thread audioThread;
    private volatile boolean running;
    private volatile int effect;
    private double phase;
    private final float[] delayL = new float[SAMPLE_RATE];
    private final float[] delayR = new float[SAMPLE_RATE];
    private int delayIndex;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopCapture();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_EFFECT.equals(action)) {
            effect = intent.getIntExtra(EXTRA_EFFECT, 0);
            return START_STICKY;
        }

        if (ACTION_START.equals(action) && !running) {
            startForeground(NOTIFICATION_ID, notification());
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            if (data != null) startCapture(resultCode, data);
        }
        return START_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT < 29) return;
        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, data);

            AudioPlaybackCaptureConfiguration config =
                    new AudioPlaybackCaptureConfiguration.Builder(projection)
                            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                            .addMatchingUsage(AudioAttributes.USAGE_GAME)
                            .build();

            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            int min = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(min * 2, SAMPLE_RATE / 2 * 4);

            recorder = new AudioRecord.Builder()
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferBytes)
                    .setAudioPlaybackCaptureConfig(config)
                    .build();

            player = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build())
                    .setBufferSizeInBytes(Math.max(bufferBytes, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            running = true;
            recorder.startRecording();
            player.play();
            audioThread = new Thread(this::processLoop, "LiveAudioDSP");
            audioThread.start();
        } catch (Exception e) {
            stopCapture();
        }
    }

    private void processLoop() {
        short[] pcm = new short[2048];
        while (running && recorder != null && player != null) {
            int read = recorder.read(pcm, 0, pcm.length, AudioRecord.READ_BLOCKING);
            if (read <= 0) continue;

            for (int i = 0; i + 1 < read; i += 2) {
                float left = pcm[i] / 32768f;
                float right = pcm[i + 1] / 32768f;
                float outL = left;
                float outR = right;

                if (effect == 1) {
                    float mid = (left + right) * 0.5f;
                    float side = (left - right) * 0.5f * 1.65f;
                    outL = mid + side;
                    outR = mid - side;
                } else if (effect == 2) {
                    int d = (int)(0.11f * SAMPLE_RATE);
                    int delayedIndex = (delayIndex - d + delayL.length) % delayL.length;
                    outL = left + delayR[delayedIndex] * 0.32f;
                    outR = right + delayL[delayedIndex] * 0.32f;
                } else if (effect == 3) {
                    double pan = Math.sin(phase);
                    float lGain = (float)Math.sqrt((1.0 - pan) * 0.5);
                    float rGain = (float)Math.sqrt((1.0 + pan) * 0.5);
                    float mono = (left + right) * 0.5f;
                    outL = mono * lGain * 1.35f;
                    outR = mono * rGain * 1.35f;
                } else if (effect == 4) {
                    outL = softLimit(left * 2.4f);
                    outR = softLimit(right * 2.4f);
                }

                if (effect != 4) {
                    outL = softLimit(outL);
                    outR = softLimit(outR);
                }

                delayL[delayIndex] = left;
                delayR[delayIndex] = right;
                delayIndex = (delayIndex + 1) % delayL.length;

                phase += 2.0 * Math.PI * 0.10 / SAMPLE_RATE;
                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;

                pcm[i] = (short)(clamp(outL) * 32767);
                pcm[i + 1] = (short)(clamp(outR) * 32767);
            }
            player.write(pcm, 0, read);
        }
    }

    private float softLimit(float x) {
        return (float)(Math.tanh(x * 1.25) / Math.tanh(1.25));
    }

    private float clamp(float x) {
        return Math.max(-1f, Math.min(1f, x));
    }

    private void stopCapture() {
        running = false;
        try { if (recorder != null) recorder.stop(); } catch (Exception ignored) {}
        try { if (player != null) player.stop(); } catch (Exception ignored) {}
        if (recorder != null) recorder.release();
        if (player != null) player.release();
        recorder = null;
        player = null;
        if (projection != null) {
            projection.stop();
            projection = null;
        }
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
                    .setContentText("Live audio processing is active")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("Experience")
                .setContentText("Live audio processing is active")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
