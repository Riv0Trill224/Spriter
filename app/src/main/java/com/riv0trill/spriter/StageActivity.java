package com.riv0trill.spriter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;

public final class StageActivity extends Activity implements DisplayManager.DisplayListener {
    private static WeakReference<StageActivity> current = new WeakReference<>(null);
    private SpriteView sprites;
    private int expectedDisplay;
    private boolean started;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            sprites.setBattery(level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1);
        }
    };
    public static void stopExisting() {
        StageActivity old = current.get();
        if (old != null && !old.isFinishing()) old.finish();
    }
    public static int activeDisplay() {
        StageActivity a = current.get();
        return a == null || a.isFinishing() ? -1 : a.expectedDisplay;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        expectedDisplay = getIntent().getIntExtra("display", Display.DEFAULT_DISPLAY);
        int actual = getWindowManager().getDefaultDisplay().getDisplayId();
        if (actual != expectedDisplay) {
            Toast.makeText(this, "La ROM abrió Spriter en otro display. Elige la pantalla desde el launcher.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        current = new WeakReference<>(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        FrameLayout root = new FrameLayout(this);
        sprites = new SpriteView(this, getIntent().getStringExtra("source"));
        root.addView(sprites, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER); controls.setBackgroundColor(0xee101521);
        FrameLayout.LayoutParams bar = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(controls, bar); controls.setVisibility(View.GONE);
        addButton(controls, "Pausar", v -> {
            boolean paused = sprites.togglePaused();
            ((Button)v).setText(paused ? "Continuar" : "Pausar");
        });
        addButton(controls, "Cambiar", v -> sprites.changeOne());
        addButton(controls, "Ocultar", v -> { controls.setVisibility(View.GONE); immersive(); });
        addButton(controls, "Cerrar", v -> finish());
        sprites.setOnClickListener(v -> controls.setVisibility(controls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        setContentView(root);
        getSystemService(DisplayManager.class).registerDisplayListener(this, new Handler(Looper.getMainLooper()));
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void addButton(LinearLayout row, String title, View.OnClickListener action) {
        Button button = new Button(this); button.setText(title); button.setTextSize(12); button.setAllCaps(false);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setPadding(0,0,0,0);
        row.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); button.setOnClickListener(action);
    }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    @Override protected void onStart() { super.onStart(); started = true; updateRunning(); }
    // A secondary activity may be paused yet still visible. Only onStop suspends rendering.
    @Override protected void onStop() { started = false; updateRunning(); super.onStop(); }
    private void updateRunning() {
        if (sprites == null) return;
        Display d = getSystemService(DisplayManager.class).getDisplay(expectedDisplay);
        sprites.setRunning(started && d != null && d.getState() == Display.STATE_ON);
    }
    @Override public void onDisplayAdded(int id) { }
    @Override public void onDisplayChanged(int id) { if (id == expectedDisplay) updateRunning(); }
    @Override public void onDisplayRemoved(int id) { if (id == expectedDisplay) finish(); }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        // Honor a deliberate move made through the console's window manager.
        expectedDisplay = getWindowManager().getDefaultDisplay().getDisplayId(); updateRunning();
    }
    @Override protected void onDestroy() {
        if (sprites != null) {
            sprites.dispose();
            getSystemService(DisplayManager.class).unregisterDisplayListener(this);
            unregisterReceiver(batteryReceiver);
        }
        if (current.get() == this) current.clear();
        super.onDestroy();
    }
}
