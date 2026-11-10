package zhaf.media.themyth.data.repository;

import android.content.Context;

import androidx.annotation.StringRes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.progression.Progression;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentTemplate;
import zhaf.media.themyth.data.staticdata.ItemCatalog;
import zhaf.media.themyth.data.staticdata.ItemTemplate;

public final class ProgressionRepository {

    public interface StateCallback {
        void onData(PlayerState state);

        void onError(DatabaseError error);
    }

    public interface ResultCallback {
        void onSuccess();

        void onError(@StringRes int messageRes);
    }

    private interface Mutation {
        @StringRes
        int apply(MutableData player);
    }

    private final CharacterCatalog characters;
    private final ItemCatalog items;
    private final EquipmentCatalog equipments;

    public ProgressionRepository(Context context) {
        Context app = context.getApplicationContext();
        this.characters = CharacterCatalog.get(app);
        this.items = ItemCatalog.get(app);
        this.equipments = EquipmentCatalog.get(app);
    }

    public static Handle observe(String uid, StateCallback callback) {
        DatabaseReference ref = FirebaseRefs.player(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onData(PlayerState.from(snapshot));
                } else {
                    callback.onData(new PlayerState(0, 0,
                            new HashMap<>(), new HashMap<>(), new HashMap<>()));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public void levelUp(String uid, String charId, ResultCallback callback) {
        mutate(uid, player -> {
            CharacterTemplate template = characters.find(charId);
            MutableData owned = ownedChar(player, charId);
            if (template == null || owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            int level = intOf(owned.child("level"), 1);
            int breakthrough = intOf(owned.child("breakthrough"), 0);
            int cap = Progression.levelCap(template.rarity, breakthrough);
            if (level >= cap) {
                return R.string.progression_level_cap;
            }
            long exp = longOf(owned.child("exp"), 0);
            long need = Progression.expToNext(level);
            if (exp < need) {
                return R.string.progression_not_enough_exp;
            }
            long cost = Progression.levelUpCoin(level);
            MutableData coin = coin(player);
            long balance = longOf(coin, 0);
            if (balance < cost) {
                return R.string.progression_not_enough_coin;
            }
            coin.setValue(balance - cost);
            owned.child("level").setValue(level + 1);
            owned.child("exp").setValue(exp - need);
            return 0;
        }, callback);
    }

    public void useExpItem(String uid, String charId, String itemId, int count,
                           ResultCallback callback) {
        mutate(uid, player -> {
            CharacterTemplate template = characters.find(charId);
            MutableData owned = ownedChar(player, charId);
            ItemTemplate item = items.find(itemId);
            if (template == null || owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            if (item == null || !item.isExpMaterial() || count <= 0) {
                return R.string.progression_no_item;
            }
            int level = intOf(owned.child("level"), 1);
            int breakthrough = intOf(owned.child("breakthrough"), 0);
            if (level >= Progression.levelCap(template.rarity, breakthrough)) {
                return R.string.progression_level_cap;
            }
            if (itemQty(player, itemId) < count) {
                return R.string.progression_not_enough_item;
            }
            long exp = longOf(owned.child("exp"), 0) + (long) item.expValue * count;
            owned.child("exp").setValue(exp);
            takeItem(player, itemId, count);
            return 0;
        }, callback);
    }

    public void breakthrough(String uid, String charId, ResultCallback callback) {
        mutate(uid, player -> {
            CharacterTemplate template = characters.find(charId);
            MutableData owned = ownedChar(player, charId);
            if (template == null || owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            int tier = intOf(owned.child("breakthrough"), 0);
            if (!Progression.canBreakthrough(tier)) {
                return R.string.progression_breakthrough_max;
            }
            int level = intOf(owned.child("level"), 1);
            if (level < Progression.breakthroughLevelReq(tier)) {
                return R.string.progression_breakthrough_level;
            }
            long cost = Progression.breakthroughCoin(tier);
            MutableData coin = coin(player);
            long balance = longOf(coin, 0);
            if (balance < cost) {
                return R.string.progression_not_enough_coin;
            }
            String stoneId = Progression.elementStoneItemId(template.element);
            int stones = Progression.breakthroughStones(tier);
            if (itemQty(player, stoneId) < stones) {
                return R.string.progression_not_enough_item;
            }
            coin.setValue(balance - cost);
            takeItem(player, stoneId, stones);
            owned.child("breakthrough").setValue(tier + 1);
            return 0;
        }, callback);
    }

    public void upgradeSkill(String uid, String charId, String skillKey,
                             ResultCallback callback) {
        mutate(uid, player -> {
            CharacterTemplate template = characters.find(charId);
            MutableData owned = ownedChar(player, charId);
            if (template == null || owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            if (!isValidSkill(skillKey)) {
                return R.string.progression_failed;
            }
            MutableData skill = owned.child("skillLevels").child(skillKey);
            int current = intOf(skill, 1);
            if (current >= Progression.MAX_SKILL_LEVEL) {
                return R.string.progression_skill_max;
            }
            long cost = Progression.skillUpgradeCoin(current);
            int scrolls = Progression.skillUpgradeScrolls(current);
            MutableData coin = coin(player);
            long balance = longOf(coin, 0);
            if (balance < cost) {
                return R.string.progression_not_enough_coin;
            }
            if (itemQty(player, "scroll_skill") < scrolls) {
                return R.string.progression_not_enough_item;
            }
            coin.setValue(balance - cost);
            takeItem(player, "scroll_skill", scrolls);
            skill.setValue(current + 1);
            return 0;
        }, callback);
    }

    public void enhance(String uid, String instanceId, ResultCallback callback) {
        mutate(uid, player -> {
            MutableData node = player.child("equipment").child(instanceId);
            if (node.getValue() == null) {
                return R.string.progression_no_equipment;
            }
            EquipmentTemplate template = equipments.find(
                    node.child("templateId").getValue(String.class));
            if (template == null) {
                return R.string.progression_no_equipment;
            }
            int level = intOf(node.child("level"), 0);
            if (level >= Progression.MAX_ENHANCE_LEVEL) {
                return R.string.progression_enhance_max;
            }
            int next = level + 1;
            long cost = Progression.enhanceCoin(next);
            int stones = Progression.enhanceStones(next);
            MutableData coin = coin(player);
            long balance = longOf(coin, 0);
            if (balance < cost) {
                return R.string.progression_not_enough_coin;
            }
            if (itemQty(player, "stone_enhance") < stones) {
                return R.string.progression_not_enough_item;
            }
            coin.setValue(balance - cost);
            takeItem(player, "stone_enhance", stones);
            node.child("level").setValue(next);
            return 0;
        }, callback);
    }

    public void equip(String uid, String charId, String instanceId, ResultCallback callback) {
        mutate(uid, player -> {
            MutableData owned = ownedChar(player, charId);
            MutableData node = player.child("equipment").child(instanceId);
            if (owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            if (node.getValue() == null) {
                return R.string.progression_no_equipment;
            }
            String templateId = node.child("templateId").getValue(String.class);
            EquipmentTemplate template = equipments.find(templateId);
            if (template == null) {
                return R.string.progression_no_equipment;
            }
            if (Boolean.TRUE.equals(node.child("locked").getValue(Boolean.class))) {
                return R.string.progression_locked;
            }
            String equippedBy = node.child("equippedBy").getValue(String.class);
            if (equippedBy != null && !equippedBy.isEmpty() && !equippedBy.equals(charId)) {
                return R.string.progression_equipped;
            }
            MutableData slotNode = owned.child("equipment").child(template.slot);
            String previous = slotNode.getValue(String.class);
            if (previous != null && !previous.isEmpty() && !previous.equals(instanceId)) {
                player.child("equipment").child(previous).child("equippedBy").setValue(null);
            }
            slotNode.setValue(instanceId);
            node.child("equippedBy").setValue(charId);
            node.child("slot").setValue(template.slot);
            return 0;
        }, callback);
    }

    public void unequip(String uid, String charId, String slot, ResultCallback callback) {
        mutate(uid, player -> {
            MutableData owned = ownedChar(player, charId);
            if (owned.getValue() == null) {
                return R.string.progression_no_char;
            }
            MutableData slotNode = owned.child("equipment").child(slot);
            String instanceId = slotNode.getValue(String.class);
            if (instanceId == null || instanceId.isEmpty()) {
                return 0;
            }
            slotNode.setValue(null);
            player.child("equipment").child(instanceId).child("equippedBy").setValue(null);
            return 0;
        }, callback);
    }

    public void sellItem(String uid, String itemId, int count, ResultCallback callback) {
        mutate(uid, player -> {
            ItemTemplate item = items.find(itemId);
            if (item == null || item.sellPrice <= 0 || count <= 0) {
                return R.string.progression_no_item;
            }
            MutableData node = player.child("inventory").child(itemId);
            if (Boolean.TRUE.equals(node.child("locked").getValue(Boolean.class))) {
                return R.string.progression_locked;
            }
            if (intOf(node.child("qty"), 0) < count) {
                return R.string.progression_not_enough_item;
            }
            takeItem(player, itemId, count);
            addCoin(player, (long) item.sellPrice * count);
            return 0;
        }, callback);
    }

    public void sellEquipment(String uid, String instanceId, ResultCallback callback) {
        mutate(uid, player -> {
            MutableData node = player.child("equipment").child(instanceId);
            if (node.getValue() == null) {
                return R.string.progression_no_equipment;
            }
            EquipmentTemplate template = equipments.find(
                    node.child("templateId").getValue(String.class));
            if (template == null) {
                return R.string.progression_no_equipment;
            }
            String equippedBy = node.child("equippedBy").getValue(String.class);
            if (equippedBy != null && !equippedBy.isEmpty()) {
                return R.string.progression_equipped;
            }
            if (Boolean.TRUE.equals(node.child("locked").getValue(Boolean.class))) {
                return R.string.progression_locked;
            }
            node.setValue(null);
            addCoin(player, template.sellPrice);
            return 0;
        }, callback);
    }

    private void mutate(String uid, Mutation mutation, ResultCallback callback) {
        final int[] failure = new int[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                failure[0] = 0;
                if (player.getValue() == null) {
                    failure[0] = R.string.progression_failed;
                    return Transaction.abort();
                }
                int result = mutation.apply(player);
                if (result != 0) {
                    failure[0] = result;
                    return Transaction.abort();
                }
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (committed) {
                    callback.onSuccess();
                } else {
                    callback.onError(failure[0] != 0
                            ? failure[0] : R.string.progression_failed);
                }
            }
        });
    }

    private static MutableData ownedChar(MutableData player, String charId) {
        return player.child("roster").child(charId == null ? "" : charId);
    }

    private static MutableData coin(MutableData player) {
        return player.child("currencies").child("coin");
    }

    private static void addCoin(MutableData player, long amount) {
        MutableData coin = coin(player);
        coin.setValue(longOf(coin, 0) + amount);
    }

    private static int itemQty(MutableData player, String itemId) {
        if (itemId == null) {
            return 0;
        }
        return intOf(player.child("inventory").child(itemId).child("qty"), 0);
    }

    private static void takeItem(MutableData player, String itemId, int count) {
        MutableData node = player.child("inventory").child(itemId);
        int remaining = intOf(node.child("qty"), 0) - count;
        if (remaining <= 0) {
            node.setValue(null);
        } else {
            node.child("qty").setValue(remaining);
        }
    }

    private static boolean isValidSkill(String skillKey) {
        if (skillKey == null) {
            return false;
        }
        for (String key : Progression.SKILL_KEYS) {
            if (key.equals(skillKey)) {
                return true;
            }
        }
        return false;
    }

    private static int intOf(MutableData data, int fallback) {
        Object value = data.getValue();
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }

    private static long longOf(MutableData data, long fallback) {
        Object value = data.getValue();
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }

    public static final class Handle {
        private final DatabaseReference ref;
        private final ValueEventListener listener;

        Handle(DatabaseReference ref, ValueEventListener listener) {
            this.ref = ref;
            this.listener = listener;
        }

        public void detach() {
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
    }
}
