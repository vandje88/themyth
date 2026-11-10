package zhaf.media.themyth.data.progression;

import java.util.Map;

import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentTemplate;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class Progression {

    public static final int MAX_SKILL_LEVEL = 5;
    public static final int MAX_ENHANCE_LEVEL = 15;
    public static final int MAX_BREAKTHROUGH = 4;
    public static final int BASE_LEVEL_CAP = 20;
    public static final String SKILL_BASIC = "basic";
    public static final String SKILL_ONE = "skill1";
    public static final String SKILL_TWO = "skill2";
    public static final String SKILL_ULTIMATE = "ultimate";
    public static final String[] SKILL_KEYS = {SKILL_BASIC, SKILL_ONE, SKILL_TWO, SKILL_ULTIMATE};

    private static final int[] TIER_LEVEL_CAP = {40, 60, 70, 80};
    private static final int[] BREAKTHROUGH_LEVEL_REQ = {20, 40, 60, 80};
    private static final long[] BREAKTHROUGH_COIN = {20000, 60000, 150000, 350000};
    private static final int[] BREAKTHROUGH_STONES = {5, 10, 20, 40};

    private Progression() {
    }

    public static long expToNext(int level) {
        int safeLevel = Math.max(1, level);
        return Math.round(120.0 * Math.pow(safeLevel, 1.35) / 10.0) * 10L;
    }

    public static long levelUpCoin(int currentLevel) {
        return 20L * Math.max(1, currentLevel);
    }

    public static int levelCap(String rarity, int breakthrough) {
        int tier = Math.max(0, Math.min(breakthrough, MAX_BREAKTHROUGH));
        int cap = tier == 0 ? BASE_LEVEL_CAP : TIER_LEVEL_CAP[tier - 1];
        return Math.min(GameConstants.maxLevel(rarity), cap);
    }

    public static boolean canBreakthrough(int breakthrough) {
        return breakthrough < MAX_BREAKTHROUGH;
    }

    public static int breakthroughLevelReq(int breakthrough) {
        return BREAKTHROUGH_LEVEL_REQ[Math.max(0, Math.min(breakthrough, MAX_BREAKTHROUGH - 1))];
    }

    public static long breakthroughCoin(int breakthrough) {
        return BREAKTHROUGH_COIN[Math.max(0, Math.min(breakthrough, MAX_BREAKTHROUGH - 1))];
    }

    public static int breakthroughStones(int breakthrough) {
        return BREAKTHROUGH_STONES[Math.max(0, Math.min(breakthrough, MAX_BREAKTHROUGH - 1))];
    }

    public static double breakthroughStatMult(int breakthrough) {
        return 1 + 0.05 * Math.max(0, Math.min(breakthrough, MAX_BREAKTHROUGH));
    }

    public static long skillUpgradeCoin(int currentLevel) {
        return 2000L * Math.max(1, currentLevel);
    }

    public static int skillUpgradeScrolls(int currentLevel) {
        return Math.max(1, currentLevel);
    }

    public static double skillMult(int level) {
        return 1 + 0.06 * (Math.max(1, Math.min(level, MAX_SKILL_LEVEL)) - 1);
    }

    public static long enhanceCoin(int newLevel) {
        return Math.round(80.0 * Math.pow(Math.max(1, newLevel), 1.5));
    }

    public static int enhanceStones(int newLevel) {
        return Math.max(1, newLevel);
    }

    public static int enhancedValue(int baseValue, int level) {
        int safeLevel = Math.max(0, Math.min(level, MAX_ENHANCE_LEVEL));
        return (int) Math.round(baseValue * (1 + 0.04 * safeLevel));
    }

    public static String elementStoneItemId(String element) {
        return "stone_" + (element == null ? "fire" : element);
    }

    public static CharacterStats statsOf(CharacterTemplate template, RosterChar owned,
                                         Map<String, EquipmentInstance> instances,
                                         EquipmentCatalog catalog) {
        int level = owned == null ? 1 : owned.level;
        int stars = owned == null ? 1 : owned.stars;
        int breakthrough = owned == null ? 0 : owned.breakthrough;
        CharacterStats base = template.statsAt(level, stars);

        double mult = breakthroughStatMult(breakthrough);
        double hp = base.hp * mult;
        double atk = base.atk * mult;
        double def = base.def * mult;
        double spd = base.spd * mult;
        int critRate = base.critRate;
        int critDmg = base.critDmg;
        int acc = base.acc;
        int eva = base.eva;
        int res = base.res;
        int elemPower = base.elemPower;

        if (owned != null && instances != null && catalog != null) {
            for (String slot : EquipmentTemplate.SLOTS) {
                String instanceId = owned.equipmentAt(slot);
                EquipmentInstance instance = instanceId == null ? null : instances.get(instanceId);
                if (instance == null) {
                    continue;
                }
                EquipmentTemplate equip = catalog.find(instance.templateId);
                if (equip == null) {
                    continue;
                }
                int value = enhancedValue(equip.mainValue, instance.level);
                switch (equip.mainKey) {
                    case "hp":
                        hp += value;
                        break;
                    case "atk":
                        atk += value;
                        break;
                    case "def":
                        def += value;
                        break;
                    case "spd":
                        spd += value;
                        break;
                    case "critRate":
                        critRate += value;
                        break;
                    case "critDmg":
                        critDmg += value;
                        break;
                    case "acc":
                        acc += value;
                        break;
                    case "eva":
                        eva += value;
                        break;
                    case "res":
                        res += value;
                        break;
                    case "elemPower":
                        elemPower += value;
                        break;
                    default:
                        break;
                }
            }
        }

        return new CharacterStats(hp, atk, def, spd,
                critRate, critDmg, acc, eva, res, elemPower);
    }
}
