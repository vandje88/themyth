package zhaf.media.themyth.data.progression;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentTemplate;

public class ProgressionTest {

    private static final double EPS = 0.001;

    @Test
    public void expCurveMatchesDesign() {
        assertEquals(120L, Progression.expToNext(1));
        assertEquals(6850L, Progression.expToNext(20));
        assertEquals(44500L, Progression.expToNext(80));
    }

    @Test
    public void levelCapFollowsBreakthroughTiers() {
        assertEquals(20, Progression.levelCap("mythic", 0));
        assertEquals(40, Progression.levelCap("mythic", 1));
        assertEquals(60, Progression.levelCap("mythic", 2));
        assertEquals(70, Progression.levelCap("mythic", 3));
        assertEquals(80, Progression.levelCap("mythic", 4));
        assertEquals(60, Progression.levelCap("common", 4));
    }

    @Test
    public void levelUpCostScalesWithLevel() {
        assertEquals(20L, Progression.levelUpCoin(1));
        assertEquals(400L, Progression.levelUpCoin(20));
    }

    @Test
    public void breakthroughRequirementsMatchBalanceSheet() {
        assertEquals(20, Progression.breakthroughLevelReq(0));
        assertEquals(80, Progression.breakthroughLevelReq(3));
        assertEquals(20000L, Progression.breakthroughCoin(0));
        assertEquals(350000L, Progression.breakthroughCoin(3));
        assertEquals(5, Progression.breakthroughStones(0));
        assertEquals(40, Progression.breakthroughStones(3));
        assertEquals(1.0, Progression.breakthroughStatMult(0), EPS);
        assertEquals(1.2, Progression.breakthroughStatMult(4), EPS);
    }

    @Test
    public void skillUpgradeCostAndMultiplier() {
        assertEquals(2000L, Progression.skillUpgradeCoin(1));
        assertEquals(10000L, Progression.skillUpgradeCoin(5));
        assertEquals(5, Progression.skillUpgradeScrolls(5));
        assertEquals(1.0, Progression.skillMult(1), EPS);
        assertEquals(1.24, Progression.skillMult(5), EPS);
    }

    @Test
    public void enhanceCostAndValue() {
        assertEquals(80L, Progression.enhanceCoin(1));
        assertEquals(640L, Progression.enhanceCoin(4));
        assertEquals(15, Progression.enhanceStones(15));
        assertEquals(120, Progression.enhancedValue(120, 0));
        assertEquals(144, Progression.enhancedValue(120, 5));
        assertEquals(192, Progression.enhancedValue(120, 15));
    }

    @Test
    public void statsIncludeBreakthroughAndEquipment() {
        CharacterTemplate template = new CharacterTemplate(
                "test", "Test", "", "fire", "fighter", "common", null);
        EquipmentTemplate blade = new EquipmentTemplate(
                "eq_iron_blade", "Bilah Besi", "Iron Blade", "WEAPON", "rare",
                "atk", 120, 400);
        EquipmentCatalog catalog = EquipmentCatalog.of(
                Arrays.asList(blade));

        Map<String, String> slots = new HashMap<>();
        slots.put("WEAPON", "inst1");
        RosterChar owned = new RosterChar("test", 1, 0, 1, 0, 2,
                1, 1, 1, 1, slots);
        Map<String, EquipmentInstance> instances = new HashMap<>();
        instances.put("inst1", new EquipmentInstance(
                "inst1", "eq_iron_blade", 5, false, "test"));

        CharacterStats stats = Progression.statsOf(template, owned, instances, catalog);
        assertEquals(4840.0, stats.hp, EPS);
        assertEquals(363.0 + 144.0, stats.atk, EPS);
        assertEquals(220.0, stats.def, EPS);
        assertEquals(126.5, stats.spd, EPS);
    }

    @Test
    public void statsWithoutEquipmentUseBaseTemplate() {
        CharacterTemplate template = new CharacterTemplate(
                "test", "Test", "", "water", "mage", "epic", null);
        CharacterStats base = template.statsAt(1, 1);
        CharacterStats stats = Progression.statsOf(template, null,
                Collections.emptyMap(), EquipmentCatalog.of(Collections.emptyList()));
        assertEquals(base.hp, stats.hp, EPS);
        assertEquals(base.atk, stats.atk, EPS);
        assertEquals(base.cp, stats.cp);
    }
}
