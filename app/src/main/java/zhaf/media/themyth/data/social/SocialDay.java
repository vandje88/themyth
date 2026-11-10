package zhaf.media.themyth.data.social;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SocialDay {

    public static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private SocialDay() {
    }

    public static long dayStart(long now) {
        Calendar calendar = calendar(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long weekStart(long now) {
        Calendar calendar = calendar(now);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int offsetDays = (dayOfWeek + 5) % 7;
        return dayStart(now) - offsetDays * DAY_MS;
    }

    public static long weekEnd(long now) {
        return weekStart(now) + 7L * DAY_MS;
    }

    public static long nextDay(long now) {
        return dayStart(now) + DAY_MS;
    }

    public static String dayKey(long now) {
        return format("yyyy-MM-dd", now);
    }

    public static String weekKey(long now) {
        return "w" + (weekStart(now) / DAY_MS);
    }

    public static boolean sameDay(String dayKey, long now) {
        return dayKey != null && dayKey.equals(dayKey(now));
    }

    public static boolean giftReady(String lastGiftDay, long now) {
        return lastGiftDay == null || lastGiftDay.isEmpty() || !sameDay(lastGiftDay, now);
    }

    public static int daysLeft(long expiresAt, long now) {
        if (expiresAt <= now) {
            return 0;
        }
        return (int) Math.ceil((expiresAt - now) / (double) DAY_MS);
    }

    private static String format(String pattern, long now) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(UTC);
        return format.format(new Date(now));
    }

    private static Calendar calendar(long now) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.US);
        calendar.setTimeInMillis(now);
        return calendar;
    }
}
