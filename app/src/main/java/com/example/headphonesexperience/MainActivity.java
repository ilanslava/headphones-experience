package com.example.headphonesexperience;

import android.app.Activity;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private LinearLayout root;

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
        root.addView(text("PUT ON YOUR HEADPHONES", 28));
        TextView sub = text("Now let's actually play with sound.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        Button begin = button("ENTER AUDIO LAB");
        begin.setOnClickListener(v -> showLab());
    }

    private void showLab() {
        base();
        root.addView(text("AUDIO LAB", 34));
        TextView sub = text("Choose what you want to hear.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        Button spatial = button("SPATIAL");
        spatial.setOnClickListener(v -> playEffect(0));

        Button reverb = button("REVERB");
        reverb.setOnClickListener(v -> playEffect(1));

        Button eightD = button("8D EFFECT");
        eightD.setOnClickListener(v -> playEffect(2));

        Button loud = button("LOUDER / CLEAN");
        loud.setOnClickListener(v -> playEffect(3));

        TextView note = text("Digital gain is increased, then controlled by a soft limiter to reduce clipping.", 13);
        note.setTextColor(Color.GRAY);
        root.addView(note);
    }

    private void playEffect(int mode) {
        final int sampleRate = 48000;
        final double seconds = 5.0;
        final int samples = (int)(sampleRate * seconds);

        // Stereo PCM. We generate a richer test signal than the original single sine tone.
        final short[] data = new short[samples * 2];

        for (int i = 0; i < samples; i++) {
            double t = i / (double) sampleRate;

            // Musical-ish harmonic test signal.
            double source =
                    0.55 * Math.sin(2 * Math.PI * 220 * t) +
                    0.25 * Math.sin(2 * Math.PI * 440 * t) +
                    0.12 * Math.sin(2 * Math.PI * 880 * t);

            // Smooth attack/release.
            double env = Math.min(1.0, i / (sampleRate * 0.08));
            env *= Math.min(1.0, (samples - i) / (sampleRate * 0.12));

            double left = source;
            double right = source;

            if (mode == 0) {
                // Spatial: slow stereo movement with a wide image.
                double pan = 0.82 * Math.sin(2 * Math.PI * 0.18 * t);
                left = source * Math.sqrt((1.0 - pan) * 0.5);
                right = source * Math.sqrt((1.0 + pan) * 0.5);
            } else if (mode == 1) {
                // Reverb: short feedback-style echo taps.
                left = source;
                right = source;
                if (i >= (int)(0.085 * sampleRate)) {
                    double delayed = 0.42 * (
                        0.55 * Math.sin(2 * Math.PI * 220 * (t - 0.085)) +
                        0.25 * Math.sin(2 * Math.PI * 440 * (t - 0.085)) +
                        0.12 * Math.sin(2 * Math.PI * 880 * (t - 0.085)));
                    left += delayed;
                    right += delayed * 0.82;
                }
                if (i >= (int)(0.145 * sampleRate)) {
                    double delayed = 0.22 * Math.sin(2 * Math.PI * 440 * (t - 0.145));
                    left += delayed;
                    right += delayed * 0.72;
                }
            } else if (mode == 2) {
                // "8D": continuous left/right rotation using equal-power panning.
                double pan = Math.sin(2 * Math.PI * 0.11 * t);
                left = source * Math.sqrt((1.0 - pan) * 0.5);
                right = source * Math.sqrt((1.0 + pan) * 0.5);
            } else {
                // Loud-but-clean test: boost before a soft limiter.
                left = source * 2.6;
                right = source * 2.6;
            }

            left *= env;
            right *= env;

            // Soft limiter. Gain can exceed 1.0 internally, but output is kept below full-scale.
            left = softLimit(left);
            right = softLimit(right);

            data[i * 2] = (short)(left * 32767);
            data[i * 2 + 1] = (short)(right * 32767);
        }

        new Thread(() -> {
            int bufferBytes = data.length * 2;
            AudioTrack track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes,
                    AudioTrack.MODE_STATIC
            );
            track.write(data, 0, data.length);
            track.play();

            try {
                Thread.sleep(5200);
            } catch (InterruptedException ignored) {}

            track.release();
        }).start();
    }

    private double softLimit(double x) {
        // Smooth saturation rather than a hard clip.
        return Math.tanh(x * 1.35) / Math.tanh(1.35);
    }
}
