package zhaf.media.themyth.data.arena;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArenaMatchmakerTest {

    @Test
    public void windowWidensEveryTenSeconds() {
        assertEquals(150, ArenaMatchmaker.window(0L));
        assertEquals(150, ArenaMatchmaker.window(9_999L));
        assertEquals(300, ArenaMatchmaker.window(10_000L));
        assertEquals(450, ArenaMatchmaker.window(25_000L));
        assertEquals(600, ArenaMatchmaker.window(60_000L));
    }

    @Test
    public void picksClosestRatingThenClosestCp() {
        List<ArenaSnapshot> pool = new ArrayList<>();
        pool.add(snapshot("far", 1450, 5000));
        pool.add(snapshot("near_cp", 1210, 9000));
        pool.add(snapshot("near_cp_better", 1210, 4000));
        ArenaSnapshot chosen = ArenaMatchmaker.pick(pool, "me", 1200, 4500, 0L);
        assertNotNull(chosen);
        assertEquals("near_cp_better", chosen.uid);
    }

    @Test
    public void skipsSelfAndIncompleteTeams() {
        List<ArenaSnapshot> pool = new ArrayList<>();
        pool.add(snapshot("me", 1200, 4000));
        pool.add(new ArenaSnapshot("short", "Short", "S", "", 4000, 1200,
                "BALANCED", new ArrayList<>(), 0L));
        assertNull(ArenaMatchmaker.pick(pool, "me", 1200, 4000, 0L));
    }

    @Test
    public void outOfWindowOpponentIsIgnored() {
        List<ArenaSnapshot> pool = new ArrayList<>();
        pool.add(snapshot("strong", 1700, 9000));
        assertNull(ArenaMatchmaker.pick(pool, "me", 1200, 4000, 0L));
        assertNotNull(ArenaMatchmaker.pick(pool, "me", 1200, 4000, 60_000L));
    }

    @Test
    public void botIsMarkedAndSlightlyWeaker() {
        ArenaSnapshot self = snapshot("me", 1200, 5000);
        ArenaSnapshot bot = ArenaMatchmaker.botFor(self);
        assertNotNull(bot);
        assertTrue(ArenaMatchmaker.isBot(bot.uid));
        assertTrue(bot.cp < self.cp);
        assertEquals(self.units.size(), bot.units.size());
    }

    private static ArenaSnapshot snapshot(String uid, int rating, long cp) {
        List<ArenaUnitSpec> units = new ArrayList<>();
        units.add(unit(0));
        units.add(unit(1));
        units.add(unit(2));
        return new ArenaSnapshot(uid, uid, "U", "", cp, rating, "BALANCED", units, 0L);
    }

    private static ArenaUnitSpec unit(int slot) {
        return new ArenaUnitSpec(slot, "gatotkaca", "Gatotkaca", "sprite_gatotkaca",
                "earth", "fighter", 20, 5000, 500, 300, 110,
                1000, 15000, 300, 600, 600, 0, 1, 1, 1, 1);
    }
}
