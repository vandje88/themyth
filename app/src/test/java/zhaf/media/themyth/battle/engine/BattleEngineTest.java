package zhaf.media.themyth.battle.engine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BattleEngineTest {

    @Test
    public void sameSeedProducesSameResult() {
        BattleInput first = input(73014444032L);
        BattleInput second = input(73014444032L);

        BattleResult a = BattleEngine.simulate(first);
        BattleResult b = BattleEngine.simulate(second);

        assertEquals(a.hash, b.hash);
        assertEquals(a.winner, b.winner);
        assertEquals(a.ticks, b.ticks);
        assertEquals(a.events.size(), b.events.size());
    }

    @Test
    public void replayResetsAllUnitState() {
        BattleInput input = input(20261110L);

        BattleResult first = BattleEngine.simulate(input);
        BattleResult second = BattleEngine.simulate(input);

        assertEquals(first.hash, second.hash);
        assertEquals(first.ticks, second.ticks);
        assertEquals(first.winner, second.winner);
    }

    @Test
    public void differentSeedCanChangeOutcome() {
        BattleResult a = BattleEngine.simulate(input(1L));
        BattleResult b = BattleEngine.simulate(input(2L));

        assertNotNull(a.hash);
        assertNotNull(b.hash);
        assertTrue(a.hash.length() == 64 && b.hash.length() == 64);
    }

    @Test
    public void strongerTeamWins() {
        List<BattleUnit> strong = Arrays.asList(
                unit("A0", 0, 0, 40, 4600, 520),
                unit("A1", 0, 1, 40, 4200, 500),
                unit("A2", 0, 2, 40, 4400, 480)
        );
        List<BattleUnit> weak = Arrays.asList(
                unit("B0", 1, 0, 1, 1600, 180),
                unit("B1", 1, 1, 1, 1500, 170),
                unit("B2", 1, 2, 1, 1550, 175)
        );

        BattleResult result = BattleEngine.simulate(new BattleInput(777L, strong, weak,
                Strategy.BALANCED, Strategy.BALANCED, BattleInput.DEFAULT_MAX_TICKS));

        assertEquals(0, result.winner);
        assertTrue(result.ticks <= BattleInput.DEFAULT_MAX_TICKS);
        assertTrue(result.teamHpPctA > 50);
        assertEquals(0, result.teamHpPctB);
        assertEquals("wipe", result.endReason());
    }

    @Test
    public void battleAlwaysEndsWithEndEvent() {
        BattleResult result = BattleEngine.simulate(input(99L));

        BattleEvent last = result.events.get(result.events.size() - 1);
        assertEquals(BattleEvent.Type.END, last.type);
        assertTrue(result.ticks <= BattleInput.DEFAULT_MAX_TICKS);
        assertTrue(result.durationMs() == result.ticks * 100);
    }

    @Test
    public void maxTicksCapsSimulation() {
        BattleUnit durableA = unit("A0", 0, 0, 10, 50000, 320);
        BattleUnit durableB = unit("B0", 1, 1, 9, 50000, 310);
        BattleInput shortBattle = new BattleInput(5L,
                Arrays.asList(durableA), Arrays.asList(durableB),
                Strategy.BALANCED, Strategy.BALANCED, 20);

        BattleResult result = BattleEngine.simulate(shortBattle);

        assertTrue(result.ticks <= 20);
        assertEquals("timeout", result.endReason());
    }

    @Test
    public void emptyTeamsProduceInvalidDraw() {
        BattleResult result = BattleEngine.simulate(new BattleInput(1L,
                new ArrayList<>(), new ArrayList<>(),
                Strategy.BALANCED, Strategy.BALANCED, 100));

        assertEquals(-1, result.winner);
        assertTrue(result.draw());
        assertEquals("draw", result.endReason());
        assertEquals(2, result.events.size());
        assertEquals(BattleEvent.Type.END, result.events.get(1).type);
    }

    @Test
    public void elementChartMatchesGdd() {
        assertEquals(ElementMatrix.STRONG, ElementMatrix.multiplierBp("fire", "wind"));
        assertEquals(ElementMatrix.WEAK, ElementMatrix.multiplierBp("fire", "water"));
        assertEquals(ElementMatrix.STRONG, ElementMatrix.multiplierBp("water", "fire"));
        assertEquals(ElementMatrix.WEAK, ElementMatrix.multiplierBp("water", "lightning"));
        assertEquals(ElementMatrix.STRONG, ElementMatrix.multiplierBp("wind", "earth"));
        assertEquals(ElementMatrix.WEAK, ElementMatrix.multiplierBp("wind", "fire"));
        assertEquals(ElementMatrix.STRONG, ElementMatrix.multiplierBp("earth", "lightning"));
        assertEquals(ElementMatrix.WEAK, ElementMatrix.multiplierBp("earth", "wind"));
        assertEquals(ElementMatrix.STRONG, ElementMatrix.multiplierBp("lightning", "water"));
        assertEquals(ElementMatrix.WEAK, ElementMatrix.multiplierBp("lightning", "earth"));
        assertEquals(ElementMatrix.NEUTRAL, ElementMatrix.multiplierBp("fire", "earth"));
        assertEquals(ElementMatrix.NEUTRAL, ElementMatrix.multiplierBp(null, "fire"));
    }

    @Test
    public void hashIsSha256Hex() {
        BattleResult result = BattleEngine.simulate(input(42L));

        assertEquals(64, result.hash.length());
        assertTrue(result.hash.matches("[0-9a-f]{64}"));
    }

    @Test
    public void battleProducesActionsAndDamage() {
        BattleResult result = BattleEngine.simulate(input(2026L));

        int actions = 0;
        int hits = 0;
        for (BattleEvent event : result.events) {
            if (event.type == BattleEvent.Type.ACTION) {
                actions++;
            } else if (event.type == BattleEvent.Type.HIT) {
                hits++;
            }
        }
        assertTrue(actions > 0);
        assertTrue(hits > 0);
        assertNotNull(result.mvpUid);
    }

    private static BattleInput input(long seed) {
        List<BattleUnit> teamA = Arrays.asList(
                unitA("A0", 0),
                unitA("A1", 1),
                unitA("A2", 2)
        );
        List<BattleUnit> teamB = Arrays.asList(
                unitB("B0", 0),
                unitB("B1", 1),
                unitB("B2", 2)
        );
        return new BattleInput(seed, teamA, teamB,
                Strategy.BALANCED, Strategy.FOCUS_BACKLINE, BattleInput.DEFAULT_MAX_TICKS);
    }

    private static BattleUnit unitA(String uid, int slot) {
        return unit(uid, 0, slot, 10, 2500, 320);
    }

    private static BattleUnit unitB(String uid, int slot) {
        return unit(uid, 1, slot, 9, 2400, 310);
    }

    private static BattleUnit unit(String uid, int team, int slot, int level,
                                   int hp, int atk) {
        String element = slot == 0 ? "fire" : slot == 1 ? "water" : "wind";
        BattleUnit unit = new BattleUnit(uid, "char_" + uid, "Unit " + uid, null,
                team, slot, level, element, slot == 1 ? "mage" : "fighter",
                hp, atk, 210, 112, 1000, 15000, 300, 600, 600, 0);
        unit.setSkills(
                new Skill.Builder("Basic", Skill.Type.BASIC)
                        .power(10000).energyGain(20).build(),
                new Skill.Builder("Skill 1", Skill.Type.SKILL1)
                        .power(17000).cooldown(3).energyGain(15).build(),
                new Skill.Builder("Skill 2", Skill.Type.SKILL2)
                        .power(13000).cooldown(4).energyGain(15)
                        .target(Skill.Target.ENEMY_DUO).build(),
                new Skill.Builder("Ultimate", Skill.Type.ULTIMATE)
                        .power(38000).energyGain(0).build()
        );
        return unit;
    }
}
