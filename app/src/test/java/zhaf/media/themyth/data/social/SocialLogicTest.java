package zhaf.media.themyth.data.social;

import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SocialLogicTest {

    private static final long T2120 = utc(2026, Calendar.NOVEMBER, 10, 12, 0);
    private static final long NEXT_DAY = T2120 + SocialDay.DAY_MS;

    @Test
    public void dayKeyUsesUtcMidnight() {
        assertEquals("2026-11-10", SocialDay.dayKey(T2120));
        assertEquals("2026-11-10", SocialDay.dayKey(T2120 + 3_600_000L));
        assertEquals("2026-11-11", SocialDay.dayKey(NEXT_DAY));
    }

    @Test
    public void weekStartsOnMondayAndLastsSevenDays() {
        assertEquals("2026-11-09", SocialDay.dayKey(SocialDay.weekStart(T2120)));
        assertEquals("2026-11-16", SocialDay.dayKey(SocialDay.weekEnd(T2120)));
        assertEquals(SocialDay.weekKey(T2120 - 24L * 3_600_000L), SocialDay.weekKey(T2120));
        assertEquals(SocialDay.weekKey(T2120), SocialDay.weekKey(SocialDay.weekEnd(T2120) - 1L));
    }

    @Test
    public void giftResetsOnNextUtcDay() {
        assertTrue(SocialDay.giftReady(null, T2120));
        assertTrue(SocialDay.giftReady("", T2120));
        assertFalse(SocialDay.giftReady("2026-11-10", T2120));
        assertTrue(SocialDay.giftReady("2026-11-10", NEXT_DAY));
    }

    @Test
    public void mailExpiresAfterThirtyDays() {
        long created = T2120;
        long expires = MailLogic.expiresAt(created);
        assertEquals(30L * SocialDay.DAY_MS, expires - created);
        assertFalse(MailLogic.isExpired(expires, T2120));
        assertTrue(MailLogic.isExpired(expires, expires));
        assertTrue(MailLogic.canClaim(false, expires, true, T2120));
        assertFalse(MailLogic.canClaim(true, expires, true, T2120));
        assertFalse(MailLogic.canClaim(false, T2120, false, T2120));
        assertEquals(0, MailLogic.clampQty(-4));
        assertEquals(99999, MailLogic.clampQty(500_000));
    }

    @Test
    public void attachmentsAreDetected() {
        Map<String, Integer> items = new HashMap<>();
        items.put("expscroll_s", 2);
        assertTrue(MailLogic.hasAttachment(0, 0, items));
        items.put("expscroll_s", 0);
        assertFalse(MailLogic.hasAttachment(0, 0, items));
        assertTrue(MailLogic.hasAttachment(10, 0, null));
        assertTrue(MailLogic.hasAttachment(0, 5, null));
    }

    @Test
    public void friendRequestChecks() {
        assertEquals(FriendLogic.SELF, FriendLogic.requestCheck("user01", "user01", true,
                false, false, false, 0));
        assertEquals(FriendLogic.MISSING, FriendLogic.requestCheck("user01", "user02", false,
                false, false, false, 0));
        assertEquals(FriendLogic.ALREADY_FRIEND, FriendLogic.requestCheck("user01", "user02", true,
                true, false, false, 0));
        assertEquals(FriendLogic.INCOMING_PENDING, FriendLogic.requestCheck("user01", "user02", true,
                false, false, true, 0));
        assertEquals(FriendLogic.PENDING, FriendLogic.requestCheck("user01", "user02", true,
                false, true, false, 0));
        assertEquals(FriendLogic.FULL, FriendLogic.requestCheck("user01", "user02", true,
                false, false, false, FriendLogic.MAX_FRIENDS));
        assertEquals(FriendLogic.OK, FriendLogic.requestCheck("user01", "user02", true,
                false, false, false, 4));
    }

    @Test
    public void uidValidationRejectsSeparators() {
        assertTrue(FriendLogic.validUid("Xk29sd8a7BcD"));
        assertFalse(FriendLogic.validUid("abc"));
        assertFalse(FriendLogic.validUid("uid/with/slash"));
        assertFalse(FriendLogic.validUid(null));
    }

    @Test
    public void guildNamesAndTags() {
        assertEquals("arunika", GuildLogic.normalizeName("  Arunika "));
        assertEquals("ARN", GuildLogic.normalizeTag(" arn "));
        assertTrue(GuildLogic.validName("Arunika"));
        assertFalse(GuildLogic.validName("AB"));
        assertTrue(GuildLogic.validTag("ARN"));
        assertFalse(GuildLogic.validTag("A"));
        assertEquals("g_arunika_1a2b", GuildLogic.guildId("Arunika", 0x1a2bL));
    }

    @Test
    public void guildJoinRules() {
        assertEquals(GuildLogic.JOIN_SAME, GuildLogic.joinCheck(20, 10, 5, true, "g1", "g1"));
        assertEquals(GuildLogic.JOIN_CLOSED, GuildLogic.joinCheck(20, 10, 5, false, null, "g1"));
        assertEquals(GuildLogic.JOIN_FULL, GuildLogic.joinCheck(20, 10,
                GuildLogic.MAX_MEMBERS, true, null, "g1"));
        assertEquals(GuildLogic.JOIN_LEVEL, GuildLogic.joinCheck(5, 10, 5, true, null, "g1"));
        assertEquals(GuildLogic.JOIN_OK, GuildLogic.joinCheck(20, 10, 5, true, null, "g1"));
    }

    @Test
    public void donationsResetDaily() {
        assertEquals(3, GuildLogic.donationsLeft("2026-11-09", 3, "2026-11-10"));
        assertEquals(3, GuildLogic.donationsLeft(null, 0, "2026-11-10"));
        assertEquals(1, GuildLogic.donationsLeft("2026-11-10", 2, "2026-11-10"));
        assertEquals(0, GuildLogic.donationsLeft("2026-11-10", 9, "2026-11-10"));
    }

    @Test
    public void roleManagement() {
        assertTrue(GuildLogic.canManage(GuildLogic.ROLE_LEADER, GuildLogic.ROLE_OFFICER));
        assertTrue(GuildLogic.canManage(GuildLogic.ROLE_OFFICER, GuildLogic.ROLE_MEMBER));
        assertFalse(GuildLogic.canManage(GuildLogic.ROLE_MEMBER, GuildLogic.ROLE_OFFICER));
        assertFalse(GuildLogic.canManage(GuildLogic.ROLE_LEADER, GuildLogic.ROLE_LEADER));

        Map<String, String> roles = new HashMap<>();
        roles.put("leader", GuildLogic.ROLE_LEADER);
        roles.put("member", GuildLogic.ROLE_MEMBER);
        roles.put("officer", GuildLogic.ROLE_OFFICER);
        assertEquals("officer", GuildLogic.nextLeader(roles, "leader"));

        roles.remove("officer");
        assertEquals("member", GuildLogic.nextLeader(roles, "leader"));

        roles.remove("member");
        assertNull(GuildLogic.nextLeader(roles, "leader"));
    }

    @Test
    public void bossDamageIsCapped() {
        assertEquals(250_000, GuildBossLogic.damageDealt(1_000_000, 75));
        assertEquals(0, GuildBossLogic.damageDealt(1_000_000, 100));
        assertEquals(250_000, GuildBossLogic.capDamage(1_000_000, 900_000));
        assertEquals(120_000, GuildBossLogic.capDamage(1_000_000, 120_000));
        assertEquals(0, GuildBossLogic.apply(120_000, 200_000));
        assertEquals(50_000, GuildBossLogic.apply(120_000, 70_000));
        assertEquals(2_000, GuildBossLogic.guildCoin(1_000_000));
        assertEquals(1_000, GuildBossLogic.weeklyPoints(1_000_000));
        assertEquals(1_200_000, GuildBossLogic.bossHp(1));
        assertEquals(1_240_000, GuildBossLogic.bossHp(2));
        assertEquals(SocialDay.weekKey(T2120), GuildBossLogic.weekKey(T2120));
    }

    @Test
    public void bossAttackMarkerIsPerDay() {
        Map<String, Map<String, Boolean>> attacks = new HashMap<>();
        Map<String, Boolean> days = new HashMap<>();
        days.put("2026-11-10", true);
        attacks.put("u1", days);
        assertTrue(GuildBossLogic.attackedToday(attacks, "u1", "2026-11-10"));
        assertFalse(GuildBossLogic.attackedToday(attacks, "u1", "2026-11-11"));
        assertFalse(GuildBossLogic.attackedToday(attacks, "u2", "2026-11-10"));
    }

    private static long utc(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}
