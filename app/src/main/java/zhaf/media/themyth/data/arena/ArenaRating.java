package zhaf.media.themyth.data.arena;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ArenaRating {

    public static final int START_RATING = 1000;
    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 5000;
    public static final int BOT_K = 8;

    private ArenaRating() {
    }

    public static String tier(int rating) {
        if (rating >= 2200) {
            return "mythic";
        }
        if (rating >= 1900) {
            return "diamond";
        }
        if (rating >= 1600) {
            return "platinum";
        }
        if (rating >= 1300) {
            return "gold";
        }
        if (rating >= 1000) {
            return "silver";
        }
        return "bronze";
    }

    public static int kFactor(int rating) {
        if (rating >= 2200) {
            return 16;
        }
        if (rating >= 1600) {
            return 24;
        }
        return 32;
    }

    public static double expected(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    public static int delta(int ratingA, int ratingB, boolean win, int streak, boolean bot) {
        return deltaScore(ratingA, ratingB, win ? 1.0 : 0.0, streak, bot);
    }

    public static int deltaScore(int ratingA, int ratingB, double score, int streak, boolean bot) {
        int k = bot ? BOT_K : kFactor(ratingA);
        int base = (int) Math.round(k * (score - expected(ratingA, ratingB)));
        int bonus = score >= 1.0 && !bot && streak >= 3 ? Math.min(15, 5 * (streak - 2)) : 0;
        return base + bonus;
    }

    public static int apply(int rating, int delta) {
        return Math.max(MIN_RATING, Math.min(MAX_RATING, rating + delta));
    }

    public static String seasonKey(long nowMillis) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(nowMillis);
        return String.format(Locale.ROOT, "s%d_%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }
}
