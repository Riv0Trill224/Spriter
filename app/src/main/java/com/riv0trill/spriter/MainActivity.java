package com.riv0trill.spriter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Display;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int FOLDER = 41;
    private SharedPreferences prefs;
    private LinearLayout body;
    private TextView folderLabel, appLabel;
    private Spinner displaySpinner;
    private Display[] displays = new Display[0];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Prefs.get(this);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18); body.setPadding(pad, pad, pad, pad);
        scroll.addView(body); setContentView(scroll);
        text("SPRITER", 28, 0xff63e6be);
        text("Tu colección en movimiento · v0.2.0", 14, 0xffb6c1d7);
        text("1 · Imágenes", 19, Color.WHITE);
        folderLabel = text("", 14, 0xffb6c1d7);
        button("Elegir carpeta de PNG", v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(pick, FOLDER); }
            catch (Exception e) { message("No hay selector de archivos disponible."); }
        });
        text("2 · Pantalla de los sprites", 19, Color.WHITE);
        displaySpinner = new Spinner(this); body.addView(displaySpinner);
        text("Los ID identifican pantallas; comprueba físicamente cuál es cada una.", 13, 0xffb6c1d7);
        choose("Personajes", "count", new String[]{"1", "2", "3", "4", "5"}, new int[]{1,2,3,4,5}, 5);
        choose("Velocidad", "speed", new String[]{"Tranquila", "Normal", "Rápida"}, new int[]{25,50,90}, 50);
        choose("Tamaño", "size", new String[]{"Pequeño", "Mediano", "Grande"}, new int[]{12,19,28}, 19);
        choose("Animación", "fps", new String[]{"15 FPS · ahorro", "30 FPS · suave"}, new int[]{15,30}, 30);
        choose("Cambiar un sprite cada…", "interval", new String[]{"20 segundos", "40 segundos", "60 segundos", "Solo manualmente"}, new int[]{20,40,60,0}, 40);
        Switch info = new Switch(this); info.setText("Mostrar hora, fecha, batería y app elegida");
        info.setChecked(prefs.getBoolean("info", true)); body.addView(info);
        info.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean("info", on).apply());
        text("3 · Iniciar", 19, Color.WHITE);
        button("Iniciar Pokémon incluidos · 151", v -> startStage("pokemon"));
        button("Iniciar con mi carpeta", v -> startStage("folder"));
        button("Probar demo de colores", v -> startStage("demo"));
        button("Detener sprites", v -> StageActivity.stopExisting());
        text("4 · Juego o emulador", 19, Color.WHITE);
        appLabel = text("", 14, 0xffb6c1d7);
        button("Elegir aplicación", v -> chooseApp());
        button("Abrir app en otra pantalla", v -> launchGame());
        text("Durante la animación: toca para ver controles. Puedes pausar, cambiar un sprite o cerrar. "
                + "La pantalla sigue animada mientras otra app recibe el foco. Si usas ambas pantallas para jugar, detén Spriter.", 14, 0xffb6c1d7);
        button("Créditos Pokémon", v -> message("Colección: PokeAPI/sprites · Pokémon 1–151.\n"
                + "Imágenes © The Pokémon Company.\nhttps://github.com/PokeAPI/sprites\n"
                + "Los avisos originales del repositorio se incluyen en el APK."));
        updateLabels();
    }

    @Override protected void onResume() { super.onResume(); refreshDisplays(); updateLabels(); }

    private void refreshDisplays() {
        displays = getSystemService(DisplayManager.class).getDisplays();
        List<String> names = new ArrayList<>();
        int saved = prefs.getInt("display", -1), selected = 0;
        for (int i = 0; i < displays.length; i++) {
            Display d = displays[i]; Display.Mode m = d.getMode();
            names.add("ID " + d.getDisplayId() + " · " + d.getName() + " · " + m.getPhysicalWidth() + "×" + m.getPhysicalHeight());
            if (d.getDisplayId() == saved) selected = i;
        }
        if (saved == -1 && displays.length > 1) selected = 1;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        displaySpinner.setAdapter(adapter); displaySpinner.setSelection(selected);
    }

    private int selectedDisplay() {
        int i = displaySpinner.getSelectedItemPosition();
        if (i < 0 || i >= displays.length) return -1;
        return displays[i].getDisplayId();
    }

    private void startStage(String source) {
        if (source.equals("folder") && prefs.getString("tree", "").isEmpty()) { message("Selecciona primero tu carpeta con PNG."); return; }
        int id = selectedDisplay();
        if (id < 0) { message("No hay pantalla disponible."); return; }
        prefs.edit().putInt("display", id).apply();
        Intent stage = new Intent(this, StageActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .putExtra("source", source).putExtra("display", id);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29 && !getSystemService(ActivityManager.class).isActivityStartAllowedOnDisplay(this, id, stage)) {
                message("Android no permite abrir Spriter en esta pantalla. Prueba otro ID."); return;
            }
            StageActivity.stopExisting();
            startActivity(stage, ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
        } catch (Exception e) { message("No se pudo abrir la pantalla: " + e.getClass().getSimpleName()); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != FOLDER || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String old = prefs.getString("tree", "");
            prefs.edit().putString("tree", uri.toString()).apply();
            if (!old.isEmpty() && !old.equals(uri.toString())) {
                try { getContentResolver().releasePersistableUriPermission(Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) { /* The previous provider may have been removed. */ }
            }
            updateLabels();
        } catch (Exception e) { message("No se pudo conservar acceso a esa carpeta. Prueba otra carpeta local o de la SD."); }
    }

    private void updateLabels() {
        String tree = prefs.getString("tree", "");
        String label = "Sin carpeta · archivos como 1.png, 2.png, 3.png";
        if (!tree.isEmpty()) {
            try { label = "Carpeta: " + DocumentsContract.getTreeDocumentId(Uri.parse(tree)); }
            catch (Exception e) { label = "Vuelve a elegir tu carpeta."; }
        }
        folderLabel.setText(label);
        appLabel.setText("App elegida: " + prefs.getString("app_label", "ninguna"));
    }

    private void chooseApp() {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(getPackageManager().queryIntentActivities(query, 0));
        apps.removeIf(a -> getPackageName().equals(a.activityInfo.packageName));
        Collections.sort(apps, (a,b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) labels[i] = apps.get(i).loadLabel(getPackageManager()) + "\n" + apps.get(i).activityInfo.packageName;
        new AlertDialog.Builder(this).setTitle("Juego o emulador").setItems(labels, (dialog, which) -> {
            ResolveInfo app = apps.get(which);
            prefs.edit().putString("app_package", app.activityInfo.packageName)
                    .putString("app_label", app.loadLabel(getPackageManager()).toString()).apply();
            updateLabels();
        }).setNegativeButton("Cancelar", null).show();
    }

    private void launchGame() {
        String pkg = prefs.getString("app_package", "");
        Intent game = getPackageManager().getLaunchIntentForPackage(pkg);
        if (game == null) { message("Elige una aplicación instalada."); return; }
        int stageId = StageActivity.activeDisplay();
        if (stageId < 0) stageId = selectedDisplay();
        List<Display> targets = new ArrayList<>();
        for (Display d : getSystemService(DisplayManager.class).getDisplays())
            if (d.getDisplayId() != stageId) targets.add(d);
        if (targets.isEmpty()) { message("Se necesitan dos pantallas para este botón. Puedes probar la demo en la pantalla actual."); return; }
        String[] labels = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) labels[i] = "ID " + targets.get(i).getDisplayId() + " · " + targets.get(i).getName();
        new AlertDialog.Builder(this).setTitle("Abrir juego en…").setItems(labels, (d, which) -> {
            int id = targets.get(which).getDisplayId();
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29 && !getSystemService(ActivityManager.class).isActivityStartAllowedOnDisplay(this, id, game)) {
                    message("Android no permite abrir esta app en la pantalla elegida."); return;
                }
                game.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(game, ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
            } catch (Exception e) { message("No se pudo abrir el juego en ese display. Ábrelo desde el launcher de la consola."); }
        }).setNegativeButton("Cancelar", null).show();
    }

    private void choose(String title, String key, String[] labels, int[] values, int fallback) {
        text(title, 15, Color.WHITE);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == prefs.getInt(key, fallback)) selected = i;
        spinner.setSelection(selected); body.addView(spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long id) { prefs.edit().putInt(key, values[i]).apply(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }
    private TextView text(String value, int size, int color) {
        TextView label = new TextView(this); label.setText(value); label.setTextSize(size); label.setTextColor(color);
        label.setPadding(0, dp(7), 0, dp(5)); body.addView(label); return label;
    }
    private void button(String title, View.OnClickListener listener) {
        Button button = new Button(this); button.setText(title); button.setAllCaps(false);
        button.setOnClickListener(listener); body.addView(button);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void message(String value) { new AlertDialog.Builder(this).setMessage(value).setPositiveButton("Entendido", null).show(); }
}
