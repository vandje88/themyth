package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class PlayerProfile {

    public final String uid;
    public final String username;
    public final String rank;
    public final String titleId;
    public final String badgeId;
    public final String guildId;
    public final int level;
    public final long lastOnline;

    private PlayerProfile(String uid, String username, String rank, String titleId,
                          String badgeId, String guildId, int level, long lastOnline) {
        this.uid = uid;
        this.username = username;
        this.rank = rank;
        this.titleId = titleId;
        this.badgeId = badgeId;
        this.guildId = guildId;
        this.level = level;
        this.lastOnline = lastOnline;
    }

    public static PlayerProfile from(DataSnapshot snapshot, String fallbackUid) {
        String uid = snapshot.child("uid").getValue(String.class);
        String username = snapshot.child("username").getValue(String.class);
        String rank = snapshot.child("rank").getValue(String.class);
        String titleId = snapshot.child("titleId").getValue(String.class);
        String badgeId = snapshot.child("badgeId").getValue(String.class);
        String guildId = snapshot.child("guildId").getValue(String.class);
        Long level = snapshot.child("level").getValue(Long.class);
        Long lastOnline = snapshot.child("lastOnline").getValue(Long.class);

        return new PlayerProfile(
                uid != null ? uid : fallbackUid,
                username != null ? username : "Pemanggil",
                rank != null ? rank : "bronze",
                titleId != null ? titleId : "rookie_myth",
                badgeId != null ? badgeId : "none",
                guildId,
                level != null ? level.intValue() : 1,
                lastOnline != null ? lastOnline : 0L
        );
    }
}
