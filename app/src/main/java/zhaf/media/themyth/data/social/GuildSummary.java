package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class GuildSummary {

    public final String gid;
    public final String name;
    public final String tag;
    public final int memberCount;
    public final int minLevel;
    public final boolean open;

    public GuildSummary(String gid, String name, String tag, int memberCount,
                        int minLevel, boolean open) {
        this.gid = gid == null ? "" : gid;
        this.name = name == null ? "" : name;
        this.tag = tag == null ? "" : tag;
        this.memberCount = Math.max(0, memberCount);
        this.minLevel = Math.max(1, minLevel);
        this.open = open;
    }

    public static GuildSummary from(DataSnapshot snapshot, String gid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new GuildSummary(
                gid,
                snapshot.child("name").getValue(String.class),
                snapshot.child("tag").getValue(String.class),
                intOf(snapshot.child("memberCount").getValue(), 0),
                intOf(snapshot.child("minLevel").getValue(), 1),
                Boolean.TRUE.equals(snapshot.child("open").getValue(Boolean.class)));
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
}
