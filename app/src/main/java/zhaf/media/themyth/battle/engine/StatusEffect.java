package zhaf.media.themyth.battle.engine;

public final class StatusEffect {

    public final StatusType type;
    public final int value;
    public int turns;
    int appliedAction;
    BattleUnit source;

    public StatusEffect(StatusType type, int value, int turns, int appliedAction) {
        this.type = type;
        this.value = value;
        this.turns = Math.max(1, turns);
        this.appliedAction = appliedAction;
    }

    public boolean isDot() {
        return type == StatusType.BURN || type == StatusType.BLEED;
    }
}
