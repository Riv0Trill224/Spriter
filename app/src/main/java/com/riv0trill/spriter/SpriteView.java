package com.riv0trill.spriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.View;
import com.riv0trill.spriter.core.Motion;
import com.riv0trill.spriter.core.ShuffleBag;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SpriteView extends View {
    private final Paint bitmapPaint = new Paint();
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint();
    private final RectF rect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final List<Actor> actors = new ArrayList<>();
    private final Set<Uri> failed = new HashSet<>();
    private final SharedPreferences prefs;
    private final int count, fps, interval, size, speed;
    private final boolean showInfo, demo;
    private ShuffleBag<Uri> bag;
    private boolean running, paused, disposed, loading;
    private long lastFrame, lastInfo;
    private float swapSeconds;
    private int nextSlot, battery = -1, total;
    private String status = "Cargando carpeta…", clock = "", date = "", app = "";

    private static final class Actor {
        final Uri uri;
        final Bitmap bitmap;
        final Motion motion;
        Actor(Uri uri, Bitmap bitmap, Motion motion) { this.uri = uri; this.bitmap = bitmap; this.motion = motion; }
    }

    SpriteView(Context context, String source) {
        super(context);
        this.demo = "demo".equals(source);
        prefs = Prefs.get(context);
        count = Math.max(1, Math.min(5, prefs.getInt("count", 5)));
        fps = prefs.getInt("fps", 30) == 15 ? 15 : 30;
        interval = Math.max(0, prefs.getInt("interval", 40));
        size = Math.max(8, Math.min(35, prefs.getInt("size", 19)));
        speed = Math.max(10, Math.min(120, prefs.getInt("speed", 50)));
        showInfo = prefs.getBoolean("info", true);
        setClickable(true);
        // Nearest-neighbor scaling keeps pixel sprites crisp.
        bitmapPaint.setFilterBitmap(false);
        if (demo) {
            status = "DEMO · toca para controles";
            post(() -> { for (int i=0; i<count; i++) actors.add(createActor(null, demoBitmap(i))); invalidate(); });
        } else {
            loader.execute(() -> {
                try {
                    List<Uri> files = "pokemon".equals(source) ? SpriteFiles.builtIn(context)
                            : SpriteFiles.list(context.getContentResolver(), Uri.parse(prefs.getString("tree", "")));
                    handler.post(() -> {
                        if (disposed) return;
                        total = files.size(); bag = new ShuffleBag<>(files, random);
                        if (files.isEmpty()) status = "No hay PNG en esta carpeta. Elige otra carpeta.";
                        else { status = "Cargando sprites…"; requestLoad(-1); }
                        invalidate();
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        if (disposed) return;
                        status = "pokemon".equals(source) ? "Falta la colección Pokémon en este APK."
                                : "Carpeta no disponible. Revisa la SD y vuelve a elegirla.";
                        invalidate();
                    });
                }
            });
        }
    }

    private Bitmap demoBitmap(int index) {
        Bitmap b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b); Paint p = new Paint();
        int[] colors = {0xff63e6be,0xfff9c784,0xffa997ff,0xff7ad7ff,0xffff7ea7};
        p.setColor(colors[index % colors.length]); c.drawRect(4,4,60,60,p);
        p.setColor(0xff101521); c.drawRect(16,20,24,28,p); c.drawRect(40,20,48,28,p); c.drawRect(20,40,44,46,p);
        return b;
    }

    private void requestLoad(int slot) {
        if (disposed || loading || bag == null) return;
        Set<Uri> excluded = new HashSet<>(failed);
        for (Actor a : actors) excluded.add(a.uri);
        Uri next = bag.next(excluded);
        if (next == null) { updateStatus(); return; }
        loading = true;
        loader.execute(() -> {
            Bitmap bitmap = SpriteFiles.load(getContext(), next);
            handler.post(() -> {
                loading = false;
                if (disposed) return;
                if (bitmap == null) { failed.add(next); requestLoad(slot); return; }
                Actor actor = createActor(next, bitmap);
                if (slot >= 0 && slot < actors.size()) {
                    Actor old = actors.get(slot);
                    actor.motion.x = Math.min(old.motion.x, Math.max(0, getWidth() - actorWidth(actor)));
                    actor.motion.y = Math.min(old.motion.y, Math.max(0, fieldHeight() - actorHeight(actor)));
                    actor.motion.vx = old.motion.vx; actor.motion.vy = old.motion.vy;
                    actors.set(slot, actor);
                } else actors.add(actor);
                updateStatus(); invalidate();
                if (actors.size() < count) requestLoad(-1);
            });
        });
    }

    private void updateStatus() {
        if (actors.isEmpty()) status = "No hay PNG legibles o visibles. Revisa la carpeta.";
        else status = actors.size() + " sprites / " + total + " PNG"
                + (failed.isEmpty() ? "" : " · " + failed.size() + " omitidos") + " · toca para controles";
    }
    private Actor createActor(Uri uri, Bitmap bitmap) {
        float scale = Math.max(1, getWidth()) / 640f;
        double angle = Math.toRadians(25 + random.nextInt(41));
        float vx = (float)Math.cos(angle) * speed * scale * (random.nextBoolean() ? 1 : -1);
        float vy = (float)Math.sin(angle) * speed * scale * (random.nextBoolean() ? 1 : -1);
        Actor actor = new Actor(uri, bitmap, new Motion(0,0,vx,vy));
        actor.motion.x = random.nextFloat() * Math.max(0, getWidth() - actorWidth(actor));
        actor.motion.y = random.nextFloat() * Math.max(0, fieldHeight() - actorHeight(actor));
        return actor;
    }
    private float uiScale() { return Math.min(getWidth()/640f, getHeight()/400f); }
    private float panelHeight() { return showInfo ? 80 * uiScale() : 0; }
    private float fieldHeight() { return Math.max(1, getHeight() - panelHeight()); }
    private float edge() { return Math.min(getWidth(), fieldHeight()) * size / 100f; }
    private float actorWidth(Actor a) { return edge() * a.bitmap.getWidth() / Math.max(a.bitmap.getWidth(), a.bitmap.getHeight()); }
    private float actorHeight(Actor a) { return edge() * a.bitmap.getHeight() / Math.max(a.bitmap.getWidth(), a.bitmap.getHeight()); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        for (Actor a : actors) {
            float factor = oldw > 0 ? (float)w / oldw : Math.max(1,w);
            a.motion.vx *= factor; a.motion.vy *= factor;
            a.motion.x = random.nextFloat() * Math.max(0,w-actorWidth(a));
            a.motion.y = random.nextFloat() * Math.max(0,fieldHeight()-actorHeight(a));
        }
    }
    void setBattery(int value) { battery = value; invalidate(); }
    boolean togglePaused() {
        paused = !paused; lastFrame = 0;
        if (running) { handler.removeCallbacks(tick); handler.post(tick); }
        invalidate(); return paused;
    }
    void changeOne() {
        if (demo) {
            for (Actor a : actors) { a.motion.vx = -a.motion.vx; a.motion.vy = -a.motion.vy; }
        } else if (!actors.isEmpty() && !loading) { requestLoad(nextSlot++ % actors.size()); }
    }
    void setRunning(boolean value) {
        running = value; lastFrame = 0; handler.removeCallbacks(tick);
        if (running && !disposed) handler.post(tick);
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || disposed) return;
            long now = SystemClock.uptimeMillis();
            float dt = lastFrame == 0 ? 0 : Math.min(.1f, (now-lastFrame)/1000f);
            lastFrame = now;
            if (!paused) {
                for (Actor a : actors) a.motion.advance(dt,
                        Math.max(0,getWidth()-actorWidth(a)), Math.max(0,fieldHeight()-actorHeight(a)));
                if (!demo && interval > 0) {
                    swapSeconds += dt;
                    if (swapSeconds >= interval) { swapSeconds = 0; changeOne(); }
                }
            }
            if (now-lastInfo >= 1000 || lastInfo == 0) {
                lastInfo = now; Date time = new Date();
                clock = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time);
                date = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(time);
                app = "Elegida: " + prefs.getString("app_label", "ninguna");
            }
            invalidate();
            handler.postDelayed(this, paused ? 1000 : Math.max(1, (1000+fps-1)/fps - (SystemClock.uptimeMillis()-now)));
        }
    };

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.drawColor(0xff080b12);
        for (Actor a : actors) {
            rect.set(a.motion.x,a.motion.y,a.motion.x+actorWidth(a),a.motion.y+actorHeight(a));
            canvas.drawBitmap(a.bitmap,null,rect,bitmapPaint);
        }
        float scale = uiScale();
        textPaint.setColor(0xff8291aa); textPaint.setTextSize(13*scale);
        if (demo || actors.isEmpty() || !failed.isEmpty()) {
            canvas.drawText(TextUtils.ellipsize(status,textPaint,Math.max(1,getWidth()-24*scale),TextUtils.TruncateAt.END).toString(),12*scale,23*scale,textPaint);
        }
        if (paused) { textPaint.setColor(0xfff9c784); canvas.drawText("PAUSA",12*scale,45*scale,textPaint); }
        if (showInfo) {
            float y = getHeight()-panelHeight();
            panelPaint.setColor(0xff101521); canvas.drawRect(0,y,getWidth(),getHeight(),panelPaint);
            textPaint.setColor(0xff63e6be); textPaint.setTextSize(30*scale);
            canvas.drawText(clock,18*scale,y+36*scale,textPaint);
            textPaint.setColor(Color.WHITE); textPaint.setTextSize(16*scale);
            canvas.drawText(date,130*scale,y+34*scale,textPaint);
            String bat = battery < 0 ? "Batería —" : "Batería " + battery + "%";
            canvas.drawText(bat,Math.max(18*scale,getWidth()-textPaint.measureText(bat)-18*scale),y+34*scale,textPaint);
            textPaint.setColor(0xffb6c1d7); textPaint.setTextSize(15*scale);
            canvas.drawText(TextUtils.ellipsize(app,textPaint,Math.max(1,getWidth()-36*scale),TextUtils.TruncateAt.END).toString(),18*scale,y+65*scale,textPaint);
        }
    }
    void dispose() {
        disposed = true; running = false; handler.removeCallbacksAndMessages(null);
        loader.shutdownNow(); actors.clear();
    }
}
