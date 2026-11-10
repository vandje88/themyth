package zhaf.media.themyth.data.arena;

import java.util.ArrayList;
import java.util.List;

public final class ArenaMatchmaker {

    public static final int BASE_WINDOW = 150;
    public static final int MAX_WINDOW = 600;
    public static final long WIDEN_STEP_MS = 10_000L;

    private ArenaMatchmaker() {
    }

    public static int window(long searchingMs) {
        long steps = Math.max(0L, searchingMs) / WIDEN_STEP_MS;
        long window = BASE_WINDOW * Math.max(1L, steps + 1L);
        return (int) Math.min(MAX_WINDOW, window);
    }

    public static ArenaSnapshot pick(List<ArenaSnapshot> candidates, String selfUid,
                                     int rating, int cp, long searchingMs) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int window = window(searchingMs);
        List<ArenaSnapshot> list = new ArrayList<>();
        for (ArenaSnapshot candidate : candidates) {
            if (candidate == null || candidate.uid == null || candidate.uid.equals(selfUid)) {
                continue;
            }
            if (!candidate.isUsable() || Math.abs(candidate.rating - rating) > window) {
                continue;
            }
            list.add(candidate);
        }
        list.sort((left, right) -> {
            int ratingDiff = Integer.compare(Math.abs(left.rating - rating),
                    Math.abs(right.rating - rating));
            if (ratingDiff != 0) {
                return ratingDiff;
            }
            int cpDiff = Long.compare(Math.abs(left.cp - cp), Math.abs(right.cp - cp));
            if (cpDiff != 0) {
                return cpDiff;
            }
            return compareUid(left.uid, right.uid);
        });
        return list.isEmpty() ? null : list.get(0);
    }

    public static ArenaSnapshot botFor(ArenaSnapshot self) {
        if (self == null || self.units.isEmpty()) {
            return null;
        }
        long cp = Math.max(1L, Math.round(self.cp * 0.95));
        List<ArenaUnitSpec> units = new ArrayList<>();
        for (ArenaUnitSpec unit : self.units) {
            units.add(new ArenaUnitSpec(unit.slot, unit.charId, unit.name, unit.sprite,
                    unit.element, unit.role, unit.level, unit.maxHp, unit.atk, unit.def,
                    unit.spd, unit.critRate, unit.critDmg, unit.acc, unit.eva, unit.res,
                    unit.elemPower, unit.basic, unit.skillOne, unit.skillTwo, unit.skillUltimate));
        }
        return new ArenaSnapshot("bot_" + self.uid, "Penjaga Arena", "P", self.title,
                cp, self.rating, self.strategy, units, self.updatedAt);
    }

    public static boolean isBot(String uid) {
        return uid != null && uid.startsWith("bot_");
    }

    private static int compareUid(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        return a.compareTo(b);
    }
}
