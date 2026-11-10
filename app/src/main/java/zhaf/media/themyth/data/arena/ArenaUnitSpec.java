package zhaf.media.themyth.data.arena;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.battle.engine.BattleUnit;

public final class ArenaUnitSpec {

    public final int slot;
    public final String charId;
    public final String name;
    public final String sprite;
    public final String element;
    public final String role;
    public final int level;
    public final int maxHp;
    public final int atk;
    public final int def;
    public final int spd;
    public final int critRate;
    public final int critDmg;
    public final int acc;
    public final int eva;
    public final int res;
    public final int elemPower;
    public final int basic;
    public final int skillOne;
    public final int skillTwo;
    public final int skillUltimate;

    public ArenaUnitSpec(int slot, String charId, String name, String sprite,
                         String element, String role, int level,
                         int maxHp, int atk, int def, int spd,
                         int critRate, int critDmg, int acc, int eva, int res, int elemPower,
                         int basic, int skillOne, int skillTwo, int skillUltimate) {
        this.slot = slot;
        this.charId = charId;
        this.name = name;
        this.sprite = sprite;
        this.element = element;
        this.role = role;
        this.level = level;
        this.maxHp = maxHp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.critRate = critRate;
        this.critDmg = critDmg;
        this.acc = acc;
        this.eva = eva;
        this.res = res;
        this.elemPower = elemPower;
        this.basic = basic;
        this.skillOne = skillOne;
        this.skillTwo = skillTwo;
        this.skillUltimate = skillUltimate;
    }

    public BattleUnit toBattleUnit(int team, int slotIndex, String uid) {
        return new BattleUnit(uid, charId, name, sprite, team, slotIndex, level,
                element, role, maxHp, atk, def, spd, critRate, critDmg, acc, eva, res, elemPower);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("slot", slot);
        map.put("charId", charId);
        map.put("name", name);
        map.put("sprite", sprite);
        map.put("element", element);
        map.put("role", role);
        map.put("level", level);
        map.put("hp", maxHp);
        map.put("atk", atk);
        map.put("def", def);
        map.put("spd", spd);
        map.put("critRate", critRate);
        map.put("critDmg", critDmg);
        map.put("acc", acc);
        map.put("eva", eva);
        map.put("res", res);
        map.put("elemPower", elemPower);
        map.put("basic", basic);
        map.put("skill1", skillOne);
        map.put("skill2", skillTwo);
        map.put("ultimate", skillUltimate);
        return map;
    }

    public static ArenaUnitSpec from(int slot, DataSnapshot snapshot) {
        String charId = snapshot.child("charId").getValue(String.class);
        if (charId == null || charId.isEmpty()) {
            return null;
        }
        return new ArenaUnitSpec(
                intOf(snapshot.child("slot").getValue(), slot),
                charId,
                text(snapshot.child("name").getValue(String.class), charId),
                text(snapshot.child("sprite").getValue(String.class), null),
                text(snapshot.child("element").getValue(String.class), "wind"),
                text(snapshot.child("role").getValue(String.class), "fighter"),
                intOf(snapshot.child("level").getValue(), 1),
                intOf(snapshot.child("hp").getValue(), 1),
                intOf(snapshot.child("atk").getValue(), 1),
                intOf(snapshot.child("def").getValue(), 0),
                intOf(snapshot.child("spd").getValue(), 1),
                intOf(snapshot.child("critRate").getValue(), 0),
                intOf(snapshot.child("critDmg").getValue(), 15000),
                intOf(snapshot.child("acc").getValue(), 0),
                intOf(snapshot.child("eva").getValue(), 0),
                intOf(snapshot.child("res").getValue(), 0),
                intOf(snapshot.child("elemPower").getValue(), 0),
                intOf(snapshot.child("basic").getValue(), 1),
                intOf(snapshot.child("skill1").getValue(), 1),
                intOf(snapshot.child("skill2").getValue(), 1),
                intOf(snapshot.child("ultimate").getValue(), 1)
        );
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static String text(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
