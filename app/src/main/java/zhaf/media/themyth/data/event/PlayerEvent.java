package zhaf.media.themyth.data.event;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerEvent {

    public final String eventId;
    public final int coin;
    public final Map<String, Integer> progress;
    public final Set<String> claims;
    public final Map<String, Integer> redeemed;

    private PlayerEvent(String eventId, int coin, Map<String, Integer> progress,
                        Set<String> claims, Map<String, Integer> redeemed) {
        this.eventId = eventId;
        this.coin = Math.max(0, coin);
        this.progress = Collections.unmodifiableMap(progress);
        this.claims = Collections.unmodifiableSet(claims);
        this.redeemed = Collections.unmodifiableMap(redeemed);
    }

    public static PlayerEvent from(String eventId, DataSnapshot snapshot) {
        Map<String, Integer> progress = new HashMap<>();
        Set<String> claims = new HashSet<>();
        Map<String, Integer> redeemed = new HashMap<>();
        int coin = 0;
        if (snapshot != null) {
            coin = intOf(snapshot.child("coin").getValue(), 0);
            for (DataSnapshot child : snapshot.child("progress").getChildren()) {
                if (child.getKey() != null && child.getValue() instanceof Number) {
                    progress.put(child.getKey(), ((Number) child.getValue()).intValue());
                }
            }
            for (DataSnapshot child : snapshot.child("claims").getChildren()) {
                if (child.getKey() != null && Boolean.TRUE.equals(child.getValue(Boolean.class))) {
                    claims.add(child.getKey());
                }
            }
            for (DataSnapshot child : snapshot.child("redeemed").getChildren()) {
                if (child.getKey() != null && child.getValue() instanceof Number) {
                    redeemed.put(child.getKey(), ((Number) child.getValue()).intValue());
                }
            }
        }
        return new PlayerEvent(eventId, coin, progress, claims, redeemed);
    }

    public int progressOf(EventMission mission) {
        if (mission == null) {
            return 0;
        }
        Integer value = progress.get(mission.id);
        return value == null ? 0 : Math.max(0, value);
    }

    public boolean claimed(EventMission mission) {
        return mission != null && claims.contains(mission.id);
    }

    public int redeemedOf(EventReward reward) {
        if (reward == null) {
            return 0;
        }
        Integer value = redeemed.get(reward.id);
        return value == null ? 0 : Math.max(0, value);
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
}
