package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class GuildMember {

    public final String uid;
    public final String name;
    public final String role;
    public final long joinedAt;
    public final int weeklyPoints;
    public final int totalPoints;

    public GuildMember(String uid, String name, String role, long joinedAt,
                       int weeklyPoints, int totalPoints) {
        this.uid = uid == null ? "" : uid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.role = role == null ? GuildLogic.ROLE_MEMBER : role;
        this.joinedAt = joinedAt;
        this.weeklyPoints = Math.max(0, weeklyPoints);
        this.totalPoints = Math.max(0, totalPoints);
    }

    public boolean isLeader() {
        return GuildLogic.ROLE_LEADER.equals(role);
    }

    public static GuildMember from(DataSnapshot snapshot, String fallbackUid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new GuildMember(
                fallbackUid,
                snapshot.child("name").getValue(String.class),
                snapshot.child("role").getValue(String.class),
                longOf(snapshot.child("joinedAt").getValue(), 0),
                intOf(snapshot.child("weeklyPoints").getValue(), 0),
                intOf(snapshot.child("totalPoints").getValue(), 0));
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
