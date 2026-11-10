package zhaf.media.themyth.data.meta;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerMeta {

    public final long lastSeen;
    public final long idleAt;
    public final long idleClaimAt;
    public final String loginDay;
    public final int loginStreak;
    public final int bossAttacks;
    public final int bestRating;
    public final Map<String, Integer> dailyProgress;
    public final Map<String, Integer> weeklyProgress;
    public final Set<String> claims;

    private PlayerMeta(long lastSeen, long idleAt, long idleClaimAt, String loginDay,
                       int loginStreak, int bossAttacks, int bestRating,
                       Map<String, Integer> dailyProgress,
                       Map<String, Integer> weeklyProgress, Set<String> claims) {
        this.lastSeen = lastSeen;
        this.idleAt = idleAt;
        this.idleClaimAt = idleClaimAt;
        this.loginDay = loginDay;
        this.loginStreak = loginStreak;
        this.bossAttacks = bossAttacks;
        this.bestRating = bestRating;
        this.dailyProgress = Collections.unmodifiableMap(dailyProgress);
        this.weeklyProgress = Collections.unmodifiableMap(weeklyProgress);
        this.claims = Collections.unmodifiableSet(claims);
    }

    public static PlayerMeta from(DataSnapshot snapshot) {
        Map<String, Integer> daily = new HashMap<>();
        Map<String, Integer> weekly = new HashMap<>();
        Set<String> claims = new HashSet<>();
        if (snapshot != null) {
            String dayKey = MetaClock.dayKey(System.currentTimeMillis());
            String weekKey = MetaClock.weekKey(System.currentTimeMillis());
            collectProgress(snapshot.child("missions").child("d").child(dayKey), daily);
            collectProgress(snapshot.child("missions").child("w").child(weekKey), weekly);
            for (DataSnapshot child : snapshot.child("claims").getChildren()) {
                if (child.getKey() != null && Boolean.TRUE.equals(child.getValue(Boolean.class))) {
                    claims.add(child.getKey());
                }
            }
            return new PlayerMeta(
                    longOf(snapshot.child("lastSeen").getValue(), 0L),
                    longOf(snapshot.child("idle").child("at").getValue(), 0L),
                    longOf(snapshot.child("idle").child("lastClaim").getValue(), 0L),
                    snapshot.child("login").child("lastDay").getValue(String.class),
                    intOf(snapshot.child("login").child("streak").getValue(), 0),
                    intOf(snapshot.child("bossAttacks").getValue(), 0),
                    intOf(snapshot.child("bestRating").getValue(), 0),
                    daily, weekly, claims);
        }
        return new PlayerMeta(0L, 0L, 0L, null, 0, 0, 0, daily, weekly, claims);
    }

    private static void collectProgress(DataSnapshot root, Map<String, Integer> out) {
        for (DataSnapshot child : root.getChildren()) {
            if (child.getKey() != null && child.getValue() instanceof Number) {
                out.put(child.getKey(), ((Number) child.getValue()).intValue());
            }
        }
    }

    public int progressOf(MissionDef def, long now) {
        if (def == null) {
            return 0;
        }
        Integer value = def.daily() ? dailyProgress.get(def.id) : weeklyProgress.get(def.id);
        return value == null ? 0 : Math.max(0, value);
    }

    public boolean claimed(MissionDef def, long now) {
        return def != null && claims.contains(claimKey(def, now));
    }

    public static String claimKey(MissionDef def, long now) {
        if (def == null) {
            return "";
        }
        String period = def.daily() ? MetaClock.dayKey(now) : MetaClock.weekKey(now);
        return period + "_" + def.id;
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
