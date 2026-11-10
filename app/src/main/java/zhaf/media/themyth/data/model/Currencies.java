package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class Currencies {

    public final long coin;
    public final long diamond;
    public final long energy;

    private Currencies(long coin, long diamond, long energy) {
        this.coin = coin;
        this.diamond = diamond;
        this.energy = energy;
    }

    public static Currencies from(DataSnapshot snapshot) {
        return new Currencies(
                value(snapshot, "coin"),
                value(snapshot, "diamond"),
                value(snapshot, "energy")
        );
    }

    private static long value(DataSnapshot snapshot, String key) {
        Long raw = snapshot.child(key).getValue(Long.class);
        return raw != null ? raw : 0L;
    }
}
