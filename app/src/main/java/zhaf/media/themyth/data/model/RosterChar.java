package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

public final class RosterChar {

    public final String charId;
    public final int level;
    public final int stars;
    public final int awakening;
    public final int breakthrough;
    public final int skillBasic;
    public final int skillOne;
    public final int skillTwo;
    public final int skillUltimate;

    public RosterChar(String charId, int level, int stars, int awakening, int breakthrough,
                      int skillBasic, int skillOne, int skillTwo, int skillUltimate) {
        this.charId = charId;
        this.level = level;
        this.stars = stars;
        this.awakening = awakening;
        this.breakthrough = breakthrough;
        this.skillBasic = skillBasic;
        this.skillOne = skillOne;
        this.skillTwo = skillTwo;
        this.skillUltimate = skillUltimate;
    }

    public static RosterChar from(String charId, DataSnapshot snapshot) {
        if (charId == null || charId.isEmpty()) {
            return null;
        }
        return new RosterChar(
                charId,
                intOf(snapshot.child("level").getValue(), 1),
                intOf(snapshot.child("stars").getValue(), 1),
                intOf(snapshot.child("awakening").getValue(), 0),
                intOf(snapshot.child("breakthrough").getValue(), 0),
                intOf(snapshot.child("skillLevels").child("basic").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("skill1").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("skill2").getValue(), 1),
                intOf(snapshot.child("skillLevels").child("ultimate").getValue(), 1)
        );
    }

    private static int intOf(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }
}
