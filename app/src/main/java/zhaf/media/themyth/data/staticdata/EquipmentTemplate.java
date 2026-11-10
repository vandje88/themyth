package zhaf.media.themyth.data.staticdata;

import zhaf.media.themyth.core.Texts;

public final class EquipmentTemplate {

    public static final String SLOT_WEAPON = "WEAPON";
    public static final String SLOT_ARMOR = "ARMOR";
    public static final String SLOT_HELMET = "HELMET";
    public static final String SLOT_GLOVES = "GLOVES";
    public static final String SLOT_BOOTS = "BOOTS";
    public static final String SLOT_ACCESSORY = "ACCESSORY";

    public static final String[] SLOTS = {
            SLOT_WEAPON, SLOT_ARMOR, SLOT_HELMET, SLOT_GLOVES, SLOT_BOOTS, SLOT_ACCESSORY
    };

    public final String id;
    public final String name;
    public final String nameEn;
    public final String slot;
    public final String rarity;
    public final String mainKey;
    public final int mainValue;
    public final int sellPrice;

    public EquipmentTemplate(String id, String name, String nameEn, String slot, String rarity,
                             String mainKey, int mainValue, int sellPrice) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
        this.slot = slot;
        this.rarity = rarity;
        this.mainKey = mainKey;
        this.mainValue = mainValue;
        this.sellPrice = sellPrice;
    }

    public String displayName() {
        return Texts.pick(name, nameEn);
    }
}
