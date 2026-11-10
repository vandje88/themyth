package zhaf.media.themyth.battle.engine;

public final class BattleEvent {

    public enum Type {
        START,
        ACTION,
        HIT,
        DOT,
        MISS,
        HEAL,
        SHIELD,
        STATUS,
        DEATH,
        END
    }

    public final Type type;
    public final int tick;
    public final String srcUid;
    public final String srcName;
    public final String dstUid;
    public final String dstName;
    public final String skillName;
    public final String skillType;
    public final String detail;
    public final int value;
    public final int hpAfter;
    public final int energyAfter;
    public final boolean crit;
    public final boolean success;

    private BattleEvent(Type type, int tick, String srcUid, String srcName,
                        String dstUid, String dstName, String skillName, String skillType,
                        String detail, int value, int hpAfter, int energyAfter,
                        boolean crit, boolean success) {
        this.type = type;
        this.tick = tick;
        this.srcUid = srcUid;
        this.srcName = srcName;
        this.dstUid = dstUid;
        this.dstName = dstName;
        this.skillName = skillName;
        this.skillType = skillType;
        this.detail = detail;
        this.value = value;
        this.hpAfter = hpAfter;
        this.energyAfter = energyAfter;
        this.crit = crit;
        this.success = success;
    }

    public static BattleEvent start(int tick) {
        return new BattleEvent(Type.START, tick, null, null, null, null,
                null, null, null, 0, 0, 0, false, true);
    }

    public static BattleEvent action(int tick, BattleUnit actor, Skill skill,
                                     BattleUnit primary, int energyAfter) {
        return new BattleEvent(Type.ACTION, tick, actor.uid, actor.name,
                primary == null ? null : primary.uid,
                primary == null ? null : primary.name,
                skill.name, skill.type.name(), skill.target.name(),
                0, actor.hp, energyAfter, false, true);
    }

    public static BattleEvent hit(int tick, BattleUnit src, BattleUnit dst, Skill skill,
                                  int damage, boolean crit) {
        return new BattleEvent(Type.HIT, tick, src.uid, src.name, dst.uid, dst.name,
                skill == null ? null : skill.name,
                skill == null ? null : skill.type.name(),
                null, damage, dst.hp, dst.energy, crit, true);
    }

    public static BattleEvent dot(int tick, BattleUnit src, BattleUnit dst,
                                  StatusEffect effect, int damage) {
        return new BattleEvent(Type.DOT, tick,
                src == null ? null : src.uid, src == null ? null : src.name,
                dst.uid, dst.name, null, null, effect.type.name(),
                damage, dst.hp, dst.energy, false, true);
    }

    public static BattleEvent miss(int tick, BattleUnit src, BattleUnit dst, Skill skill) {
        return new BattleEvent(Type.MISS, tick, src.uid, src.name, dst.uid, dst.name,
                skill == null ? null : skill.name,
                skill == null ? null : skill.type.name(),
                null, 0, dst.hp, dst.energy, false, false);
    }

    public static BattleEvent heal(int tick, BattleUnit src, BattleUnit dst,
                                   Skill skill, int amount) {
        return new BattleEvent(Type.HEAL, tick, src.uid, src.name, dst.uid, dst.name,
                skill == null ? null : skill.name,
                skill == null ? null : skill.type.name(),
                null, amount, dst.hp, dst.energy, false, true);
    }

    public static BattleEvent shield(int tick, BattleUnit src, BattleUnit dst,
                                     Skill skill, int amount) {
        return new BattleEvent(Type.SHIELD, tick, src.uid, src.name, dst.uid, dst.name,
                skill == null ? null : skill.name,
                skill == null ? null : skill.type.name(),
                null, amount, dst.hp, dst.energy, false, true);
    }

    public static BattleEvent status(int tick, BattleUnit src, BattleUnit dst,
                                     StatusType status, int turns, boolean success) {
        return new BattleEvent(Type.STATUS, tick,
                src == null ? null : src.uid, src == null ? null : src.name,
                dst.uid, dst.name, null, null, status.name(),
                turns, dst.hp, dst.energy, false, success);
    }

    public static BattleEvent death(int tick, BattleUnit dst) {
        return new BattleEvent(Type.DEATH, tick, null, null, dst.uid, dst.name,
                null, null, null, 0, 0, dst.energy, false, true);
    }

    public static BattleEvent end(int tick, int winner, String reason) {
        return new BattleEvent(Type.END, tick, null, null, null, null,
                null, null, reason, winner, 0, 0, false, true);
    }
}
