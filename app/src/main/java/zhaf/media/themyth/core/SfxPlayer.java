package zhaf.media.themyth.core;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import zhaf.media.themyth.R;

public final class SfxPlayer {

    private static SoundPool soundPool;
    private static int clickId;
    private static int claimId;
    private static int winId;
    private static int loseId;
    private static int summonId;
    private static boolean loaded;

    private SfxPlayer() {
    }

    private static void ensureLoaded(Context context) {
        if (loaded) {
            return;
        }
        loaded = true;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build();
        clickId = soundPool.load(context.getApplicationContext(), R.raw.sfx_click, 1);
        claimId = soundPool.load(context.getApplicationContext(), R.raw.sfx_claim, 1);
        winId = soundPool.load(context.getApplicationContext(), R.raw.sfx_win, 1);
        loseId = soundPool.load(context.getApplicationContext(), R.raw.sfx_lose, 1);
        summonId = soundPool.load(context.getApplicationContext(), R.raw.sfx_summon, 1);
    }

    private static void play(Context context, int soundId) {
        if (!Prefs.sfxEnabled(context)) {
            return;
        }
        ensureLoaded(context);
        if (soundPool != null) {
            soundPool.play(soundId, 0.75f, 0.75f, 1, 0, 1f);
        }
    }

    public static void click(Context context) {
        ensureLoaded(context);
        play(context, clickId);
    }

    public static void claim(Context context) {
        ensureLoaded(context);
        play(context, claimId);
    }

    public static void win(Context context) {
        ensureLoaded(context);
        play(context, winId);
    }

    public static void lose(Context context) {
        ensureLoaded(context);
        play(context, loseId);
    }

    public static void summon(Context context) {
        ensureLoaded(context);
        play(context, summonId);
    }
}
