package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RosterChar {

    public final String charId;
    public final int level;
    public final long exp;
    public final int stars;
    public final int awakening;
    public final int breakthrough;
    public final int skillBasic;
    public final int skillOne;
    public final int skillTwo;
    public final int skillUltimate;
    public final Map<String, String> equipment;

    public RosterChar(String charId, int level, long exp, int stars, int awakening,
                      int breakthrough, int skillBasic, int skillOne, int skillTwo,
                      int skillUltimate, Map<String, String> equipment) {
        this.charId = charId;
        this.level = level;
        this.exp = exp;
        this.stars = stars;
        this.awakening = awakening;
        this.breakthrough = breakthrough;
        this.skillBasic = skillBasic;
        this.skillOne = skillOne;
        this.skillTwo = skillTwo;
        this.skillUltimate = skillUltimate;
        this.equipment = Collections.unmodifiableMap(
                equipment == null ? new HashMap<>() : equipment);
    }

    public String equipmentAt(String slot) {
        if (slot == null) {
            return null;
        }
        return equipment.get(slot);
    }

    public int skillLevel(String key) {
        if ("basic".equals(key)) {
            return skillBasic;
        }
        if ("skill1".equals(key)) {
            return skillOne;
        }
        if ("skill2".equals(key)) {
            return skillTwo;
        }
        if ("ultimate".equals(key)) {
            return skillUltimate;
        }
        return 1;
    }

    public static RosterChar from(String charId, DataSnapshot snapshot) {
        if (charId == null || charId.isEmpty()) {
            return null;
        }
        Map<String, String> equipment = new HashMap<>();
        for (DataSnapshot child : snapshot.child("equipment").getChildren()) {
            String instanceId = child.getValue(String.class);
            if (child.getKey() != null && instanceId != null && !instanceId.isEmpty()) {
                equipment.put(child.getKey(), instanceId);
            }
        }
        return new RosterChar(
                charId,
                intOf(snapshot.child("level").getValue(), 1),
                longOf(snapshot.child("exp").getValue(), 0),
                intOf(snapshot.child("stars").getValue(), 1),
                intOf(snapshot.child("awakening").getValue(), 0),
                intOf(snapshot.child("breakthrough").getValue(), 0),
                intOf(snapshot.child("skillLevels").child("basic").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("skill1").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("skill2").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("ultimate").getValue(), 1),
                equipment
        );
    }

    private static int intOf(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }

    private static long longOf(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }
}
