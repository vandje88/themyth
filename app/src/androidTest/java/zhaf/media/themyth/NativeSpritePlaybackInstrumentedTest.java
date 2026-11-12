package zhaf.media.themyth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import zhaf.media.themyth.battle.SkillFactory;
import zhaf.media.themyth.battle.engine.BattleEvent;
import zhaf.media.themyth.battle.engine.BattleResult;
import zhaf.media.themyth.battle.engine.BattleUnit;
import zhaf.media.themyth.databinding.ActivityBattleBinding;
import zhaf.media.themyth.ui.battle.BattleFieldView;
import zhaf.media.themyth.ui.home.HomeActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

@RunWith(AndroidJUnit4.class)
public final class NativeSpritePlaybackInstrumentedTest {
    @Test
    public void fullRootEffectUsesBattleClockAndRejectsStaleReplayLoads() throws Exception {
        Class<? extends Activity> hostActivity = FirebaseAuth.getInstance().getCurrentUser() == null
                ? LoginActivity.class : HomeActivity.class;
        String[] sprites = {"gatotkaca", "nyairorokidul", "prabusiliwangi", "sipitung", "lutungkasarung", "calonarang"};
        String[] ids = {"gatotkaca", "nyai_roro_kidul", "prabu_siliwangi", "si_pitung", "lutung_kasarung", "calon_arang"};
        List<BattleUnit> units = new ArrayList<>();
        for (int i = 0; i < sprites.length; i++) {
            BattleUnit unit = new BattleUnit("test-" + i, ids[i], sprites[i], sprites[i],
                    i / 3, i % 3, 1, "lightning", "fighter", 1000, 100, 100, 100,
                    1000, 15000, 10000, 0, 0, 0);
            SkillFactory.assign(unit);
            units.add(unit);
        }
        BattleUnit actor = units.get(0);
        List<BattleEvent> events = Arrays.asList(
                BattleEvent.action(0, actor, actor.ultimate2, null, 0),
                BattleEvent.end(0, 0, "native-render-test"));
        BattleResult result = new BattleResult(1, 0, 1, 100, 0,
                actor.uid, actor.name, "local-render-only", events, units);
        AtomicReference<ActivityBattleBinding> bindingRef = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(1);
        try (ActivityScenario<? extends Activity> scenario = ActivityScenario.launch(hostActivity)) {
            scenario.onActivity(activity -> {
                ActivityBattleBinding binding = ActivityBattleBinding.inflate(activity.getLayoutInflater());
                bindingRef.set(binding);
                activity.setContentView(binding.getRoot());
                binding.overlayLoading.setVisibility(View.GONE);
                binding.battleField.setUltimateEffectView(binding.ultimateEffect);
                binding.battleField.setListener(new BattleFieldView.Listener() {
                    @Override public void onReady() { ready.countDown(); }
                    @Override public void onLog(BattleEvent event) { }
                    @Override public void onPlaybackEnd() { }
                });
                binding.battleField.setBattle(result);
            });
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            ActivityBattleBinding binding = bindingRef.get();
            BattleFieldView field = binding.battleField;
            onMain(() -> {
                assertTrue(binding.ultimateEffect.getHeight() > field.getHeight());
                invoke(field, "drainEvents");
                field.play();
                field.pause();
            });
            waitOnView(field, 1200);
            onMain(() -> {
                assertNotNull(read(field, "activeEffect"));
                write(field, "clockMs", 750L);
                invoke(field, "updateDisplays");
                assertEquals(15, read(binding.ultimateEffect, "frameIndex"));
                assertNotNull(read(binding.ultimateEffect, "bitmap"));
            });
            waitOnView(field, 120);
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(screenshot);
            File capture = new File(context.getExternalFilesDir(null), "native-ultimate.png");
            try (FileOutputStream output = new FileOutputStream(capture)) {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
            screenshot.recycle();
            waitOnView(field, 150);
            onMain(() -> {
                assertEquals(750L, read(field, "clockMs"));
                assertEquals(15, read(binding.ultimateEffect, "frameIndex"));
                field.setSpeed(3f);
                field.resumePlayback();
            });
            waitOnView(field, 130);
            onMain(() -> {
                field.pause();
                assertTrue((Long) read(field, "clockMs") > 750L);
                field.restart();
                field.pause();
                assertNull(read(binding.ultimateEffect, "bitmap"));
                invoke(field, "drainEvents");
                field.restart();
                field.pause();
            });
            waitOnView(field, 180);
            onMain(() -> {
                assertNull(read(field, "activeEffect"));
                assertNull(read(binding.ultimateEffect, "bitmap"));
                invoke(field, "drainEvents");
            });
            waitOnView(field, 180);
            onMain(() -> {
                write(field, "clockMs", 1000L);
                invoke(field, "drainEvents");
                assertEquals(0L, read(field, "playbackEndsAt"));
                write(field, "clockMs", 2500L);
                invoke(field, "drainEvents");
                assertNull(read(binding.ultimateEffect, "bitmap"));
                assertEquals(4100L, read(field, "playbackEndsAt"));
                write(field, "clockMs", 4200L);
                invoke(field, "updateDisplays");
                assertTrue(field.isEnded());
            });
        }
    }

    private static void waitOnView(View view, long delay) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        view.postDelayed(latch::countDown, delay);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    private static Object read(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void write(Object target, String name, long value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setLong(target, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void invoke(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
