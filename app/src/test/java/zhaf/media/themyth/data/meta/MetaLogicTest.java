package zhaf.media.themyth.data.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

public class MetaLogicTest {

    private static final long NOW = Instant.parse("2026-11-10T12:00:00Z").toEpochMilli();
    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    public void loginClaimOncePerDay() {
        String today = MetaClock.dayKey(NOW);
        assertFalse(DailyLoginLogic.canClaim(today, NOW));
        assertTrue(DailyLoginLogic.canClaim(MetaClock.yesterdayKey(NOW), NOW));
        assertTrue(DailyLoginLogic.canClaim(null, NOW));
    }

    @Test
    public void loginStreakIncrementsAndWraps() {
        assertEquals(1, DailyLoginLogic.nextStreak(null, 0, NOW));
        assertEquals(2, DailyLoginLogic.nextStreak(MetaClock.yesterdayKey(NOW), 1, NOW));
        assertEquals(4, DailyLoginLogic.nextStreak(MetaClock.yesterdayKey(NOW), 3, NOW));
        assertEquals(1, DailyLoginLogic.nextStreak(MetaClock.yesterdayKey(NOW), 7, NOW));
        assertEquals(1, DailyLoginLogic.nextStreak(MetaClock.dayKey(NOW - 3 * DAY), 4, NOW));
        assertEquals(5, DailyLoginLogic.nextStreak(MetaClock.dayKey(NOW), 5, NOW));
    }

    @Test
    public void loginRewardsFollowCycleTable() {
        LoginReward first = DailyLoginLogic.rewardFor(1);
        assertEquals(2000, first.coin);
        assertEquals(0, first.diamond);
        LoginReward last = DailyLoginLogic.rewardFor(7);
        assertEquals(12000, last.coin);
        assertEquals(50, last.diamond);
        assertEquals(1, last.tickets);

        LoginReward clampedLow = DailyLoginLogic.rewardFor(0);
        assertEquals(1, clampedLow.day);
        assertEquals(7, DailyLoginLogic.rewardFor(99).day);
    }

    @Test
    public void idleCapGrowsWithLevel() {
        assertEquals(480, IdleRewardLogic.capMinutes(1));
        assertEquals(534, IdleRewardLogic.capMinutes(10));
        assertEquals(IdleRewardLogic.MAX_CAP_MINUTES, IdleRewardLogic.capMinutes(500));
    }

    @Test
    public void idleAccrualIsCapped() {
        long cap = IdleRewardLogic.capMs(1);
        assertEquals(cap, IdleRewardLogic.accruedMs(NOW - 30 * DAY, NOW, 1));
        assertEquals(IdleRewardLogic.MIN_MS,
                IdleRewardLogic.accruedMs(NOW - IdleRewardLogic.MIN_MS, NOW, 1));
        assertEquals(0L, IdleRewardLogic.accruedMs(NOW + 1000L, NOW, 1));
        assertEquals(0L, IdleRewardLogic.accruedMs(0L, NOW, 1));
    }

    @Test
    public void idleReadinessAndReward() {
        assertFalse(IdleRewardLogic.ready(IdleRewardLogic.MIN_MS - 1));
        assertTrue(IdleRewardLogic.ready(IdleRewardLogic.MIN_MS));

        IdleReward reward = IdleRewardLogic.reward(60L * 60L * 1000L, 1);
        assertEquals(60, reward.minutes);
        assertEquals(60 * IdleRewardLogic.COIN_PER_MINUTE, reward.coin);
        assertEquals(2, reward.energy);
    }

    @Test
    public void missionProgressClampsAndClaimRules() {
        assertEquals(2, MissionLogic.addProgress(1, 1, 3));
        assertEquals(3, MissionLogic.addProgress(2, 5, 3));
        assertEquals(0, MissionLogic.addProgress(0, -4, 3));
        assertTrue(MissionLogic.complete(3, 3));
        assertFalse(MissionLogic.complete(2, 3));
        assertTrue(MissionLogic.canClaim(3, 3, false));
        assertFalse(MissionLogic.canClaim(3, 3, true));
        assertFalse(MissionLogic.canClaim(2, 3, false));
    }

    @Test
    public void missionCatalogContainsBothScopes() {
        assertEquals(4, MissionCatalog.daily().size());
        assertEquals(3, MissionCatalog.weekly().size());
        for (MissionDef def : MissionCatalog.all()) {
            assertTrue(def.target >= 1);
            assertTrue(def.daily() || MissionDef.SCOPE_WEEKLY.equals(def.scope));
            assertFalse(def.title().isEmpty());
            assertFalse(def.description().isEmpty());
        }
        assertEquals(MissionCatalog.DAILY_WIN, MissionCatalog.find("daily_win").id);
    }

    @Test
    public void claimKeyUsesPeriodAndMission() {
        MissionDef daily = MissionCatalog.find(MissionCatalog.DAILY_WIN);
        MissionDef weekly = MissionCatalog.find(MissionCatalog.WEEKLY_WIN);
        assertEquals(MetaClock.dayKey(NOW) + "_daily_win", PlayerMeta.claimKey(daily, NOW));
        assertEquals(MetaClock.weekKey(NOW) + "_weekly_win", PlayerMeta.claimKey(weekly, NOW));
        assertFalse(PlayerMeta.claimKey(daily, NOW).equals(PlayerMeta.claimKey(daily, NOW + DAY)));
    }

    @Test
    public void badgesUnlockByStats() {
        PlayerStats rookie = new PlayerStats(1, 1000, 0, 0, 1, 0, 0, false);
        assertFalse(BadgeCatalog.unlocked("first_win", rookie));
        assertTrue(BadgeCatalog.unlocked(BadgeCatalog.NONE, rookie));
        assertFalse(BadgeCatalog.unlocked("guild_warrior", rookie));

        PlayerStats veteran = new PlayerStats(20, 1950, 60, 10, 12, 2, 12, true);
        assertTrue(BadgeCatalog.unlocked("first_win", veteran));
        assertTrue(BadgeCatalog.unlocked("myth_master", veteran));
        assertTrue(BadgeCatalog.unlocked("legendary_fighter", veteran));
        assertTrue(BadgeCatalog.unlocked("pvp_champion", veteran));
        assertTrue(BadgeCatalog.unlocked("dungeon_explorer", veteran));
        assertTrue(BadgeCatalog.unlocked("collector", veteran));
        assertTrue(BadgeCatalog.unlocked("guild_warrior", veteran));
        assertTrue(BadgeCatalog.unlocked("top_ranking", veteran));
        assertFalse(BadgeCatalog.unlocked("unknown_badge", veteran));
    }

    @Test
    public void titlesUnlockByStats() {
        PlayerStats rookie = new PlayerStats(1, 1000, 0, 0, 1, 0, 0, false);
        assertTrue(TitleCatalog.unlocked(TitleCatalog.DEFAULT_TITLE, rookie));
        assertFalse(TitleCatalog.unlocked("shadow_hunter", rookie));
        assertFalse(TitleCatalog.unlocked("elite_warrior", rookie));

        PlayerStats veteran = new PlayerStats(12, 1900, 100, 5, 10, 1, 0, true);
        assertTrue(TitleCatalog.unlocked("shadow_hunter", veteran));
        assertTrue(TitleCatalog.unlocked("master_shinobi", veteran));
        assertTrue(TitleCatalog.unlocked("elite_warrior", veteran));
        assertTrue(TitleCatalog.unlocked("legendary_myth", veteran));
        assertTrue(TitleCatalog.unlocked("grandmaster", veteran));
        assertEquals(6, TitleCatalog.all().size());
        assertEquals(8, BadgeCatalog.all().size());
    }

    @Test
    public void playerStatsWinRate() {
        PlayerStats stats = new PlayerStats(5, 1200, 3, 1, 4, 0, 0, false);
        assertEquals(4, stats.totalBattles());
        assertEquals(75, stats.winRatePercent());
        assertEquals(0, new PlayerStats(5, 1200, 0, 0, 0, 0, 0, false).winRatePercent());
    }
}
