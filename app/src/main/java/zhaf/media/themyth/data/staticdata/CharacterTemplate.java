package zhaf.media.themyth.data.staticdata;

public final class CharacterTemplate {

    public final String id;
    public final String name;
    public final String title;
    public final String element;
    public final String role;
    public final String rarity;
    public final String sprite;

    public CharacterTemplate(String id, String name, String title,
                             String element, String role, String rarity, String sprite) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.element = element;
        this.role = role;
        this.rarity = rarity;
        this.sprite = sprite;
    }

    public CharacterStats statsAt(int level, int stars) {
        int safeLevel = Math.max(1, Math.min(level, GameConstants.maxLevel(rarity)));
        int safeStars = Math.max(1, Math.min(stars, GameConstants.MAX_STARS));

        GameConstants.ClassBase base = GameConstants.classBase(role);
        double rarityMult = GameConstants.rarityStatMult(rarity);
        double starMult = 1 + 0.08 * (safeStars - 1);

        double hp = base.hp * rarityMult * starMult * (1 + 0.048 * (safeLevel - 1));
        double atk = base.atk * rarityMult * starMult * (1 + 0.042 * (safeLevel - 1));
        double def = base.def * rarityMult * starMult * (1 + 0.045 * (safeLevel - 1));
        double spd = base.spd + Math.min(8.0, 0.10 * (safeLevel - 1));

        return new CharacterStats(
                hp, atk, def, spd,
                base.critRate, base.critDmg, base.acc, base.eva, base.res,
                0
        );
    }
}
