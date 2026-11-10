package zhaf.media.themyth.data.meta;

public final class DailyLoginLogic {

    public static final int CYCLE_DAYS = 7;

    private static final int[] COIN = {2000, 3000, 4000, 5000, 6000, 8000, 12000};
    private static final int[] DIAMOND = {0, 0, 10, 10, 15, 20, 50};
    private static final int[] ENERGY = {0, 10, 10, 0, 10, 0, 20};
    private static final int[] TICKETS = {0, 0, 0, 1, 0, 1, 1};

    private DailyLoginLogic() {
    }

    public static boolean canClaim(String lastDay, long now) {
        return lastDay == null || lastDay.isEmpty() || !MetaClock.sameDay(lastDay, now);
    }

    public static int nextStreak(String lastDay, int streak, long now) {
        int current = Math.max(0, Math.min(streak, CYCLE_DAYS));
        if (lastDay == null || lastDay.isEmpty()) {
            return 1;
        }
        if (MetaClock.sameDay(lastDay, now)) {
            return Math.max(1, current);
        }
        if (lastDay.equals(MetaClock.yesterdayKey(now))) {
            return current >= CYCLE_DAYS ? 1 : current + 1;
        }
        return 1;
    }

    public static LoginReward rewardFor(int day) {
        int index = Math.max(1, Math.min(day, CYCLE_DAYS)) - 1;
        return new LoginReward(index + 1, COIN[index], DIAMOND[index],
                ENERGY[index], TICKETS[index]);
    }
}
