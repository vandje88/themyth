package zhaf.media.themyth.battle.engine;

public final class Skill {

    public enum Type {
        BASIC,
        SKILL1,
        SKILL2,
        ULTIMATE
    }

    public enum Target {
        ENEMY_SINGLE,
        ENEMY_DUO,
        ENEMY_ROW,
        ENEMY_ALL,
        ALLY_LOWEST
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
    public final StatusType selfEffect;
    public final int selfTurns;
    public final int selfValue;

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
        this.selfEffect = builder.selfEffect;
        this.selfTurns = builder.selfTurns;
        this.selfValue = builder.selfValue;
    }

    public boolean isSupport() {
        return heal || shieldPower > 0;
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
        private StatusType selfEffect;
        private int selfTurns;
        private int selfValue;

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

        public Builder selfEffect(StatusType effect, int turns, int value) {
            this.selfEffect = effect;
            this.selfTurns = turns;
            this.selfValue = value;
            return this;
        }

        public Skill build() {
            return new Skill(this);
        }
    }
}
