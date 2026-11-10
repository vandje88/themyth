package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class InventoryItem {

    public final String itemId;
    public final int qty;
    public final boolean locked;

    public InventoryItem(String itemId, int qty, boolean locked) {
        this.itemId = itemId;
        this.qty = qty;
        this.locked = locked;
    }

    public static InventoryItem from(String itemId, DataSnapshot snapshot) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        long qty = longOf(snapshot.child("qty").getValue(), 0);
        if (qty <= 0) {
            return null;
        }
        return new InventoryItem(itemId, (int) Math.min(qty, Integer.MAX_VALUE),
                Boolean.TRUE.equals(snapshot.child("locked").getValue(Boolean.class)));
    }

    private static long longOf(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }
}
