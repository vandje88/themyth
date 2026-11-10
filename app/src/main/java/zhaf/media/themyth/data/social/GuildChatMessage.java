package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

public final class GuildChatMessage {

    public final String id;
    public final String uid;
    public final String name;
    public final String text;
    public final long at;

    public GuildChatMessage(String id, String uid, String name, String text, long at) {
        this.id = id == null ? "" : id;
        this.uid = uid == null ? "" : uid;
        this.name = name == null || name.isEmpty() ? "Pemanggil" : name;
        this.text = text == null ? "" : text;
        this.at = at;
    }

    public static GuildChatMessage from(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new GuildChatMessage(
                snapshot.getKey(),
                snapshot.child("uid").getValue(String.class),
                snapshot.child("name").getValue(String.class),
                snapshot.child("text").getValue(String.class),
                longOf(snapshot.child("at").getValue(), 0));
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
