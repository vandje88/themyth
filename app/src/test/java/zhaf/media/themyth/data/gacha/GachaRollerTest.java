package zhaf.media.themyth.data.gacha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import zhaf.media.themyth.data.staticdata.GachaPool;
import zhaf.media.themyth.data.staticdata.GameConstants;

public class GachaRollerTest {

    private static GachaPool pool(int pityEpic, int pityLegendary, Map<String, Integer> rates) {
        return new GachaPool("test", "Test", "Test", "", "",
                300, 2700, 1, 10, pityEpic, pityLegendary,
                true, "", rates);
    }

    private static Map<String, Integer> designRates() {
        Map<String, Integer> rates = new HashMap<>();
        rates.put("common", 4500);
        rates.put("uncommon", 3000);
        rates.put("rare", 1500);
        rates.put("epic", 750);
        rates.put("legendary", 220);
        rates.put("mythic", 30);
        return rates;
    }

    @Test
    public void designRatesSumToHundredPercent() {
        assertEquals(10000, pool(20, 80, designRates()).totalRate());
    }

    @Test
    public void epicPityForcesEpicOnThresholdPull() {
        Map<String, Integer> rates = new HashMap<>();
        rates.put("common", 10000);
        GachaPool pool = pool(20, 80, rates);
        GachaRoller.Pity pity = new GachaRoller.Pity();
        Random rng = new Random(7);

        for (int i = 0; i < 19; i++) {
            assertEquals("common", GachaRoller.roll(pool, rng, pity));
        }
        assertEquals("epic", GachaRoller.roll(pool, rng, pity));
        assertEquals(0, pity.epic);
        assertEquals(20, pity.legendary);
    }

    @Test
    public void legendaryPityWinsOverEpicPity() {
        Map<String, Integer> rates = new HashMap<>();
        rates.put("common", 10000);
        GachaPool pool = pool(2, 5, rates);
        GachaRoller.Pity pity = new GachaRoller.Pity(1, 4);
        Random rng = new Random(3);

        assertEquals("legendary", GachaRoller.roll(pool, rng, pity));
        assertEquals(0, pity.legendary);
        assertEquals(0, pity.epic);
    }

    @Test
    public void naturalLegendaryResetsBothCounters() {
        Map<String, Integer> rates = new HashMap<>();
        rates.put("legendary", 10000);
        GachaPool pool = pool(20, 80, rates);
        GachaRoller.Pity pity = new GachaRoller.Pity(9, 39);
        Random rng = new Random(11);

        assertEquals("legendary", GachaRoller.roll(pool, rng, pity));
        assertEquals(0, pity.epic);
        assertEquals(0, pity.legendary);
    }

    @Test
    public void rarePlusRollNeverDropsBelowRare() {
        GachaPool pool = pool(20, 80, designRates());
        GachaRoller.Pity pity = new GachaRoller.Pity();
        Random rng = new Random(19);
        for (int i = 0; i < 5000; i++) {
            String rarity = GachaRoller.rollRarePlus(pool, rng, pity);
            assertTrue(GameConstants.rarityRank(rarity) >= GameConstants.rarityRank("rare"));
        }
    }

    @Test
    public void millionPullsStayInsidePublishedRates() {
        GachaPool pool = pool(20, 80, designRates());
        Random rng = new Random(20261110L);
        int pulls = 1_000_000;
        int[] counts = new int[GameConstants.RARITY_LADDER.length];
        for (int i = 0; i < pulls; i++) {
            String rarity = GachaRoller.weighted(pool, rng, 0);
            counts[GameConstants.rarityRank(rarity)]++;
        }

        assertRate(counts[0], pulls, 45.0);
        assertRate(counts[1], pulls, 30.0);
        assertRate(counts[2], pulls, 15.0);
        assertRate(counts[3], pulls, 7.5);
        assertRate(counts[4], pulls, 2.2);
        assertRate(counts[5], pulls, 0.3);
    }

    private static void assertRate(int count, int total, double expectedPercent) {
        double actual = count * 100.0 / total;
        assertTrue("rate " + actual + " vs " + expectedPercent,
                Math.abs(actual - expectedPercent) <= 0.3);
    }
}
