package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class EquipmentInstance {

    public final String instanceId;
    public final String templateId;
    public final int level;
    public final boolean locked;
    public final String equippedBy;

    public EquipmentInstance(String instanceId, String templateId, int level,
                             boolean locked, String equippedBy) {
        this.instanceId = instanceId;
        this.templateId = templateId;
        this.level = level;
        this.locked = locked;
        this.equippedBy = equippedBy == null ? "" : equippedBy;
    }

    public boolean isEquipped() {
        return !equippedBy.isEmpty();
    }

    public static EquipmentInstance from(String instanceId, DataSnapshot snapshot) {
        if (instanceId == null || instanceId.isEmpty()) {
            return null;
        }
        String templateId = snapshot.child("templateId").getValue(String.class);
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }
        return new EquipmentInstance(
                instanceId,
                templateId,
                intOf(snapshot.child("level").getValue(), 0),
                Boolean.TRUE.equals(snapshot.child("locked").getValue(Boolean.class)),
                snapshot.child("equippedBy").getValue(String.class)
        );
    }

    private static int intOf(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }
}
