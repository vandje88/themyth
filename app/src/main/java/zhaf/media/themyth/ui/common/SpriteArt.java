package zhaf.media.themyth.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SpriteArt {

    public static final int IDLE_COLUMNS = 4;
    public static final int IDLE_ROWS = 2;
    public static final int IDLE_FRAMES = IDLE_COLUMNS * IDLE_ROWS;

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    private static final String ROOT = "Sprites/Characters/";
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(16 * 1024 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private SpriteArt() {
    }

    public static void loadPortrait(Context context, String sprite, Callback callback) {
        load(context, sprite, sprite + ".png", 512, callback);
    }

    public static void loadIdle(Context context, String sprite, Callback callback) {
        load(context, sprite, "idle.png", 1024, callback);
    }

    private static void load(Context context, String sprite, String file, int maxSize,
                             Callback callback) {
        if (sprite == null || sprite.isEmpty()) {
            callback.onLoaded(null);
            return;
        }
        String path = ROOT + sprite + "/" + file;
        String key = path + "#" + maxSize;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            callback.onLoaded(cached);
            return;
        }
        Context appContext = context.getApplicationContext();
        POOL.execute(() -> {
            Bitmap bitmap = decode(appContext, path, maxSize);
            if (bitmap != null) {
                CACHE.put(key, bitmap);
            }
            MAIN.post(() -> callback.onLoaded(bitmap));
        });
    }

    private static Bitmap decode(Context context, String path, int maxSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getAssets().open(path)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        } catch (IOException error) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > maxSize) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream stream = context.getAssets().open(path)) {
            return BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException error) {
            return null;
        }
    }
}
