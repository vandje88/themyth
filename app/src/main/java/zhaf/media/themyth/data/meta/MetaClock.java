package zhaf.media.themyth.data.meta;

import zhaf.media.themyth.data.social.SocialDay;

public final class MetaClock {

    private MetaClock() {
    }

    public static String dayKey(long now) {
        return SocialDay.dayKey(now);
    }

    public static String yesterdayKey(long now) {
        return SocialDay.dayKey(now - SocialDay.DAY_MS);
    }

    public static String weekKey(long now) {
        return SocialDay.weekKey(now);
    }

    public static long nextDay(long now) {
        return SocialDay.nextDay(now);
    }

    public static boolean sameDay(String key, long now) {
        return SocialDay.sameDay(key, now);
    }
}
