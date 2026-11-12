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
    public static final int EFFECT_COLUMNS = 4;
    public static final int EFFECT_ROWS = 11;
    public static final int EFFECT_FRAMES = EFFECT_COLUMNS * EFFECT_ROWS;
    public static final int EFFECT_FRAME_MS = 50;

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    public interface FramesCallback {
        void onLoaded(Frames frames);
    }

    public static final class Frames {
        public final Bitmap bitmap;
        public final int columns;
        public final int rows;
        public final int frameCount;

        Frames(Bitmap bitmap, int columns, int rows, int frameCount) {
            this.bitmap = bitmap;
            this.columns = columns;
            this.rows = rows;
            this.frameCount = frameCount;
        }
    }

    private static final String ROOT = "Sprites/Characters/";
    private static final String NEW_ROOT = "characters/";
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
        if (usesNewSheets(sprite)) {
            load(context, sprite, "idle.png", 0, IDLE_COLUMNS * 192, IDLE_ROWS * 256,
                    callback);
        } else {
            load(context, sprite, "idle.png", 1024, callback);
        }
    }

    public static void loadFrames(Context context, String sprite, SpriteAnimation animation,
                                  FramesCallback callback) {
        int columns = columnsFor(sprite, animation);
        int rows = rowsFor(sprite, animation);
        Callback loaded = bitmap -> callback.onLoaded(bitmap == null ? null
                : new Frames(bitmap, columns, rows, columns * rows));
        if (usesNewSheets(sprite)) {
            // Decode to 96x128 PER CELL, including the tall 4x11 ultimate sheets.
            load(context, sprite, animation.file, 0, columns * 96, rows * 128, loaded);
        } else {
            load(context, sprite, animation.file, ANIMATION_MAX_SIZE, loaded);
        }
    }

    public static void loadEffectFrames(Context context, String sprite, SpriteAnimation animation,
                                        FramesCallback callback) {
        if (!usesNewSheets(sprite) || (animation != SpriteAnimation.ULTIMA1
                && animation != SpriteAnimation.ULTIMA2)) {
            callback.onLoaded(null);
            return;
        }
        // Keep cached effect callbacks asynchronous to preserve action sequencing.
        load(context, sprite, "effects/" + animation.file, 0,
                EFFECT_COLUMNS * 320, EFFECT_ROWS * 180,
                bitmap -> MAIN.post(() -> callback.onLoaded(bitmap == null ? null
                        : new Frames(bitmap, EFFECT_COLUMNS, EFFECT_ROWS, EFFECT_FRAMES))));
    }

    public static int frameMs(String sprite, SpriteAnimation animation) {
        if (!usesNewSheets(sprite)) {
            return animation.frameMs;
        }
        switch (animation) {
            case RUN:
            case ATTACK:
            case HURT:
                return 65;
            case SKILL1:
            case SKILL2:
                return 45;
            case ULTIMA1:
            case ULTIMA2:
                return EFFECT_FRAME_MS;
            default:
                return 80;
        }
    }

    public static int frameCount(String sprite, SpriteAnimation animation) {
        return columnsFor(sprite, animation) * rowsFor(sprite, animation);
    }

    private static int columnsFor(String sprite, SpriteAnimation animation) {
        if (!usesNewSheets(sprite)) {
            return animation.columns;
        }
        return animation == SpriteAnimation.SKILL1 || animation == SpriteAnimation.SKILL2
                ? 6 : 4;
    }

    private static int rowsFor(String sprite, SpriteAnimation animation) {
        if (!usesNewSheets(sprite)) {
            return animation.rows;
        }
        switch (animation) {
            case VICTORY:
                return 3;
            case SKILL1:
            case SKILL2:
                return 6;
            case ULTIMA1:
            case ULTIMA2:
                return 11;
            default:
                return 2;
        }
    }

    public static boolean usesNewSheets(String sprite) {
        if (sprite == null) {
            return false;
        }
        switch (sprite) {
            case "gatotkaca":
            case "nyairorokidul":
            case "prabusiliwangi":
            case "sipitung":
            case "lutungkasarung":
            case "calonarang":
            case "kenarok":
            case "roromendut":
            case "damarwulan":
            case "jakatarub":
                return true;
            default:
                return false;
        }
    }

    private static void load(Context context, String sprite, String file, int maxSize,
                             Callback callback) {
        load(context, sprite, file, maxSize, 0, 0, callback);
    }

    private static void load(Context context, String sprite, String file, int maxSize,
                             int width, int height, Callback callback) {
        if (sprite == null || sprite.isEmpty()) {
            callback.onLoaded(null);
            return;
        }
        String path = (usesNewSheets(sprite) ? NEW_ROOT : ROOT) + sprite + "/" + file;
        String key = path + "#" + maxSize + "#" + width + "x" + height;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            callback.onLoaded(cached);
            return;
        }
        Context appContext = context.getApplicationContext();
        POOL.execute(() -> {
            Bitmap bitmap = width > 0 && height > 0
                    ? decodeSheet(appContext, path, width, height)
                    : decode(appContext, path, maxSize);
            if (bitmap != null) {
                CACHE.put(key, bitmap);
            }
            MAIN.post(() -> callback.onLoaded(bitmap));
        });
    }

    private static Bitmap decodeSheet(Context context, String path, int width, int height) {
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
        while (bounds.outWidth / (sample * 2) >= width
                && bounds.outHeight / (sample * 2) >= height) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream stream = context.getAssets().open(path)) {
            decoded = BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException error) {
            return null;
        }
        if (decoded == null || (decoded.getWidth() == width && decoded.getHeight() == height)) {
            return decoded;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(decoded, width, height, true);
        if (scaled != decoded) {
            decoded.recycle();
        }
        return scaled;
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
