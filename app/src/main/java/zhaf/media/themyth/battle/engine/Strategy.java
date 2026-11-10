package zhaf.media.themyth.battle.engine;

public enum Strategy {
    BALANCED,
    FOCUS_BACKLINE,
    PROTECT_ALLY,
    BURST;

    public static Strategy from(String raw) {
        if (raw == null) {
            return BALANCED;
        }
        for (Strategy value : values()) {
            if (value.name().equals(raw)) {
                return value;
            }
        }
        return BALANCED;
    }
}
