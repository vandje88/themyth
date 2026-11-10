package zhaf.media.themyth.battle.engine;

import java.util.ArrayList;
import java.util.List;

public final class BattleUnit {

    public static final int MAX_ENERGY = 100;
    public static final int START_ENERGY = 30;
    public static final int GAUGE_ACTION = 5000;

    public final String uid;
    public final String charId;
    public final String name;
    public final String sprite;
    public final String element;
    public final String role;
    public final int team;
    public final int slot;
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
    public int elemPower;

    public int hp;
    public int gauge;
    public int energy;
    public int shield;
    public int shieldTurns;
    public int shieldAppliedAction;
    public int tauntTurns;
    public boolean alive;
    public boolean stunImmune;
    public int cooldown1;
    public int cooldown2;
    public int energyFromDamageThisTurn;

    public Skill basic;
    public Skill skill1;
    public Skill skill2;
    public Skill ultimate;

    public final List<StatusEffect> effects = new ArrayList<>();

    public long damageDealt;
    public long healingDone;
    public long damageTaken;

    public BattleUnit(String uid, String charId, String name, String sprite,
                      int team, int slot, int level, String element, String role,
                      int maxHp, int atk, int def, int spd,
                      int critRate, int critDmg, int acc, int eva, int res, int elemPower) {
        this.uid = uid;
        this.charId = charId;
        this.name = name;
        this.sprite = sprite;
        this.team = team;
        this.slot = slot;
        this.level = Math.max(1, level);
        this.element = element;
        this.role = role;
        this.maxHp = Math.max(1, maxHp);
        this.atk = Math.max(1, atk);
        this.def = Math.max(0, def);
        this.spd = Math.max(1, spd);
        this.critRate = critRate;
        this.critDmg = critDmg;
        this.acc = acc;
        this.eva = eva;
        this.res = res;
        this.elemPower = elemPower;
        reset();
    }

    public void setSkills(Skill basic, Skill skill1, Skill skill2, Skill ultimate) {
        this.basic = basic;
        this.skill1 = skill1;
        this.skill2 = skill2;
        this.ultimate = ultimate;
    }

    public void reset() {
        hp = maxHp;
        gauge = 0;
        energy = START_ENERGY;
        shield = 0;
        shieldTurns = 0;
        shieldAppliedAction = 0;
        tauntTurns = 0;
        alive = true;
        stunImmune = false;
        cooldown1 = 0;
        cooldown2 = 0;
        energyFromDamageThisTurn = 0;
        effects.clear();
        damageDealt = 0;
        healingDone = 0;
        damageTaken = 0;
    }

    public boolean isPlayer() {
        return team == 0;
    }

    public int hpPctBp() {
        if (maxHp <= 0) {
            return 0;
        }
        return (int) ((long) hp * 10000L / maxHp);
    }

    public int effectiveAtk() {
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.ATK_UP) {
                bonus += effect.value;
            }
        }
        bonus = Math.min(bonus, 3000);
        return (int) ((long) atk * (10000L + bonus) / 10000L);
    }

    public int effectiveDef() {
        int penalty = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.DEF_DOWN) {
                penalty += effect.value;
            }
        }
        long mult = 10000L - Math.min(penalty, 9000);
        return (int) ((long) def * mult / 10000L);
    }

    public int effectiveRes() {
        int penalty = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.WET) {
                penalty += effect.value;
            }
        }
        return Math.max(0, Math.min(9500, res - penalty));
    }

    public int damageTakenBp() {
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.SHOCK) {
                bonus += effect.value;
            }
        }
        return 10000 + bonus;
    }

    public boolean hasStatus(StatusType type) {
        for (StatusEffect effect : effects) {
            if (effect.type == type) {
                return true;
            }
        }
        return false;
    }

    public int statusCount(StatusType type) {
        int count = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == type) {
                count++;
            }
        }
        return count;
    }

    public int dotPerTurn() {
        int total = 0;
        for (StatusEffect effect : effects) {
            if (effect.isDot()) {
                total += effect.value;
            }
        }
        return total;
    }
}
