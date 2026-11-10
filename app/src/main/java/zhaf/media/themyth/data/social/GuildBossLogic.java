package zhaf.media.themyth.data.social;

import java.util.Map;

public final class GuildBossLogic {

    public static final int MAX_DAMAGE_BP = 2500;
    public static final int MAX_HP = 50_000_000;
    public static final String BOSS_ID = "boss_bayangan_gatotkaca";

    private GuildBossLogic() {
    }

    public static int damageDealt(int hpMax, int hpPercentLeft) {
        if (hpMax <= 0) {
            return 0;
        }
        int left = Math.max(0, Math.min(100, hpPercentLeft));
        return (int) Math.max(0L, Math.round(hpMax * (100.0 - left) / 100.0));
    }

    public static int capDamage(int hpMax, int damage) {
        if (hpMax <= 0 || damage <= 0) {
            return 0;
        }
        int cap = (int) ((long) hpMax * MAX_DAMAGE_BP / 10000L);
        return Math.min(Math.min(damage, cap), hpMax);
    }

    public static int apply(int hpLeft, int damage) {
        return Math.max(0, hpLeft - Math.max(0, damage));
    }

    public static int guildCoin(int damage) {
        return Math.max(0, damage) / 500;
    }

    public static int weeklyPoints(int damage) {
        return Math.max(0, damage) / 1000;
    }

    public static boolean attackedToday(Map<String, Map<String, Boolean>> attacks,
                                        String uid, String dayKey) {
        if (attacks == null || uid == null || dayKey == null) {
            return false;
        }
        Map<String, Boolean> days = attacks.get(uid);
        return days != null && Boolean.TRUE.equals(days.get(dayKey));
    }

    public static int bossHp(int memberCount) {
        int base = 1_200_000;
        int scaled = base + Math.max(0, memberCount - 1) * 40_000;
        return (int) Math.min(scaled, MAX_HP);
    }

    public static String weekKey(long now) {
        return SocialDay.weekKey(now);
    }

    public static long weekEnd(long now) {
        return SocialDay.weekEnd(now);
    }
}
