package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class FriendRequest {

    public final String uid;
    public final String name;
    public final long at;

    public FriendRequest(String uid, String name, long at) {
        this.uid = uid == null ? "" : uid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.at = at;
    }

    public static FriendRequest from(DataSnapshot snapshot, String fallbackUid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        String uid = snapshot.child("uid").getValue(String.class);
        if (uid == null || uid.isEmpty()) {
            uid = snapshot.child("fromUid").getValue(String.class);
        }
        return new FriendRequest(
                uid == null || uid.isEmpty() ? fallbackUid : uid,
                snapshot.child("name").getValue(String.class),
                longOf(snapshot.child("at").getValue(), 0));
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
