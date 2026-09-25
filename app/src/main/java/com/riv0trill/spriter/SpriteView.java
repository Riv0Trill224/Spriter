package com.riv0trill.spriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import com.riv0trill.spriter.core.Motion;
import com.riv0trill.spriter.core.Population;
import com.riv0trill.spriter.core.ShuffleBag;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.io.File;

final class SpriteView extends View {
    private final Paint spritePaint = new Paint(), backgroundPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final List<Actor> actors = new ArrayList<>();
    private final Set<Uri> failed = new HashSet<>();
    private final SharedPreferences prefs;
    private ShuffleBag<Uri> bag;
    private Bitmap background;
    private boolean running, disposed, loading;
    private int generation, total, desired, battery = -1, opacity;
    private float speed, elapsed, nextPopulation = 30, nextReplacement = 18;
    private long lastFrame, lastInfo;
    private String status = "Cargando colección…", info = "";
    private static final class Actor {
        final Uri uri; final Bitmap bitmap; final Motion motion; final float scale;
        Actor(Uri uri, Bitmap bitmap, Motion motion, float scale) {
            this.uri=uri; this.bitmap=bitmap; this.motion=motion; this.scale=scale;
        }
    }
    SpriteView(Context context) {
        super(context); prefs=Prefs.get(context); applySettings(); reloadCollection(); reloadBackground();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void applySettings() {
        speed=Math.max(5,Math.min(160,prefs.getInt("speed",50)));
        opacity=Math.max(0,Math.min(100,prefs.getInt("background_opacity",80)));
        invalidate();
    }
    void reloadBackground() {
        loader.execute(() -> {
            Bitmap b=null;
            File f=new File(getContext().getFilesDir(),"wallpaper.png");
            try {
                BitmapFactory.Options o=new BitmapFactory.Options(); o.inJustDecodeBounds=true;
                BitmapFactory.decodeFile(f.getPath(),o);
                o.inSampleSize=1;
                while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1600) o.inSampleSize*=2;
                o.inJustDecodeBounds=false;
                if(f.isFile()) b=BitmapFactory.decodeFile(f.getPath(),o);
            } catch (Exception | OutOfMemoryError ignored) { }
            final Bitmap result=b;
            handler.post(() -> { if(!disposed) { background=result; invalidate(); } });
        });
    }
    void reloadCollection() {
        final int token=++generation;
        loading=false; actors.clear(); failed.clear(); desired=0; total=0; bag=null;
        status="Cargando colección…";
        String saved=prefs.getString("source","mixed");
        final String source=Arrays.asList("mixed","pokemon","custom","folder").contains(saved)?saved:"mixed";
        final String folder=prefs.getString("tree","");
        loader.execute(() -> {
            try {
                List<Uri> files=new ArrayList<>();
                if(source.equals("mixed") || source.equals("pokemon")) files.addAll(SpriteFiles.builtIn(getContext(),"pokemon"));
                if(source.equals("mixed") || source.equals("custom")) files.addAll(SpriteFiles.builtIn(getContext(),"custom"));
                if(source.equals("folder")) files.addAll(SpriteFiles.list(getContext().getContentResolver(),Uri.parse(folder)));
                handler.post(() -> {
                    if(disposed || token!=generation) return;
                    total=files.size(); bag=new ShuffleBag<>(files,random);
                    status=files.isEmpty()?"No hay PNG disponibles en esta colección.":"Cargando sprites…";
                    pickPopulation(); invalidate();
                });
            } catch(Exception e) {
                handler.post(() -> { if(!disposed && token==generation) {
                    status="Carpeta no disponible. Vuelve a elegirla en ⋮ → Colección."; invalidate();
                }});
            }
        });
    }
    private void pickPopulation() {
        desired=Population.choose(Math.max(0,total-failed.size()),getWidth(),fieldHeight(),edge(),random);
        reconcile();
    }
    private void reconcile() {
        while(actors.size()>desired) actors.remove(random.nextInt(actors.size()));
        if(actors.size()<desired) requestLoad(-1);
    }
    private void requestLoad(int slot) {
        if(disposed || loading || bag==null) return;
        Set<Uri> excluded=new HashSet<>(failed);
        for(Actor a:actors) excluded.add(a.uri);
        Uri next=bag.next(excluded);
        if(next==null) { if(actors.isEmpty()) status="No hay PNG legibles en la colección."; return; }
        final int token=generation;
        loading=true;
        loader.execute(() -> {
            Bitmap bitmap=SpriteFiles.load(getContext(),next);
            handler.post(() -> {
                if(disposed || token!=generation) return;
                loading=false;
                if(bitmap==null) {
                    failed.add(next); desired=Math.min(desired,total-failed.size()); requestLoad(slot); return;
                }
                Actor a=createActor(next,bitmap);
                if(slot>=0 && slot<actors.size()) actors.set(slot,a);
                else if(actors.size()<desired) actors.add(a);
                status=""; reconcile(); invalidate();
            });
        });
    }
    private Actor createActor(Uri uri,Bitmap bitmap) {
        double angle=Math.toRadians(20+random.nextInt(51));
        float rate=.75f+random.nextFloat()*.5f;
        Actor a=new Actor(uri,bitmap,new Motion(0,0,
                (float)Math.cos(angle)*rate*(random.nextBoolean()?1:-1),
                (float)Math.sin(angle)*rate*(random.nextBoolean()?1:-1)),.8f+random.nextFloat()*.4f);
        a.motion.x=random.nextFloat()*Math.max(0,getWidth()-actorWidth(a));
        a.motion.y=random.nextFloat()*Math.max(0,fieldHeight()-actorHeight(a));
        return a;
    }
    private float uiScale() { return Math.min(getWidth()/640f,getHeight()/400f); }
    private float fieldHeight() { return Math.max(1,getHeight()-(prefs.getBoolean("info",true)?35*uiScale():0)); }
    private float edge() { return Math.max(1,Math.min(getWidth(),fieldHeight())*.15f); }
    private float actorWidth(Actor a) { return edge()*a.scale*a.bitmap.getWidth()/Math.max(a.bitmap.getWidth(),a.bitmap.getHeight()); }
    private float actorHeight(Actor a) { return edge()*a.scale*a.bitmap.getHeight()/Math.max(a.bitmap.getWidth(),a.bitmap.getHeight()); }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh) {
        super.onSizeChanged(w,h,ow,oh);
        for(Actor a:actors) {
            a.motion.x=Math.min(a.motion.x,Math.max(0,w-actorWidth(a)));
            a.motion.y=Math.min(a.motion.y,Math.max(0,fieldHeight()-actorHeight(a)));
        }
        pickPopulation();
    }
    void setBattery(int b) { battery=b; }
    void setRunning(boolean value) {
        running=value; lastFrame=0; handler.removeCallbacks(tick);
        if(value && !disposed) handler.post(tick);
    }
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(disposed || !running) return;
            long now=SystemClock.uptimeMillis();
            float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1000f); lastFrame=now;
            for(Actor a:actors) a.motion.advance(dt*speed*getWidth()/640f,
                    Math.max(0,getWidth()-actorWidth(a)),Math.max(0,fieldHeight()-actorHeight(a)));
            elapsed+=dt;
            if(elapsed>=nextPopulation) { pickPopulation(); nextPopulation=elapsed+25+random.nextInt(26); }
            if(elapsed>=nextReplacement) {
                if(!actors.isEmpty() && !loading) requestLoad(random.nextInt(actors.size()));
                nextReplacement=elapsed+12+random.nextInt(19);
            }
            if(now-lastInfo>1000 || lastInfo==0) {
                lastInfo=now;
                info=new SimpleDateFormat("HH:mm  ·  EEE d MMM",Locale.getDefault()).format(new Date())
                        +(battery>=0?"  ·  "+battery+"%":"");
            }
            invalidate(); handler.postDelayed(this,Math.max(1,34-(SystemClock.uptimeMillis()-now)));
        }
    };
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(0xff080b12);
        if(background!=null) {
            float factor=Math.max((float)getWidth()/background.getWidth(),(float)getHeight()/background.getHeight());
            float w=background.getWidth()*factor,h=background.getHeight()*factor;
            rect.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
            backgroundPaint.setAlpha(Math.round(opacity*2.55f));
            c.drawBitmap(background,null,rect,backgroundPaint);
        }
        for(Actor a:actors) {
            rect.set(a.motion.x,a.motion.y,a.motion.x+actorWidth(a),a.motion.y+actorHeight(a));
            c.drawBitmap(a.bitmap,null,rect,spritePaint);
        }
        float scale=uiScale();textPaint.setColor(0xffdce8ef);textPaint.setTextSize(15*scale);
        textPaint.setShadowLayer(3,0,1,Color.BLACK);
        if(actors.isEmpty()) c.drawText(TextUtils.ellipsize(status,textPaint,Math.max(1,getWidth()-60*scale),TextUtils.TruncateAt.END).toString(),12*scale,getHeight()/2f,textPaint);
        if(prefs.getBoolean("info",true)) c.drawText(info,12*scale,getHeight()-12*scale,textPaint);
    }
    void dispose() { disposed=true; generation++; handler.removeCallbacksAndMessages(null); loader.shutdownNow(); actors.clear(); }
}
