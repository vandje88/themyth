package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class GiftEntry {

    public final String fromUid;
    public final String name;
    public final String day;
    public final long at;

    public GiftEntry(String fromUid, String name, String day, long at) {
        this.fromUid = fromUid == null ? "" : fromUid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.day = day == null ? "" : day;
        this.at = at;
    }

    public String key() {
        return FriendLogic.giftKey(fromUid, day);
    }

    public static GiftEntry from(DataSnapshot snapshot, String fallbackUid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new GiftEntry(
                fallbackUid,
                snapshot.child("name").getValue(String.class),
                snapshot.child("day").getValue(String.class),
                longOf(snapshot.child("at").getValue(), 0));
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
