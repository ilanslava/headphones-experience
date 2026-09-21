package com.example.headphonesexperience;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
        root.addView(text("LIVE AUDIO", 30));
        TextView sub = text("No uploads. No files.\nNo audio duplication.\nControl the output effects.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        Button start = button("ACTIVATE");
        start.setOnClickListener(v -> startEffects());

        TextView note = text("No capture permission. No second audio stream.", 13);
        note.setTextColor(Color.GRAY);
        root.addView(note);
    }

    private void showControls() {
        base();
        root.addView(text("LIVE AUDIO", 30));
        TextView status = text("Output effects are active.\nLeave this screen and keep listening.", 16);
        status.setTextColor(Color.LTGRAY);
        root.addView(status);

        addEffect("ORIGINAL", 0);
        addEffect("SPATIAL", 1);
        addEffect("REVERB", 2);
        addEffect("8D", 3);
        addEffect("LOUD / CLEAN", 4);

        Button stop = button("STOP");
        stop.setOnClickListener(v -> {
            sendStop();
            showHome();
        });
    }

    private void addEffect(String label, int effect) {
        Button b = button(label);
        b.setOnClickListener(v -> sendEffect(effect));
    }

    private void startEffects() {
        Intent service = new Intent(this, AudioService.class);
        service.setAction(AudioService.ACTION_START);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }

        showControls();
    }

    private void sendEffect(int effect) {
        Intent service = new Intent(this, AudioService.class);
        service.setAction(AudioService.ACTION_EFFECT);
        service.putExtra(AudioService.EXTRA_EFFECT, effect);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private void sendStop() {
        Intent service = new Intent(this, AudioService.class);
        service.setAction(AudioService.ACTION_STOP);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }
}
