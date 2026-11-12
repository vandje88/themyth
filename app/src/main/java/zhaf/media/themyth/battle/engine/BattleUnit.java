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
    public boolean ultimate2Next;
    public int cooldown1;
    public int cooldown2;
    public int energyFromDamageThisTurn;

    public Skill basic;
    public Skill skill1;
    public Skill skill2;
    public Skill ultimate;
    public Skill ultimate2;

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
        setSkills(basic, skill1, skill2, ultimate, null);
    }

    public void setSkills(Skill basic, Skill skill1, Skill skill2, Skill ultimate,
                          Skill ultimate2) {
        this.basic = basic;
        this.skill1 = skill1;
        this.skill2 = skill2;
        this.ultimate = ultimate;
        this.ultimate2 = ultimate2;
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
        ultimate2Next = false;
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
        int penalty = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.ATK_UP) {
                bonus += effect.value;
            } else if (effect.type == StatusType.ATK_DOWN) {
                penalty += effect.value;
            }
        }
        long mult = 10000L + Math.min(bonus, 3000) - Math.min(penalty, 6000);
        return (int) Math.max(1L, (long) atk * mult / 10000L);
    }

    public int effectiveDef() {
        int penalty = 0;
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.DEF_DOWN) {
                penalty += effect.value;
            } else if (effect.type == StatusType.DEF_UP) {
                bonus += effect.value;
            }
        }
        long mult = 10000L - Math.min(penalty, 9000) + Math.min(bonus, 6000);
        return (int) ((long) def * mult / 10000L);
    }

    public int effectiveRes() {
        int penalty = 0;
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.WET) {
                penalty += effect.value;
            } else if (effect.type == StatusType.RES_UP) {
                bonus += effect.value;
            }
        }
        return Math.max(0, Math.min(9500, res - penalty + Math.min(bonus, 5000)));
    }

    public int critBonusBp() {
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.CRIT_UP) {
                bonus += effect.value;
            }
        }
        return Math.min(bonus, 5000);
    }

    public int dodgeBonusBp() {
        int bonus = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.EVADE_UP) {
                bonus += effect.value;
            }
        }
        return Math.min(bonus, 6000);
    }

    public int blindBp() {
        int penalty = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.BLIND) {
                penalty += effect.value;
            }
        }
        return Math.min(penalty, 6000);
    }

    public int speedBp() {
        int bonus = 0;
        int penalty = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.SPD_UP) {
                bonus += effect.value;
            } else if (effect.type == StatusType.SLOW) {
                penalty += effect.value;
            }
        }
        return Math.max(3000, 10000 + Math.min(bonus, 5000) - Math.min(penalty, 7000));
    }

    public int reflectBp() {
        int total = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == StatusType.REFLECT) {
                total += effect.value;
            }
        }
        return Math.min(total, 5000);
    }

    public boolean silenced() {
        return hasStatus(StatusType.SILENCE);
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
