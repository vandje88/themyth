package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class PlayerProfile {

    public final String uid;
    public final String username;
    public final String rank;
    public final String titleId;
    public final int level;

    private PlayerProfile(String uid, String username, String rank, String titleId, int level) {
        this.uid = uid;
        this.username = username;
        this.rank = rank;
        this.titleId = titleId;
        this.level = level;
    }

    public static PlayerProfile from(DataSnapshot snapshot, String fallbackUid) {
        String uid = snapshot.child("uid").getValue(String.class);
        String username = snapshot.child("username").getValue(String.class);
        String rank = snapshot.child("rank").getValue(String.class);
        String titleId = snapshot.child("titleId").getValue(String.class);
        Long level = snapshot.child("level").getValue(Long.class);

        return new PlayerProfile(
                uid != null ? uid : fallbackUid,
                username != null ? username : "Pemanggil",
                rank != null ? rank : "bronze",
                titleId != null ? titleId : "rookie_myth",
                level != null ? level.intValue() : 1
        );
    }
}
