package zhaf.media.themyth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import zhaf.media.themyth.ui.battle.UltimateEffectView;
import zhaf.media.themyth.ui.common.SpriteAnimation;
import zhaf.media.themyth.ui.common.SpriteArt;

@RunWith(AndroidJUnit4.class)
public final class SpriteAssetInstrumentedTest {
    private static final String[] SPRITES = {
            "gatotkaca", "nyairorokidul", "prabusiliwangi", "sipitung", "lutungkasarung",
            "calonarang", "kenarok", "roromendut", "damarwulan", "jakatarub"
    };

    @Test
    public void packagedAtlasesMatchManifest() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int characters = 0;
        int effects = 0;
        for (String sprite : SPRITES) {
            JSONObject manifest;
            try (InputStream input = context.getAssets().open("characters/" + sprite + "/manifest.json")) {
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                manifest = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            }
            assertEquals(236, manifest.getInt("totalCharacterFrames"));
            JSONObject animations = manifest.getJSONObject("animations");
            assertEquals(13, animations.length());
            characters += verifySheets(context, sprite, animations);
            effects += verifySheets(context, sprite, manifest.getJSONObject("effects"));
        }
        assertEquals(2360, characters);
        assertEquals(880, effects);
    }

    private int verifySheets(Context context, String sprite, JSONObject sheets) throws Exception {
        int total = 0;
        Iterator<String> names = sheets.keys();
        while (names.hasNext()) {
            JSONObject sheet = sheets.getJSONObject(names.next());
            int columns = sheet.getInt("columns");
            int rows = sheet.getInt("rows");
            int frames = sheet.getInt("frameCount");
            assertEquals(columns * rows, frames);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream input = context.getAssets().open("characters/" + sprite + "/" + sheet.getString("file"))) {
                BitmapFactory.decodeStream(input, null, options);
            }
            assertEquals(columns * sheet.getInt("frameWidth"), options.outWidth);
            assertEquals(rows * sheet.getInt("frameHeight"), options.outHeight);
            total += frames;
        }
        return total;
    }

    @Test
    public void loaderUsesExactGridAndPerCellResolution() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String sprite : SPRITES) {
            assertTrue(SpriteArt.usesNewSheets(sprite));
            int total = 0;
            for (SpriteAnimation animation : SpriteAnimation.values()) {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<SpriteArt.Frames> result = new AtomicReference<>();
                SpriteArt.loadFrames(context, sprite, animation, frames -> {
                    result.set(frames);
                    latch.countDown();
                });
                assertTrue(sprite + "/" + animation, latch.await(15, TimeUnit.SECONDS));
                SpriteArt.Frames frames = result.get();
                assertNotNull(sprite + "/" + animation, frames);
                assertEquals(frames.columns * 96, frames.bitmap.getWidth());
                assertEquals(frames.rows * 128, frames.bitmap.getHeight());
                assertTrue(frames.bitmap.hasAlpha());
                assertEquals(SpriteArt.frameCount(sprite, animation), frames.frameCount);
                total += frames.frameCount;
            }
            assertEquals(236, total);
        }
        assertFalse(SpriteArt.usesNewSheets("aruna"));
        assertEquals(SpriteAnimation.HURT.frames(), SpriteArt.frameCount("aruna", SpriteAnimation.HURT));
    }

    @Test
    public void ultimateLoaderAndUnsupportedAnimation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SpriteArt.Frames> result = new AtomicReference<>();
        SpriteArt.loadEffectFrames(context, "nyairorokidul", SpriteAnimation.ULTIMA2, frames -> {
            result.set(frames);
            latch.countDown();
        });
        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertEquals(44, result.get().frameCount);
        assertEquals(1280, result.get().bitmap.getWidth());
        assertEquals(1980, result.get().bitmap.getHeight());
        assertTrue(result.get().bitmap.hasAlpha());
        SpriteArt.loadEffectFrames(context, "nyairorokidul", SpriteAnimation.IDLE, result::set);
        assertEquals(null, result.get());
    }

    @Test
    public void ultimateOverlaySlicesFramesAndDoesNotConsumeTouch() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            UltimateEffectView view = new UltimateEffectView(context);
            view.layout(0, 0, 80, 80);
            assertFalse(view.isClickable());
            assertFalse(view.isFocusable());
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.getImportantForAccessibility());
            Bitmap atlas = Bitmap.createBitmap(8, 22, Bitmap.Config.ARGB_8888);
            for (int frame = 0; frame < 44; frame++) {
                int color = Color.rgb(frame * 5, 40, 120);
                int x = frame % 4 * 2;
                int y = frame / 4 * 2;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        atlas.setPixel(x + dx, y + dy, color);
                    }
                }
            }
            Bitmap output = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
            view.setFrame(atlas, 31, false);
            view.draw(new Canvas(output));
            assertEquals(Color.rgb(155, 40, 120), output.getPixel(40, 40));
            output.eraseColor(Color.TRANSPARENT);
            view.setFrame(atlas, 31, true);
            view.draw(new Canvas(output));
            assertEquals(Color.rgb(155, 40, 120), output.getPixel(40, 40));
            output.eraseColor(Color.TRANSPARENT);
            view.setFrame(atlas, 44, false);
            view.draw(new Canvas(output));
            assertEquals(Color.TRANSPARENT, output.getPixel(40, 40));
            atlas.recycle();
            output.recycle();
        });
    }
}
