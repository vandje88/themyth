package zhaf.media.themyth.data.arena;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArenaRatingTest {

    @Test
    public void tierFollowsDocumentedBands() {
        assertEquals("bronze", ArenaRating.tier(0));
        assertEquals("bronze", ArenaRating.tier(999));
        assertEquals("silver", ArenaRating.tier(1000));
        assertEquals("gold", ArenaRating.tier(1300));
        assertEquals("platinum", ArenaRating.tier(1600));
        assertEquals("diamond", ArenaRating.tier(1900));
        assertEquals("mythic", ArenaRating.tier(2200));
        assertEquals("mythic", ArenaRating.tier(4200));
    }

    @Test
    public void kFactorDropsAsRatingGrows() {
        assertEquals(32, ArenaRating.kFactor(800));
        assertEquals(24, ArenaRating.kFactor(1700));
        assertEquals(16, ArenaRating.kFactor(2300));
    }

    @Test
    public void equalRatingsExpectEvenOdds() {
        assertEquals(0.5, ArenaRating.expected(1200, 1200), 0.0001);
        assertTrue(ArenaRating.expected(1400, 1200) > 0.5);
        assertTrue(ArenaRating.expected(1000, 1200) < 0.5);
    }

    @Test
    public void winAwardsPositiveAndLossNegativeDelta() {
        assertTrue(ArenaRating.delta(1200, 1200, true, 0, false) > 0);
        assertTrue(ArenaRating.delta(1200, 1200, false, 0, false) < 0);
    }

    @Test
    public void streakBonusAppliesFromThirdWin() {
        int plain = ArenaRating.delta(1200, 1200, true, 2, false);
        int streaking = ArenaRating.delta(1200, 1200, true, 3, false);
        int longStreak = ArenaRating.delta(1200, 1200, true, 10, false);
        assertEquals(plain, streaking - 5);
        assertTrue(longStreak - plain <= 15);
    }

    @Test
    public void botMatchesUseSmallKAndNoStreakBonus() {
        int bot = ArenaRating.delta(1200, 1200, true, 10, true);
        int normal = ArenaRating.delta(1200, 1200, true, 10, false);
        assertEquals(4, bot);
        assertTrue(bot < normal);
    }

    @Test
    public void drawScoresHalfAndSkipsStreakBonus() {
        assertEquals(0, ArenaRating.deltaScore(1200, 1200, 0.5, 10, false));
        int drawAsWin = ArenaRating.deltaScore(1200, 1200, 1.0, 10, false);
        int draw = ArenaRating.deltaScore(1200, 1200, 0.5, 10, false);
        assertTrue(draw < drawAsWin);
    }

    @Test
    public void ratingNeverGoesBelowZeroOrAboveCap() {
        assertEquals(0, ArenaRating.apply(5, -40));
        assertEquals(5000, ArenaRating.apply(4990, 40));
        assertEquals(1220, ArenaRating.apply(1200, 20));
    }

    @Test
    public void seasonKeyUsesUtcMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2026, Calendar.NOVEMBER, 10, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals("s2026_11", ArenaRating.seasonKey(calendar.getTimeInMillis()));
    }
}
