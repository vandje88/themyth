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
    public static final int ANIMATION_MAX_SIZE = 640;

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    private static final String ROOT = "Sprites/Characters/";
    private static final String NYAI_SPRITE = "nyairorokidul";
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(32 * 1024 * 1024) {
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

    public static void loadAnimation(Context context, String sprite, SpriteAnimation animation,
                                     Callback callback) {
        load(context, sprite, animation.file, ANIMATION_MAX_SIZE, callback);
    }

    public static int columns(String sprite, SpriteAnimation animation) {
        if (animation == SpriteAnimation.ULTIMA1 && NYAI_SPRITE.equals(sprite)) {
            return 5;
        }
        return animation.columns;
    }

    public static int rows(String sprite, SpriteAnimation animation) {
        if (animation == SpriteAnimation.ULTIMA1 && NYAI_SPRITE.equals(sprite)) {
            return 5;
        }
        return animation.rows;
    }

    public static int frames(String sprite, SpriteAnimation animation) {
        return columns(sprite, animation) * rows(sprite, animation);
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
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream stream = context.getAssets().open(path)) {
            decoded = BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException error) {
            return null;
        }
        if (decoded == null) {
            return null;
        }
        int longest = Math.max(decoded.getWidth(), decoded.getHeight());
        if (longest <= maxSize) {
            return decoded;
        }
        float scale = (float) maxSize / longest;
        int width = Math.max(1, Math.round(decoded.getWidth() * scale));
        int height = Math.max(1, Math.round(decoded.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(decoded, width, height, true);
        if (scaled != decoded) {
            decoded.recycle();
        }
        return scaled;
    }
}
