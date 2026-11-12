package zhaf.media.themyth.battle.engine;

public final class Skill {

    public enum Type {
        BASIC,
        SKILL1,
        SKILL2,
        ULTIMATE,
        ULTIMATE2
    }

    public enum Target {
        ENEMY_SINGLE,
        ENEMY_DUO,
        ENEMY_ROW,
        ENEMY_ALL,
        ENEMY_LOWEST,
        ENEMY_HIGHEST,
        ALLY_SELF,
        ALLY_LOWEST,
        ALLY_ALL
    }

    public final String name;
    public final Type type;
    public final Target target;
    public final int power;
    public final int scaleBp;
    public final int cooldown;
    public final int energyGain;
    public final boolean heal;
    public final int healPower;
    public final int shieldPower;
    public final StatusType effect;
    public final int effectChance;
    public final int effectTurns;
    public final int effectValue;
    public final StatusType effect2;
    public final int effect2Chance;
    public final int effect2Turns;
    public final int effect2Value;
    public final StatusType selfEffect;
    public final int selfTurns;
    public final int selfValue;
    public final StatusType selfEffect2;
    public final int selfTurns2;
    public final int selfValue2;
    public final StatusType teamEffect;
    public final int teamTurns;
    public final int teamValue;
    public final StatusType teamEffect2;
    public final int teamTurns2;
    public final int teamValue2;
    public final int teamHealMaxHpBp;
    public final int teamShieldMaxHpBp;
    public final int hits;
    public final int armorPierceBp;
    public final int executeBp;
    public final int drainEnergyBp;
    public final int selfHpCostBp;
    public final int selfHealBpOfDamage;
    public final boolean cleanseTeam;
    public final boolean stripBuff;

    private Skill(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.target = builder.target;
        this.power = builder.power;
        this.scaleBp = builder.scaleBp;
        this.cooldown = builder.cooldown;
        this.energyGain = builder.energyGain;
        this.heal = builder.heal;
        this.healPower = builder.healPower;
        this.shieldPower = builder.shieldPower;
        this.effect = builder.effect;
        this.effectChance = builder.effectChance;
        this.effectTurns = builder.effectTurns;
        this.effectValue = builder.effectValue;
        this.effect2 = builder.effect2;
        this.effect2Chance = builder.effect2Chance;
        this.effect2Turns = builder.effect2Turns;
        this.effect2Value = builder.effect2Value;
        this.selfEffect = builder.selfEffect;
        this.selfTurns = builder.selfTurns;
        this.selfValue = builder.selfValue;
        this.selfEffect2 = builder.selfEffect2;
        this.selfTurns2 = builder.selfTurns2;
        this.selfValue2 = builder.selfValue2;
        this.teamEffect = builder.teamEffect;
        this.teamTurns = builder.teamTurns;
        this.teamValue = builder.teamValue;
        this.teamEffect2 = builder.teamEffect2;
        this.teamTurns2 = builder.teamTurns2;
        this.teamValue2 = builder.teamValue2;
        this.teamHealMaxHpBp = builder.teamHealMaxHpBp;
        this.teamShieldMaxHpBp = builder.teamShieldMaxHpBp;
        this.hits = builder.hits;
        this.armorPierceBp = builder.armorPierceBp;
        this.executeBp = builder.executeBp;
        this.drainEnergyBp = builder.drainEnergyBp;
        this.selfHpCostBp = builder.selfHpCostBp;
        this.selfHealBpOfDamage = builder.selfHealBpOfDamage;
        this.cleanseTeam = builder.cleanseTeam;
        this.stripBuff = builder.stripBuff;
    }

    public boolean isSupport() {
        if (power > 0) {
            return false;
        }
        return heal || shieldPower > 0
                || teamHealMaxHpBp > 0 || teamShieldMaxHpBp > 0
                || selfEffect != null || selfEffect2 != null
                || (target == Target.ALLY_ALL
                && (teamEffect != null || teamEffect2 != null));
    }

    public boolean isUltimate() {
        return type == Type.ULTIMATE || type == Type.ULTIMATE2;
    }

    public static final class Builder {

        private final String name;
        private final Type type;
        private Target target = Target.ENEMY_SINGLE;
        private int power;
        private int scaleBp = 10000;
        private int cooldown;
        private int energyGain;
        private boolean heal;
        private int healPower;
        private int shieldPower;
        private StatusType effect;
        private int effectChance;
        private int effectTurns;
        private int effectValue;
        private StatusType effect2;
        private int effect2Chance;
        private int effect2Turns;
        private int effect2Value;
        private StatusType selfEffect;
        private int selfTurns;
        private int selfValue;
        private StatusType selfEffect2;
        private int selfTurns2;
        private int selfValue2;
        private StatusType teamEffect;
        private int teamTurns;
        private int teamValue;
        private StatusType teamEffect2;
        private int teamTurns2;
        private int teamValue2;
        private int teamHealMaxHpBp;
        private int teamShieldMaxHpBp;
        private int hits = 1;
        private int armorPierceBp;
        private int executeBp;
        private int drainEnergyBp;
        private int selfHpCostBp;
        private int selfHealBpOfDamage;
        private boolean cleanseTeam;
        private boolean stripBuff;

        public Builder(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public Builder target(Target target) {
            this.target = target;
            return this;
        }

        public Builder power(int power) {
            this.power = power;
            return this;
        }

        public Builder scaleBp(int scaleBp) {
            this.scaleBp = Math.max(1, scaleBp);
            return this;
        }

        public Builder cooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder energyGain(int energyGain) {
            this.energyGain = energyGain;
            return this;
        }

        public Builder heal(int healPower) {
            this.heal = true;
            this.healPower = healPower;
            this.target = Target.ALLY_LOWEST;
            return this;
        }

        public Builder shield(int shieldPower) {
            this.shieldPower = shieldPower;
            return this;
        }

        public Builder effect(StatusType effect, int chanceBp, int turns, int value) {
            this.effect = effect;
            this.effectChance = chanceBp;
            this.effectTurns = turns;
            this.effectValue = value;
            return this;
        }

        public Builder effect2(StatusType effect, int chanceBp, int turns, int value) {
            this.effect2 = effect;
            this.effect2Chance = chanceBp;
            this.effect2Turns = turns;
            this.effect2Value = value;
            return this;
        }

        public Builder selfEffect(StatusType effect, int turns, int value) {
            this.selfEffect = effect;
            this.selfTurns = turns;
            this.selfValue = value;
            return this;
        }

        public Builder selfEffect2(StatusType effect, int turns, int value) {
            this.selfEffect2 = effect;
            this.selfTurns2 = turns;
            this.selfValue2 = value;
            return this;
        }

        public Builder teamEffect(StatusType effect, int turns, int value) {
            this.teamEffect = effect;
            this.teamTurns = turns;
            this.teamValue = value;
            return this;
        }

        public Builder teamEffect2(StatusType effect, int turns, int value) {
            this.teamEffect2 = effect;
            this.teamTurns2 = turns;
            this.teamValue2 = value;
            return this;
        }

        public Builder teamHealMaxHp(int bp) {
            this.teamHealMaxHpBp = bp;
            return this;
        }

        public Builder teamShieldMaxHp(int bp) {
            this.teamShieldMaxHpBp = bp;
            return this;
        }

        public Builder hits(int hits) {
            this.hits = Math.max(1, hits);
            return this;
        }

        public Builder armorPierce(int bp) {
            this.armorPierceBp = bp;
            return this;
        }

        public Builder execute(int bp) {
            this.executeBp = bp;
            return this;
        }

        public Builder drainEnergy(int bp) {
            this.drainEnergyBp = bp;
            return this;
        }

        public Builder selfHpCost(int bp) {
            this.selfHpCostBp = bp;
            return this;
        }

        public Builder selfHealFromDamage(int bp) {
            this.selfHealBpOfDamage = bp;
            return this;
        }

        public Builder cleanseTeam() {
            this.cleanseTeam = true;
            return this;
        }

        public Builder stripBuff() {
            this.stripBuff = true;
            return this;
        }

        public Skill build() {
            return new Skill(this);
        }
    }
}
