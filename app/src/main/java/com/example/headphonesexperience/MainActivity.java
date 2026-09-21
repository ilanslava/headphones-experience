package com.example.headphonesexperience;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 42;
    private static final int SAMPLE_RATE = 48000;
    private LinearLayout root;
    private MediaProjection projection;
    private AudioRecord recorder;
    private AudioTrack player;
    private Thread audioThread;
    private volatile boolean running = false;
    private volatile int effect = 0;
    private double phase = 0.0;
    private final float[] delayL = new float[SAMPLE_RATE];
    private final float[] delayR = new float[SAMPLE_RATE];
    private int delayIndex = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private TextView text(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, 14, 0, 14);
        return v;
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(8, 8, 8));
        setContentView(root);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        root.addView(b);
        return b;
    }

    private void showHome() {
        base();
        root.addView(text("LIVE AUDIO EXPERIMENT", 28));
        TextView sub = text("Play a podcast or music in another app.\nThen let this app process it.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);
        Button start = button("START AUDIO CAPTURE");
        start.setOnClickListener(v -> requestCapture());
        TextView note = text("Android will ask for permission to capture playback audio.", 13);
        note.setTextColor(Color.GRAY);
        root.addView(note);
    }

    private void showControls() {
        base();
        root.addView(text("LIVE AUDIO", 30));
        TextView status = text("Processing audio from another app.", 16);
        status.setTextColor(Color.LTGRAY);
        root.addView(status);

        Button original = button("ORIGINAL");
        original.setOnClickListener(v -> effect = 0);
        Button spatial = button("SPATIAL");
        spatial.setOnClickListener(v -> effect = 1);
        Button reverb = button("REVERB");
        reverb.setOnClickListener(v -> effect = 2);
        Button eightD = button("8D");
        eightD.setOnClickListener(v -> effect = 3);
        Button loud = button("LOUD / CLEAN");
        loud.setOnClickListener(v -> effect = 4);
        Button stop = button("STOP");
        stop.setOnClickListener(v -> stopCapture());
    }

    private void requestCapture() {
        if (Build.VERSION.SDK_INT < 29) return;
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, data);
            startCapture();
            showControls();
        }
    }

    private void startCapture() {
        if (projection == null || running) return;
        try {
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
            audioThread = new Thread(this::processLoop, "AudioDSP");
            audioThread.start();
        } catch (Exception e) {
            stopCapture();
            showHome();
        }
    }

    private void processLoop() {
        short[] pcm = new short[1024 * 2];
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
                    float dl = delayL[delayedIndex];
                    float dr = delayR[delayedIndex];
                    outL = left + dr * 0.32f;
                    outR = right + dl * 0.32f;
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

            if (player != null) player.write(pcm, 0, read);
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

    @Override protected void onDestroy() {
        stopCapture();
        super.onDestroy();
    }
}
