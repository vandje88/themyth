package zhaf.media.themyth.data.arena;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArenaSnapshot {

    public final String uid;
    public final String username;
    public final String avatar;
    public final String title;
    public final long cp;
    public final int rating;
    public final String strategy;
    public final List<ArenaUnitSpec> units;
    public final long updatedAt;

    public ArenaSnapshot(String uid, String username, String avatar, String title,
                         long cp, int rating, String strategy,
                         List<ArenaUnitSpec> units, long updatedAt) {
        this.uid = uid;
        this.username = username == null || username.isEmpty() ? "Pemanggil" : username;
        this.avatar = avatar == null || avatar.isEmpty() ? "P" : avatar;
        this.title = title == null ? "" : title;
        this.cp = cp;
        this.rating = rating;
        this.strategy = strategy == null || strategy.isEmpty() ? "BALANCED" : strategy;
        this.units = Collections.unmodifiableList(
                units == null ? Collections.emptyList() : new ArrayList<>(units));
        this.updatedAt = updatedAt;
    }

    public boolean isUsable() {
        return units.size() >= 3;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("username", username);
        map.put("avatar", avatar);
        map.put("title", title);
        map.put("cp", cp);
        map.put("rating", rating);
        map.put("strategy", strategy);
        map.put("updatedAt", updatedAt);
        Map<String, Object> unitMap = new HashMap<>();
        for (ArenaUnitSpec unit : units) {
            unitMap.put(String.valueOf(unit.slot), unit.toMap());
        }
        map.put("units", unitMap);
        return map;
    }

    public static ArenaSnapshot from(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        List<ArenaUnitSpec> units = new ArrayList<>();
        for (DataSnapshot child : snapshot.child("units").getChildren()) {
            int slot = parseInt(child.getKey(), units.size());
            ArenaUnitSpec spec = ArenaUnitSpec.from(slot, child);
            if (spec != null) {
                units.add(spec);
            }
        }
        units.sort((left, right) -> Integer.compare(left.slot, right.slot));
        return new ArenaSnapshot(
                snapshot.child("uid").getValue(String.class),
                snapshot.child("username").getValue(String.class),
                snapshot.child("avatar").getValue(String.class),
                snapshot.child("title").getValue(String.class),
                longOf(snapshot.child("cp").getValue(), 0L),
                (int) longOf(snapshot.child("rating").getValue(), ArenaRating.START_RATING),
                snapshot.child("strategy").getValue(String.class),
                units,
                longOf(snapshot.child("updatedAt").getValue(), 0L)
        );
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
