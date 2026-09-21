package com.example.headphonesexperience;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 42;
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
        TextView sub = text("No uploads. No files.\nProcess audio already playing on your phone.", 17);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        Button start = button("ACTIVATE");
        start.setOnClickListener(v -> requestCapture());

        TextView note = text("One Android permission. Then play audio in another app.", 13);
        note.setTextColor(Color.GRAY);
        root.addView(note);
    }

    private void showControls() {
        base();
        root.addView(text("LIVE AUDIO", 30));
        TextView status = text("The audio layer is active.\nYou can leave this screen and keep listening.", 16);
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

    private void requestCapture() {
        if (Build.VERSION.SDK_INT < 29) return;
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, AudioService.class);
            service.setAction(AudioService.ACTION_START);
            service.putExtra(AudioService.EXTRA_RESULT_CODE, resultCode);
            service.putExtra(AudioService.EXTRA_DATA, data);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(service);
            } else {
                startService(service);
            }
            showControls();
        }
    }

    private void sendEffect(int effect) {
        Intent service = new Intent(this, AudioService.class);
        service.setAction(AudioService.ACTION_EFFECT);
        service.putExtra(AudioService.EXTRA_EFFECT, effect);
        startService(service);
    }

    private void sendStop() {
        Intent service = new Intent(this, AudioService.class);
        service.setAction(AudioService.ACTION_STOP);
        startService(service);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }
}
