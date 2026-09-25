package com.riv0trill.spriter;

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;
import com.riv0trill.spriter.core.UpdateRules;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class Updater {
    private static final String API="https://api.github.com/repos/Riv0Trill224/Spriter";
    private final StageActivity activity;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private boolean busy,waitingInstall;
    private Candidate verified;
    private static final class Candidate {
        String name,sha;long code,size,asset;
    }
    Updater(StageActivity activity){this.activity=activity;}
    void credentials(){
        EditText field=new EditText(activity);field.setSingleLine();field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setHint("Token de lectura de este repositorio");
        new AlertDialog.Builder(activity).setTitle("Actualizaciones de GitHub")
            .setMessage("Este repositorio es privado. Para comprobar y descargar releases, usa un token de GitHub limitado a Spriter, con Contents: Read-only. Se guarda cifrado en este dispositivo. Si el repositorio se hace público, no será necesario.")
            .setView(field).setPositiveButton("Guardar",(d,n)->{
                String token=field.getText().toString().trim();
                if(token.contains("\n")||token.contains(" ")){activity.message("El token contiene espacios no válidos.");return;}
                try{SecureToken.save(activity,token);check(true);}catch(Exception e){activity.message("No se pudo guardar el acceso de forma segura.");}
            }).setNeutralButton("Eliminar acceso",(d,n)->{try{SecureToken.save(activity,"");}catch(Exception ignored){}})
            .setNegativeButton("Cancelar",null).show();
    }
    void check(boolean manual){
        if(busy || closed){if(manual)toast("Hay una comprobación o descarga en curso.");return;}
        busy=true;Prefs.get(activity).edit().putLong("last_update_check",System.currentTimeMillis()).apply();
        if(manual)toast("Buscando actualizaciones…");
        worker.execute(()->{
            try {
                Candidate best=find();
                ui(()->{
                    busy=false;
                    if(best==null){if(manual)toast("No hay una versión más reciente disponible.");return;}
                    if(!manual){toast("Spriter "+best.name+" disponible · ⋮ → Buscar actualizaciones");return;}
                    new AlertDialog.Builder(activity).setTitle("Spriter "+best.name)
                        .setMessage("Hay una nueva versión. ¿Descargar e instalar?")
                        .setPositiveButton("Actualizar",(d,n)->download(best)).setNegativeButton("Después",null).show();
                });
            }catch(Exception e){ui(()->{busy=false;if(manual)activity.message(error(e));});}
        });
    }
    private Candidate find()throws Exception{
        JSONArray releases=new JSONArray(new String(read(API+"/releases?per_page=10",false,256*1024),"UTF-8"));Candidate best=null;
        for(int i=0;i<releases.length();i++){
            JSONObject release=releases.getJSONObject(i);if(release.optBoolean("draft")||release.optBoolean("prerelease"))continue;
            JSONArray assets=release.optJSONArray("assets");
            if(assets==null||assets.length()==0)assets=new JSONArray(new String(read(API+"/releases/"+release.getLong("id")+"/assets?per_page=100",false,256*1024),"UTF-8"));
            long meta=0,apk=0,assetSize=0;
            for(int j=0;j<assets.length();j++){
                JSONObject a=assets.getJSONObject(j);
                if("update.json".equals(a.optString("name")))meta=a.getLong("id");
                if("Spriter.apk".equals(a.optString("name"))){apk=a.getLong("id");assetSize=a.getLong("size");}
            }
            if(meta<=0||apk<=0)continue;
            JSONObject m=new JSONObject(new String(read(API+"/releases/assets/"+meta,true,32768),"UTF-8"));
            long code=m.getLong("versionCode"),size=m.getLong("sizeBytes");
            if(!UpdateRules.validMetadata(m.getString("package"),code,BuildConfig.VERSION_CODE,size,m.getString("sha256"))||size!=assetSize)continue;
            if(best==null||code>best.code){best=new Candidate();best.code=code;best.size=size;best.sha=m.getString("sha256");best.name=m.getString("versionName");best.asset=apk;}
        }return best;
    }
    private HttpURLConnection connect(String url,boolean binary)throws Exception{
        String token=SecureToken.read(activity);
        for(int hop=0;hop<6;hop++){
            if(!UpdateRules.allowedDownload(url))throw new IOException("Destino de descarga no permitido.");
            URL target=new URL(url);HttpURLConnection c=(HttpURLConnection)target.openConnection();
            c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Spriter/"+BuildConfig.VERSION_NAME);
            c.setRequestProperty("Accept",binary?"application/octet-stream":"application/vnd.github+json");
            if(target.getHost().equals("api.github.com")){
                if(!target.getPath().startsWith("/repos/Riv0Trill224/Spriter/")){c.disconnect();throw new IOException("Ruta de actualización no válida.");}
                c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
                if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
            }
            int status=c.getResponseCode();
            if(status>=300&&status<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException();url=new URL(target,location).toString();continue;}
            if(status==401||status==403||status==404){c.disconnect();throw new IOException("GitHub no permitió acceder. En ⋮ → Acerca de → Acceso a updates configura un token de lectura de Spriter. Comprueba también que exista un release publicado.");}
            if(status!=200){c.disconnect();throw new IOException("GitHub respondió HTTP "+status+". Inténtalo más tarde.");}
            return c;
        }throw new IOException("Demasiadas redirecciones de descarga.");
    }
    private byte[] read(String url,boolean binary,int max)throws Exception{
        HttpURLConnection c=connect(url,binary);
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n,total=0;
            while((n=in.read(buffer))!=-1){total+=n;if(total>max||closed)throw new IOException("Respuesta demasiado grande o cancelada.");out.write(buffer,0,n);}return out.toByteArray();
        }finally{c.disconnect();}
    }
    private void download(Candidate candidate){
        if(busy||closed)return;busy=true;verified=null;toast("Descargando Spriter…");
        worker.execute(()->{
            File temp=new File(activity.getCacheDir(),"update.part"),apk=new File(activity.getCacheDir(),"update.apk");
            try{
                HttpURLConnection c=connect(API+"/releases/assets/"+candidate.asset,true);long total=0;
                MessageDigest hash=MessageDigest.getInstance("SHA-256");
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                    byte[] b=new byte[16384];int n;
                    while((n=in.read(b))!=-1){total+=n;if(closed||total>candidate.size)throw new IOException("Tamaño de APK no válido.");out.write(b,0,n);hash.update(b,0,n);}
                }finally{c.disconnect();}
                if(total!=candidate.size||!hex(hash.digest()).equalsIgnoreCase(candidate.sha))throw new IOException("La descarga está incompleta o su hash no coincide.");
                validateApk(temp,candidate);
                if(!temp.renameTo(apk))throw new IOException("No se pudo guardar la actualización.");
                ui(()->{busy=false;verified=candidate;install();});
            }catch(Exception e){temp.delete();ui(()->{busy=false;activity.message(error(e));});}
        });
    }
    @SuppressWarnings("deprecation")
    private Set<String> signatures(PackageInfo info)throws Exception{
        android.content.pm.Signature[] values=Build.VERSION.SDK_INT>=28?info.signingInfo.getApkContentsSigners():info.signatures;
        Set<String> hashes=new HashSet<>();for(android.content.pm.Signature s:values)hashes.add(hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));return hashes;
    }
    @SuppressWarnings("deprecation")
    private void validateApk(File file,Candidate c)throws Exception{
        PackageManager pm=activity.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo incoming=pm.getPackageArchiveInfo(file.getPath(),flags),installed=pm.getPackageInfo(activity.getPackageName(),flags);
        if(incoming==null||!activity.getPackageName().equals(incoming.packageName))throw new IOException("El APK no pertenece a Spriter.");
        long version=Build.VERSION.SDK_INT>=28?incoming.getLongVersionCode():incoming.versionCode;
        if(version!=c.code||version<=BuildConfig.VERSION_CODE)throw new IOException("La versión del APK no coincide.");
        Set<String> original=signatures(installed),next=signatures(incoming);
        if(original.isEmpty()||!original.equals(next))throw new IOException("La firma no coincide con esta instalación. Para pasar de debug al primer release firmado debes instalar ese release manualmente.");
    }
    private void install(){
        if(verified==null)return;
        if(!activity.getPackageManager().canRequestPackageInstalls()){
            waitingInstall=true;
            try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}
            catch(Exception e){waitingInstall=false;activity.message("Permite a Spriter instalar aplicaciones desde los ajustes de Android.");}return;
        }
        Candidate c=verified;verified=null;
        // Revalidate the private cached file immediately before handing it to Android.
        try{
            File apk=new File(activity.getCacheDir(),"update.apk");validateApk(apk,c);
            Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+activity.getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        }catch(Exception e){activity.message(error(e));}
    }
    void resumeInstall(){if(waitingInstall){waitingInstall=false;if(activity.getPackageManager().canRequestPackageInstalls())install();else activity.message("Instalación pendiente: permite instalar aplicaciones y vuelve a buscar la actualización.");}}
    private void ui(Runnable r){activity.runOnUiThread(()->{if(!closed&&!activity.isFinishing()&&!activity.isDestroyed())r.run();});}
    private void toast(String s){Toast.makeText(activity,s,Toast.LENGTH_LONG).show();}
    private static String error(Exception e){return e instanceof IOException&&e.getMessage()!=null?e.getMessage():"No se pudo comprobar la actualización. Revisa tu conexión y el acceso a GitHub.";}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    void close(){closed=true;worker.shutdownNow();}
}
