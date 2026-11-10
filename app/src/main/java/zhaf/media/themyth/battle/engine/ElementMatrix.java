package zhaf.media.themyth.battle.engine;

public final class ElementMatrix {

    public static final String FIRE = "fire";
    public static final String WATER = "water";
    public static final String WIND = "wind";
    public static final String EARTH = "earth";
    public static final String LIGHTNING = "lightning";

    public static final int STRONG = 12500;
    public static final int NEUTRAL = 10000;
    public static final int WEAK = 8500;

    private ElementMatrix() {
    }

    public static int multiplierBp(String attacker, String defender) {
        if (attacker == null || defender == null) {
            return NEUTRAL;
        }
        switch (attacker) {
            case FIRE:
                return WIND.equals(defender) ? STRONG : (WATER.equals(defender) ? WEAK : NEUTRAL);
            case WATER:
                return FIRE.equals(defender) ? STRONG : (LIGHTNING.equals(defender) ? WEAK : NEUTRAL);
            case WIND:
                return EARTH.equals(defender) ? STRONG : (FIRE.equals(defender) ? WEAK : NEUTRAL);
            case EARTH:
                return LIGHTNING.equals(defender) ? STRONG : (WIND.equals(defender) ? WEAK : NEUTRAL);
            case LIGHTNING:
                return WATER.equals(defender) ? STRONG : (EARTH.equals(defender) ? WEAK : NEUTRAL);
            default:
                return NEUTRAL;
        }
    }
}
