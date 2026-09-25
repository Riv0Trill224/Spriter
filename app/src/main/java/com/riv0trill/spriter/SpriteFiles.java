package com.riv0trill.spriter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SpriteFiles {
    static List<Uri> builtIn(Context context, String collection) throws Exception {
        List<Uri> result = new ArrayList<>();
        String[] files = context.getAssets().list(collection);
        if (files == null) return result;
        for (String name : files) if (name.matches("[0-9]+\\.png"))
            result.add(Uri.parse("asset:///" + collection + "/" + name));
        return result;
    }

    private static InputStream open(Context context, Uri uri) throws Exception {
        if ("asset".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null || !path.matches("/(pokemon|custom)/[0-9]+\\.png"))
                throw new IllegalArgumentException("Recurso desconocido");
            return context.getAssets().open(path.substring(1));
        }
        return context.getContentResolver().openInputStream(uri);
    }
    static List<Uri> list(ContentResolver resolver, Uri tree) throws Exception {
        List<Uri> result = new ArrayList<>();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                DocumentsContract.getTreeDocumentId(tree));
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE};
        try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
            if (cursor == null) throw new IllegalStateException("La carpeta no está disponible.");
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".png")
                        && !DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) {
                    if (result.size() >= 10000) throw new IllegalStateException("Usa una carpeta con hasta 10 000 PNG.");
                    result.add(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)));
                }
            }
        }
        return result;
    }

    static Bitmap load(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = open(context, uri)) { BitmapFactory.decodeStream(in, null, bounds); }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 256)
                options.inSampleSize *= 2;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inScaled = false;
            Bitmap bitmap;
            try (InputStream in = open(context, uri)) { bitmap = BitmapFactory.decodeStream(in, null, options); }
            if (bitmap == null) return null;
            // Trim transparent borders, preserving aspect ratio and all visible pixels.
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            int left = w, top = h, right = -1, bottom = -1;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                if ((pixels[y * w + x] >>> 24) != 0) {
                    left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                }
            }
            if (right < left) return null;
            return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1);
        } catch (Exception | OutOfMemoryError e) { return null; }
    }
}
