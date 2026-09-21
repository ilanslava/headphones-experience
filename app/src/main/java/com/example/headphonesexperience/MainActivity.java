package com.example.headphonesexperience;

import android.app.Activity;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private LinearLayout root;
    private int step = 0;
    private final String[] experiences = {"SPACE", "DETAIL", "BASS", "VOICE"};

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

    private void showHome() {
        base();
        root.addView(text("PUT ON YOUR HEADPHONES", 28));
        TextView sub = text("Something is waiting to be heard.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);
        Button begin = new Button(this);
        begin.setText("BEGIN");
        root.addView(begin);
        begin.setOnClickListener(v -> showExperience());
    }

    private void showExperience() {
        if (step >= experiences.length) { showResult(); return; }
        final int current = step++;
        base();
        root.addView(text(experiences[current], 34));
        TextView sub = text("Listen. Notice what changes.", 18);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);
        Button listen = new Button(this);
        listen.setText("LISTEN");
        root.addView(listen);
        listen.setOnClickListener(v -> playTone(current));
        Button next = new Button(this);
        next.setText("NEXT");
        root.addView(next);
        next.setOnClickListener(v -> showExperience());
    }

    private void playTone(int mode) {
        final int sampleRate = 44100;
        final int samples = sampleRate * 2;
        final short[] data = new short[samples];
        final double frequency = mode == 0 ? 440 : mode == 1 ? 880 : mode == 2 ? 90 : 520;
        for (int i = 0; i < samples; i++) {
            double envelope = Math.min(1.0, Math.min(i / 1000.0, (samples - i) / 1000.0));
            data[i] = (short)(Math.sin(2 * Math.PI * frequency * i / sampleRate) * 0.18 * envelope * 32767);
        }
        new Thread(() -> {
            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    data.length * 2, AudioTrack.MODE_STATIC);
            track.write(data, 0, data.length);
            track.play();
            try { Thread.sleep(2100); } catch (InterruptedException ignored) {}
            track.release();
        }).start();
    }

    private void showResult() {
        base();
        root.addView(text("YOUR SOUND", 34));
        TextView sub = text("You discovered how you hear it.", 20);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);
        Button restart = new Button(this);
        restart.setText("START AGAIN");
        root.addView(restart);
        restart.setOnClickListener(v -> { step = 0; showHome(); });
    }
}
