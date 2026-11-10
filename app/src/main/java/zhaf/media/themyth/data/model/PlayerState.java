package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PlayerState {

    public final long coin;
    public final long diamond;
    public final Map<String, RosterChar> roster;
    public final Map<String, InventoryItem> inventory;
    public final Map<String, EquipmentInstance> equipment;

    public PlayerState(long coin, long diamond, Map<String, RosterChar> roster,
                       Map<String, InventoryItem> inventory,
                       Map<String, EquipmentInstance> equipment) {
        this.coin = coin;
        this.diamond = diamond;
        this.roster = Collections.unmodifiableMap(roster);
        this.inventory = Collections.unmodifiableMap(inventory);
        this.equipment = Collections.unmodifiableMap(equipment);
    }

    public static PlayerState from(DataSnapshot player) {
        Map<String, RosterChar> roster = new HashMap<>();
        for (DataSnapshot child : player.child("roster").getChildren()) {
            RosterChar owned = RosterChar.from(child.getKey(), child);
            if (owned != null) {
                roster.put(owned.charId, owned);
            }
        }

        Map<String, InventoryItem> inventory = new HashMap<>();
        for (DataSnapshot child : player.child("inventory").getChildren()) {
            InventoryItem item = InventoryItem.from(child.getKey(), child);
            if (item != null) {
                inventory.put(item.itemId, item);
            }
        }

        Map<String, EquipmentInstance> equipment = new HashMap<>();
        for (DataSnapshot child : player.child("equipment").getChildren()) {
            EquipmentInstance instance = EquipmentInstance.from(child.getKey(), child);
            if (instance != null) {
                equipment.put(instance.instanceId, instance);
            }
        }

        return new PlayerState(
                longOf(player.child("currencies").child("coin").getValue(), 0),
                longOf(player.child("currencies").child("diamond").getValue(), 0),
                roster, inventory, equipment);
    }

    public RosterChar charOf(String charId) {
        return charId == null ? null : roster.get(charId);
    }

    public int itemQty(String itemId) {
        InventoryItem item = itemId == null ? null : inventory.get(itemId);
        return item == null ? 0 : item.qty;
    }

    private static long longOf(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }
}
