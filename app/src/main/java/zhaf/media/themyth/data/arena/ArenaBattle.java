package zhaf.media.themyth.data.arena;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

public final class ArenaBattle {

    public final String bid;
    public final String aUid;
    public final String bUid;
    public final long seed;
    public final String status;
    public final ArenaSnapshot a;
    public final ArenaSnapshot b;
    public final boolean settled;
    public final int winner;
    public final int ticks;
    public final int hpA;
    public final int hpB;
    public final String hash;

    public ArenaBattle(String bid, String aUid, String bUid, long seed, String status,
                       ArenaSnapshot a, ArenaSnapshot b, boolean settled,
                       int winner, int ticks, int hpA, int hpB, String hash) {
        this.bid = bid;
        this.aUid = aUid;
        this.bUid = bUid;
        this.seed = seed;
        this.status = status == null ? "PENDING" : status;
        this.a = a;
        this.b = b;
        this.settled = settled;
        this.winner = winner;
        this.ticks = ticks;
        this.hpA = hpA;
        this.hpB = hpB;
        this.hash = hash;
    }

    public static ArenaBattle from(DataSnapshot snapshot, String bid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        DataSnapshot result = snapshot.child("result");
        boolean settled = result.exists();
        return new ArenaBattle(
                bid,
                snapshot.child("aUid").getValue(String.class),
                snapshot.child("bUid").getValue(String.class),
                longOf(snapshot.child("seed").getValue(), 0L),
                snapshot.child("status").getValue(String.class),
                ArenaSnapshot.from(snapshot.child("a")),
                ArenaSnapshot.from(snapshot.child("b")),
                settled,
                (int) longOf(result.child("winner").getValue(), -2),
                (int) longOf(result.child("ticks").getValue(), 0),
                (int) longOf(result.child("hpA").getValue(), 0),
                (int) longOf(result.child("hpB").getValue(), 0),
                result.child("hash").getValue(String.class)
        );
    }

    public Map<String, Object> toCreateMap(long now) {
        Map<String, Object> map = new HashMap<>();
        map.put("mode", "ARENA");
        map.put("aUid", aUid);
        map.put("bUid", bUid);
        map.put("seed", seed);
        map.put("status", "PENDING");
        map.put("createdAt", now);
        map.put("a", a.toMap());
        map.put("b", b.toMap());
        return map;
    }

    public Map<String, Object> toResultMap(int winner, int ticks, int hpA, int hpB,
                                           String hash, long now) {
        Map<String, Object> map = new HashMap<>();
        map.put("winner", winner);
        map.put("ticks", ticks);
        map.put("hpA", hpA);
        map.put("hpB", hpB);
        map.put("hash", hash);
        map.put("by", aUid);
        map.put("at", now);
        return map;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
