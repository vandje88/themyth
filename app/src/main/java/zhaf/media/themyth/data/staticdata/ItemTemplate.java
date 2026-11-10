package zhaf.media.themyth.data.staticdata;

import zhaf.media.themyth.core.Texts;

public final class ItemTemplate {

    public final String id;
    public final String name;
    public final String nameEn;
    public final String category;
    public final String rarity;
    public final int stack;
    public final int expValue;
    public final int sellPrice;
    public final String desc;
    public final String descEn;

    public ItemTemplate(String id, String name, String nameEn, String category, String rarity,
                        int stack, int expValue, int sellPrice, String desc, String descEn) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
        this.category = category;
        this.rarity = rarity;
        this.stack = stack;
        this.expValue = expValue;
        this.sellPrice = sellPrice;
        this.desc = desc;
        this.descEn = descEn;
    }

    public String displayName() {
        return Texts.pick(name, nameEn);
    }

    public String description() {
        return Texts.pick(desc, descEn);
    }

    public boolean isExpMaterial() {
        return expValue > 0;
    }

    public boolean isElementStone() {
        return "EVOLUTION_STONE".equals(category);
    }
}
