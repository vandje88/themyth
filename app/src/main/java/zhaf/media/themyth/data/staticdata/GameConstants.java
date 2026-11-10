package zhaf.media.themyth.data.staticdata;

public final class GameConstants {

    public static final int SLOT_COUNT = 3;
    public static final int MAX_STARS = 6;

    private GameConstants() {
    }

    public static double rarityStatMult(String rarity) {
        if (rarity == null) {
            return 1.0;
        }
        switch (rarity) {
            case "uncommon":
                return 1.06;
            case "rare":
                return 1.12;
            case "epic":
                return 1.20;
            case "legendary":
                return 1.30;
            case "mythic":
                return 1.42;
            default:
                return 1.0;
        }
    }

    public static int maxLevel(String rarity) {
        if (rarity == null) {
            return 60;
        }
        switch (rarity) {
            case "uncommon":
                return 65;
            case "rare":
                return 70;
            case "epic":
                return 75;
            case "legendary":
            case "mythic":
                return 80;
            default:
                return 60;
        }
    }

    public static int rarityRank(String rarity) {
        if (rarity == null) {
            return 0;
        }
        switch (rarity) {
            case "mythic":
                return 5;
            case "legendary":
                return 4;
            case "epic":
                return 3;
            case "rare":
                return 2;
            case "uncommon":
                return 1;
            default:
                return 0;
        }
    }

    public static ClassBase classBase(String role) {
        if (role == null) {
            return new ClassBase(4400, 330, 200, 115, 1000, 15000, 300, 600, 600);
        }
        switch (role) {
            case "assassin":
                return new ClassBase(3200, 380, 120, 135, 1500, 16000, 400, 1000, 400);
            case "tank":
                return new ClassBase(5600, 250, 260, 95, 500, 14000, 200, 400, 800);
            case "mage":
                return new ClassBase(3400, 400, 140, 105, 800, 15500, 350, 500, 500);
            case "support":
                return new ClassBase(4200, 260, 180, 110, 500, 14000, 250, 600, 700);
            default:
                return new ClassBase(4400, 330, 200, 115, 1000, 15000, 300, 600, 600);
        }
    }

    public static final class ClassBase {
        public final double hp;
        public final double atk;
        public final double def;
        public final double spd;
        public final int critRate;
        public final int critDmg;
        public final int acc;
        public final int eva;
        public final int res;

        ClassBase(double hp, double atk, double def, double spd,
                  int critRate, int critDmg, int acc, int eva, int res) {
            this.hp = hp;
            this.atk = atk;
            this.def = def;
            this.spd = spd;
            this.critRate = critRate;
            this.critDmg = critDmg;
            this.acc = acc;
            this.eva = eva;
            this.res = res;
        }
    }
}
