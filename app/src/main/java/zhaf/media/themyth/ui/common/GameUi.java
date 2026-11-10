package zhaf.media.themyth.ui.common;

import java.util.Locale;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import zhaf.media.themyth.R;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class GameUi {

    private GameUi() {
    }

    @ColorRes
    public static int elementColor(String element) {
        if (element == null) {
            return R.color.text_muted;
        }
        switch (element) {
            case "fire":
                return R.color.element_fire;
            case "water":
                return R.color.element_water;
            case "wind":
                return R.color.element_wind;
            case "earth":
                return R.color.element_earth;
            case "lightning":
                return R.color.element_lightning;
            default:
                return R.color.text_muted;
        }
    }

    @StringRes
    public static int elementLabel(String element) {
        if (element == null) {
            return R.string.element_fire;
        }
        switch (element) {
            case "water":
                return R.string.element_water;
            case "wind":
                return R.string.element_wind;
            case "earth":
                return R.string.element_earth;
            case "lightning":
                return R.string.element_lightning;
            default:
                return R.string.element_fire;
        }
    }

    @ColorRes
    public static int rarityColor(String rarity) {
        if (rarity == null) {
            return R.color.rarity_common;
        }
        switch (rarity) {
            case "uncommon":
                return R.color.rarity_uncommon;
            case "rare":
                return R.color.rarity_rare;
            case "epic":
                return R.color.rarity_epic;
            case "legendary":
                return R.color.rarity_legendary;
            case "mythic":
                return R.color.rarity_mythic;
            default:
                return R.color.rarity_common;
        }
    }

    @StringRes
    public static int rarityLabel(String rarity) {
        if (rarity == null) {
            return R.string.rarity_common;
        }
        switch (rarity) {
            case "uncommon":
                return R.string.rarity_uncommon;
            case "rare":
                return R.string.rarity_rare;
            case "epic":
                return R.string.rarity_epic;
            case "legendary":
                return R.string.rarity_legendary;
            case "mythic":
                return R.string.rarity_mythic;
            default:
                return R.string.rarity_common;
        }
    }

    @StringRes
    public static int roleLabel(String role) {
        if (role == null) {
            return R.string.role_fighter;
        }
        switch (role) {
            case "assassin":
                return R.string.role_assassin;
            case "tank":
                return R.string.role_tank;
            case "mage":
                return R.string.role_mage;
            case "support":
                return R.string.role_support;
            default:
                return R.string.role_fighter;
        }
    }

    @StringRes
    public static int slotLabel(int index) {
        if (index == 0) {
            return R.string.formation_slot_front;
        }
        if (index == 1) {
            return R.string.formation_slot_mid;
        }
        return R.string.formation_slot_back;
    }

    @StringRes
    public static int equipSlotLabel(String slot) {
        if (slot == null) {
            return R.string.equip_slot_weapon;
        }
        switch (slot) {
            case "ARMOR":
                return R.string.equip_slot_armor;
            case "HELMET":
                return R.string.equip_slot_helmet;
            case "GLOVES":
                return R.string.equip_slot_gloves;
            case "BOOTS":
                return R.string.equip_slot_boots;
            case "ACCESSORY":
                return R.string.equip_slot_accessory;
            default:
                return R.string.equip_slot_weapon;
        }
    }

    @StringRes
    public static int statLabel(String key) {
        if (key == null) {
            return R.string.stat_hp;
        }
        switch (key) {
            case "atk":
                return R.string.stat_atk;
            case "def":
                return R.string.stat_def;
            case "spd":
                return R.string.stat_spd;
            case "critRate":
                return R.string.stat_crit;
            case "critDmg":
                return R.string.stat_crit_dmg;
            case "acc":
                return R.string.stat_acc;
            case "eva":
                return R.string.stat_eva;
            case "res":
                return R.string.stat_res;
            case "elemPower":
                return R.string.stat_elem_power;
            default:
                return R.string.stat_hp;
        }
    }

    @StringRes
    public static int skillLabel(String key) {
        if (key == null) {
            return R.string.skill_basic;
        }
        switch (key) {
            case "skill1":
                return R.string.skill_one;
            case "skill2":
                return R.string.skill_two;
            case "ultimate":
                return R.string.skill_ultimate;
            default:
                return R.string.skill_basic;
        }
    }

    @StringRes
    public static int strategyLabel(String strategy) {
        if (FormationState.STRATEGY_FOCUS_BACKLINE.equals(strategy)) {
            return R.string.strategy_focus_backline;
        }
        if (FormationState.STRATEGY_PROTECT_ALLY.equals(strategy)) {
            return R.string.strategy_protect_ally;
        }
        if (FormationState.STRATEGY_BURST.equals(strategy)) {
            return R.string.strategy_burst;
        }
        return R.string.strategy_balanced;
    }

    @StringRes
    public static int strategyDescription(String strategy) {
        if (FormationState.STRATEGY_FOCUS_BACKLINE.equals(strategy)) {
            return R.string.strategy_focus_backline_desc;
        }
        if (FormationState.STRATEGY_PROTECT_ALLY.equals(strategy)) {
            return R.string.strategy_protect_ally_desc;
        }
        if (FormationState.STRATEGY_BURST.equals(strategy)) {
            return R.string.strategy_burst_desc;
        }
        return R.string.strategy_balanced_desc;
    }

    public static String percent(int basisPoints) {
        return String.format(Locale.ROOT, "%.1f%%", basisPoints / 100.0);
    }

    public static int safeSlot(int index) {
        return Math.max(0, Math.min(index, GameConstants.SLOT_COUNT - 1));
    }
}
