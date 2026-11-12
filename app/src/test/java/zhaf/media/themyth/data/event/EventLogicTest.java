package zhaf.media.themyth.data.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

public class EventLogicTest {

    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long DURING_FESTIVAL = Instant.parse("2026-11-10T12:00:00Z").toEpochMilli();
    private static final long DURING_SHADOW = Instant.parse("2026-12-05T12:00:00Z").toEpochMilli();
    private static final long AFTER_ALL = Instant.parse("2027-01-05T12:00:00Z").toEpochMilli();

    @Test
    public void eventWindowBoundaries() {
        EventDef festival = EventCatalog.findEvent(EventCatalog.EVENT_FESTIVAL);
        assertNotNull(festival);
        assertFalse(EventLogic.active(festival, festival.startsAt - 1));
        assertTrue(EventLogic.active(festival, festival.startsAt));
        assertTrue(EventLogic.active(festival, festival.endsAt));
        assertFalse(EventLogic.active(festival, festival.endsAt + 1));

        assertTrue(EventLogic.claimOpen(festival, festival.endsAt + EventLogic.CLAIM_GRACE_MS));
        assertFalse(EventLogic.claimOpen(festival,
                festival.endsAt + EventLogic.CLAIM_GRACE_MS + 1));
        assertFalse(EventLogic.claimOpen(festival, festival.startsAt - 1));
        assertEquals(festival.endsAt + EventLogic.CLAIM_GRACE_MS,
                EventLogic.claimDeadline(festival));
    }

    @Test
    public void activeEventFollowsSchedule() {
        assertEquals(EventCatalog.EVENT_FESTIVAL,
                EventLogic.activeEvent(DURING_FESTIVAL).id);
        assertEquals(EventCatalog.EVENT_SHADOW,
                EventLogic.activeEvent(DURING_SHADOW).id);
        assertNull(EventLogic.activeEvent(AFTER_ALL));
        assertNull(EventLogic.activeEvent(0L));
    }

    @Test
    public void nextEventSkipsStartedOnes() {
        EventDef festival = EventCatalog.findEvent(EventCatalog.EVENT_FESTIVAL);
        EventDef shadow = EventCatalog.findEvent(EventCatalog.EVENT_SHADOW);
        assertEquals(festival.id, EventLogic.nextEvent(festival.startsAt - DAY).id);
        assertEquals(shadow.id, EventLogic.nextEvent(DURING_FESTIVAL).id);
        assertNull(EventLogic.nextEvent(AFTER_ALL));
    }

    @Test
    public void eventsDoNotOverlap() {
        EventDef festival = EventCatalog.findEvent(EventCatalog.EVENT_FESTIVAL);
        EventDef shadow = EventCatalog.findEvent(EventCatalog.EVENT_SHADOW);
        assertTrue(festival.endsAt < shadow.startsAt);
        assertEquals(2, EventCatalog.events().size());
        for (EventDef event : EventCatalog.events()) {
            assertFalse(event.title().isEmpty());
            assertFalse(event.description().isEmpty());
            assertFalse(event.coinName().isEmpty());
            assertTrue(event.endsAt > event.startsAt);
        }
    }

    @Test
    public void missionCatalogShape() {
        assertEquals(5, EventCatalog.missions().size());
        assertEquals(3, EventCatalog.rewards().size());
        int total = 0;
        for (EventMission mission : EventCatalog.missions()) {
            assertTrue(mission.target >= 1);
            assertTrue(mission.eventCoin > 0);
            assertFalse(mission.title().isEmpty());
            assertFalse(mission.description().isEmpty());
            total += mission.eventCoin;
        }
        assertEquals(400, total);
        assertEquals(EventCatalog.MISSION_WIN, EventCatalog.findMission("event_win").id);
        assertNull(EventCatalog.findMission("missing"));
        for (EventReward reward : EventCatalog.rewards()) {
            assertTrue(reward.cost >= 1);
            assertTrue(reward.limit >= 1);
            assertTrue(reward.coin + reward.diamond + reward.tickets > 0);
        }
    }

    @Test
    public void progressClampAndClaimRules() {
        EventMission mission = EventCatalog.findMission(EventCatalog.MISSION_WIN);
        assertEquals(1, EventLogic.addProgress(0, 1, mission.target));
        assertEquals(mission.target, EventLogic.addProgress(4, 3, mission.target));
        assertEquals(0, EventLogic.addProgress(0, -2, mission.target));
        assertTrue(EventLogic.canClaim(mission.target, mission.target, false, true));
        assertFalse(EventLogic.canClaim(mission.target, mission.target, true, true));
        assertFalse(EventLogic.canClaim(mission.target - 1, mission.target, false, true));
        assertFalse(EventLogic.canClaim(mission.target, mission.target, false, false));
    }

    @Test
    public void redeemRules() {
        EventReward reward = EventCatalog.findReward("ev_diamond");
        assertTrue(EventLogic.canRedeem(reward.cost, reward.cost, 0, reward.limit, true));
        assertFalse(EventLogic.canRedeem(reward.cost, reward.cost - 1, 0, reward.limit, true));
        assertFalse(EventLogic.canRedeem(reward.cost, 9999, reward.limit, reward.limit, true));
        assertFalse(EventLogic.canRedeem(reward.cost, 9999, 0, reward.limit, false));
        assertTrue(EventLogic.canRedeem(reward.cost, 9999, reward.limit - 1,
                reward.limit, true));
    }
}
