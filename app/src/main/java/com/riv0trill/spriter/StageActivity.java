package com.riv0trill.spriter;

import android.app.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

public class StageActivity extends Activity implements DisplayManager.DisplayListener {
    private static WeakReference<StageActivity> current=new WeakReference<>(null);
    private static final int WALLPAPER=41,FOLDER=42;
    private final ExecutorService files=Executors.newSingleThreadExecutor();
    private SpriteView sprites;
    private Updater updater;
    private boolean started,registered;
    private int expectedDisplay;
    private final BroadcastReceiver batteryReceiver=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {
            int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            sprites.setBattery(level>=0 && scale>0?Math.round(level*100f/scale):-1);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        expectedDisplay=getIntent().getIntExtra("display",getWindowManager().getDefaultDisplay().getDisplayId());
        if(expectedDisplay!=getWindowManager().getDefaultDisplay().getDisplayId()) {
            Toast.makeText(this,"La ROM no permitió abrir la pantalla elegida.",Toast.LENGTH_LONG).show();finish();return;
        }
        StageActivity old=current.get();if(old!=null && old!=this) old.finish();current=new WeakReference<>(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); immersive();applyBrightness();
        FrameLayout root=new FrameLayout(this);
        sprites=new SpriteView(this);root.addView(sprites,new FrameLayout.LayoutParams(-1,-1));
        TextView dots=new TextView(this);dots.setText("⋮");dots.setTextColor(Color.WHITE);dots.setTextSize(28);
        dots.setGravity(Gravity.CENTER);dots.setContentDescription("Abrir opciones");dots.setBackgroundColor(0x77080b12);
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.END);
        root.addView(dots,position);dots.setOnClickListener(this::options);setContentView(root);
        getSystemService(DisplayManager.class).registerDisplayListener(this,new Handler(Looper.getMainLooper()));
        registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));registered=true;
        updater=new Updater(this);
        if(state==null) Toast.makeText(this,"Spriter · GitHub",Toast.LENGTH_LONG).show();
        if(System.currentTimeMillis()-Prefs.get(this).getLong("last_update_check",0)>24*60*60*1000L) updater.check(false);
    }
    private void options(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        String[] items={"Velocidad","Brillo de esta pantalla","Fondo personalizado","Opacidad del fondo","Colección de sprites","Pantalla","Mostrar / ocultar reloj","Abrir juego","Buscar actualizaciones","Acerca de","Cerrar"};
        for(int i=0;i<items.length;i++) menu.getMenu().add(0,i,i,items[i]);
        menu.setOnMenuItemClickListener(item -> {
            switch(item.getItemId()) {
                case 0: slider("Velocidad","speed",5,160,50);break;
                case 1: slider("Brillo de esta pantalla","brightness",5,100,70);break;
                case 2: new AlertDialog.Builder(this).setTitle("Fondo").setItems(new String[]{"Elegir imagen desde archivos","Quitar fondo"},(d,n)->{
                    if(n==0) pick(Intent.ACTION_OPEN_DOCUMENT,"image/*",WALLPAPER);
                    else { new File(getFilesDir(),"wallpaper.png").delete();sprites.reloadBackground(); }
                }).show();break;
                case 3: slider("Opacidad del fondo","background_opacity",0,100,80);break;
                case 4: collection();break;
                case 5: displays();break;
                case 6: Prefs.get(this).edit().putBoolean("info",!Prefs.get(this).getBoolean("info",true)).apply();sprites.applySettings();break;
                case 7: chooseGame();break;
                case 8: updater.check(true);break;
                case 9: about();break;
                case 10: finish();break;
                default:break;
            }return true;
        });menu.show();
    }
    private void slider(String title,String key,int min,int max,int fallback) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView label=new TextView(this);SeekBar slider=new SeekBar(this);box.addView(label);box.addView(slider);
        int original=Prefs.get(this).getInt(key,fallback);
        slider.setMax(max-min);slider.setProgress(Math.max(min,Math.min(max,original))-min);label.setText(String.valueOf(slider.getProgress()+min));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
            public void onProgressChanged(SeekBar b,int value,boolean user){
                int v=value+min;label.setText(String.valueOf(v));Prefs.get(StageActivity.this).edit().putInt(key,v).apply();sprites.applySettings();applyBrightness();
            }
        });
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(title).setView(box).setPositiveButton("Listo",null);
        if(key.equals("brightness")) dialog.setNeutralButton("Usar brillo del sistema",(d,n)->{Prefs.get(this).edit().remove(key).apply();applyBrightness();});
        dialog.show();
    }
    private void applyBrightness() {
        WindowManager.LayoutParams p=getWindow().getAttributes();
        p.screenBrightness=Prefs.get(this).contains("brightness")?Math.max(.05f,Math.min(1f,Prefs.get(this).getInt("brightness",70)/100f)):-1f;
        getWindow().setAttributes(p);
    }
    private void collection() {
        new AlertDialog.Builder(this).setTitle("Colección · cantidad automática").setItems(new String[]{"Todas: Pokémon + tus 154 sprites","Solo Pokémon","Solo tus 154 sprites","Elegir carpeta externa"},(d,n)->{
            if(n==3) {pick(Intent.ACTION_OPEN_DOCUMENT_TREE,null,FOLDER);return;}
            Prefs.get(this).edit().putString("source",new String[]{"mixed","pokemon","custom"}[n]).apply();sprites.reloadCollection();
        }).show();
    }
    private void pick(String action,String type,int request) {
        Intent i=new Intent(action);if(type!=null) {i.setType(type);i.addCategory(Intent.CATEGORY_OPENABLE);}
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(i,request);}catch(Exception e){message("No hay explorador de archivos disponible.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        if(request==FOLDER) {
            try {
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String previous=Prefs.get(this).getString("tree","");
                Prefs.get(this).edit().putString("tree",uri.toString()).putString("source","folder").apply();
                if(!previous.isEmpty()&&!previous.equals(uri.toString()))try {getContentResolver().releasePersistableUriPermission(Uri.parse(previous),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
                sprites.reloadCollection();
            }catch(Exception e){message("No se pudo conservar acceso a esa carpeta.");}
        }else if(request==WALLPAPER) {
            Toast.makeText(this,"Preparando fondo…",Toast.LENGTH_SHORT).show();
            files.execute(()->{
                File temp=new File(getFilesDir(),"wallpaper.tmp");
                try {
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
                    try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}
                    if(o.outWidth<=0 || o.outHeight<=0)throw new IOException();
                    o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1600)o.inSampleSize*=2;o.inJustDecodeBounds=false;
                    Bitmap b;try(InputStream in=getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,o);}
                    if(b==null)throw new IOException();
                    try(FileOutputStream out=new FileOutputStream(temp)){if(!b.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();}
                    if(!temp.renameTo(new File(getFilesDir(),"wallpaper.png")))throw new IOException();
                    runOnUiThread(()->{if(!isDestroyed()){sprites.reloadBackground();Toast.makeText(this,"Fondo guardado",Toast.LENGTH_SHORT).show();}});
                }catch(Exception|OutOfMemoryError e){temp.delete();runOnUiThread(()->{if(!isDestroyed())message("No se pudo leer esa imagen. Prueba un PNG o JPG más pequeño.");});}
            });
        }
    }
    private void displays() {
        Display[] ds=getSystemService(DisplayManager.class).getDisplays();String[] names=new String[ds.length];
        for(int i=0;i<ds.length;i++)names[i]="ID "+ds[i].getDisplayId()+" · "+ds[i].getName();
        new AlertDialog.Builder(this).setTitle("Pantalla de Spriter").setItems(names,(d,n)->{
            int id=ds[n].getDisplayId();if(id==getWindowManager().getDefaultDisplay().getDisplayId())return;
            Intent target=new Intent(this,StageActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK).putExtra("display",id);
            try{startActivity(target,ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());}catch(Exception e){message("Android no permite abrir esa pantalla.");}
        }).show();
    }
    private void chooseGame() {
        List<ResolveInfo> apps=new ArrayList<>(getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0));
        apps.removeIf(x->getPackageName().equals(x.activityInfo.packageName));
        Collections.sort(apps,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        String[] names=new String[apps.size()];for(int i=0;i<names.length;i++)names[i]=apps.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle("Juego o emulador").setItems(names,(d,n)->{
            List<Display> other=new ArrayList<>();for(Display display:getSystemService(DisplayManager.class).getDisplays())if(display.getDisplayId()!=expectedDisplay)other.add(display);
            if(other.isEmpty()){message("No hay otra pantalla disponible.");return;}
            String[] labels=new String[other.size()];for(int i=0;i<labels.length;i++)labels[i]="ID "+other.get(i).getDisplayId();
            new AlertDialog.Builder(this).setTitle("Pantalla del juego").setItems(labels,(dialog,index)->{
                Intent game=getPackageManager().getLaunchIntentForPackage(apps.get(n).activityInfo.packageName);
                try{if(game!=null)startActivity(game,ActivityOptions.makeBasic().setLaunchDisplayId(other.get(index).getDisplayId()).toBundle());}catch(Exception e){message("Abre el juego desde el launcher de la consola.");}
            }).show();
        }).show();
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("Spriter "+BuildConfig.VERSION_NAME)
            .setMessage("Por Riv0Trill224\n305 sprites incluidos: 151 de PokeAPI y 154 de tu colección.\n\nPokémon: imágenes © The Pokémon Company. Créditos a PokeAPI/sprites.\n\nRepositorio:\nhttps://github.com/Riv0Trill224/Spriter\n\nCantidad de personajes elegida automáticamente según el espacio disponible.")
            .setPositiveButton("GitHub Spriter",(d,n)->openUrl("https://github.com/Riv0Trill224/Spriter"))
            .setNeutralButton("Créditos PokeAPI",(d,n)->openUrl("https://github.com/PokeAPI/sprites"))
            .setNegativeButton("Acceso a updates",(d,n)->updater.credentials()).show();
    }
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){message(url);}}
    void message(String text){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setMessage(text).setPositiveButton("Entendido",null).show();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override protected void onStart(){super.onStart();started=true;updateRunning();}
    @Override protected void onStop(){started=false;updateRunning();super.onStop();}
    @Override protected void onResume(){super.onResume();if(updater!=null)updater.resumeInstall();}
    private void updateRunning(){if(sprites!=null){expectedDisplay=getWindowManager().getDefaultDisplay().getDisplayId();Display d=getSystemService(DisplayManager.class).getDisplay(expectedDisplay);sprites.setRunning(started&&d!=null&&d.getState()==Display.STATE_ON);}}
    @Override public void onDisplayAdded(int id){}
    @Override public void onDisplayChanged(int id){updateRunning();}
    @Override public void onDisplayRemoved(int id){if(id==expectedDisplay)finish();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);updateRunning();}
    @Override protected void onDestroy(){if(sprites!=null)sprites.dispose();if(updater!=null)updater.close();files.shutdownNow();if(registered){getSystemService(DisplayManager.class).unregisterDisplayListener(this);unregisterReceiver(batteryReceiver);}if(current.get()==this)current.clear();super.onDestroy();}
}
