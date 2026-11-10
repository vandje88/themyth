package zhaf.media.themyth.data.meta;

public final class IdleRewardLogic {

    public static final long MIN_MS = 10L * 60L * 1000L;
    public static final int COIN_PER_MINUTE = 5;
    public static final int MINUTES_PER_ENERGY = 30;
    public static final int BASE_CAP_MINUTES = 8 * 60;
    public static final int BONUS_MINUTES_PER_LEVEL = 6;
    public static final int MAX_CAP_MINUTES = 24 * 60;

    private IdleRewardLogic() {
    }

    public static int capMinutes(int level) {
        int bonus = Math.max(0, level - 1) * BONUS_MINUTES_PER_LEVEL;
        return Math.min(MAX_CAP_MINUTES, BASE_CAP_MINUTES + bonus);
    }

    public static long capMs(int level) {
        return capMinutes(level) * 60L * 1000L;
    }

    public static long accruedMs(long since, long now, int level) {
        if (since <= 0L || now <= since) {
            return 0L;
        }
        return Math.min(now - since, capMs(level));
    }

    public static boolean ready(long accruedMs) {
        return accruedMs >= MIN_MS;
    }

    public static IdleReward reward(long accruedMs, int level) {
        long minutes = Math.max(0L, accruedMs / 60000L);
        int coin = (int) Math.min(Integer.MAX_VALUE, minutes * COIN_PER_MINUTE);
        int energy = (int) (minutes / MINUTES_PER_ENERGY);
        return new IdleReward(minutes, coin, energy);
    }
}
