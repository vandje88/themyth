package zhaf.media.themyth.data.staticdata;

public final class CharacterStats {

    public final double hp;
    public final double atk;
    public final double def;
    public final double spd;
    public final int critRate;
    public final int critDmg;
    public final int acc;
    public final int eva;
    public final int res;
    public final int elemPower;
    public final long cp;

    public CharacterStats(double hp, double atk, double def, double spd,
                          int critRate, int critDmg, int acc, int eva, int res,
                          int elemPower) {
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.critRate = critRate;
        this.critDmg = critDmg;
        this.acc = acc;
        this.eva = eva;
        this.res = res;
        this.elemPower = elemPower;
        this.cp = Math.round(
                hp * 0.12
                        + atk * 3.2
                        + def * 1.8
                        + spd * 12
                        + critRate * 0.6
                        + critDmg * 0.12
                        + acc * 0.3
                        + eva * 0.3
                        + res * 0.4
                        + elemPower * 6
        );
    }
}
