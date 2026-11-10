package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class FriendEntry {

    public final String uid;
    public final String name;
    public final long since;
    public final String lastGiftDay;

    public FriendEntry(String uid, String name, long since, String lastGiftDay) {
        this.uid = uid == null ? "" : uid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.since = since;
        this.lastGiftDay = lastGiftDay == null ? "" : lastGiftDay;
    }

    public boolean giftReady(long now) {
        return SocialDay.giftReady(lastGiftDay, now);
    }

    public static FriendEntry from(DataSnapshot snapshot, String fallbackUid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new FriendEntry(
                text(snapshot.child("uid").getValue(String.class), fallbackUid),
                text(snapshot.child("name").getValue(String.class), "Pemanggil"),
                longOf(snapshot.child("since").getValue(), 0),
                text(snapshot.child("lastGiftDay").getValue(String.class), ""));
    }

    private static String text(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
