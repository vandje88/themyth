package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class ShopLimit {

    public final int count;
    public final long resetAt;

    public ShopLimit(int count, long resetAt) {
        this.count = count;
        this.resetAt = resetAt;
    }

    public static ShopLimit from(DataSnapshot snapshot) {
        Long count = snapshot.child("count").getValue(Long.class);
        Long resetAt = snapshot.child("resetAt").getValue(Long.class);
        return new ShopLimit(
                count == null ? 0 : count.intValue(),
                resetAt == null ? 0L : resetAt);
    }

    public boolean isFresh(long now) {
        return resetAt <= 0 || now >= resetAt;
    }

    public int remaining(int dailyLimit) {
        return Math.max(0, dailyLimit - count);
    }
}
