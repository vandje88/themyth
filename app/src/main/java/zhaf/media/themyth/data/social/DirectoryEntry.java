package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class DirectoryEntry {

    public final String uid;
    public final String name;
    public final String avatar;
    public final int level;
    public final String guildId;
    public final long updatedAt;

    public DirectoryEntry(String uid, String name, String avatar, int level,
                          String guildId, long updatedAt) {
        this.uid = uid == null ? "" : uid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.avatar = avatar == null || avatar.isEmpty() ? "P" : avatar;
        this.level = Math.max(1, level);
        this.guildId = guildId;
        this.updatedAt = updatedAt;
    }

    public static DirectoryEntry from(DataSnapshot snapshot, String fallbackUid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new DirectoryEntry(
                fallbackUid,
                snapshot.child("name").getValue(String.class),
                snapshot.child("avatar").getValue(String.class),
                intOf(snapshot.child("level").getValue(), 1),
                snapshot.child("guildId").getValue(String.class),
                longOf(snapshot.child("updatedAt").getValue(), 0));
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
