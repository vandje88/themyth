package zhaf.media.themyth.data.arena;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ArenaTicketsTest {

    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void missingAnchorKeepsStoredCount() {
        assertEquals(2, ArenaTickets.effective(2, 0L, NOW));
    }

    @Test
    public void regenAddsOneTicketEveryThirtyMinutes() {
        assertEquals(1, ArenaTickets.effective(1, NOW - 1L, NOW));
        assertEquals(2, ArenaTickets.effective(1, NOW - ArenaTickets.REGEN_MS, NOW));
        assertEquals(3, ArenaTickets.effective(1, NOW - ArenaTickets.REGEN_MS * 2, NOW));
        assertEquals(5, ArenaTickets.effective(1, NOW - ArenaTickets.REGEN_MS * 9, NOW));
    }

    @Test
    public void spendFailsWithoutTickets() {
        assertNull(ArenaTickets.spend(0, NOW, NOW));
    }

    @Test
    public void spendFromFullStartsCountdownNow() {
        ArenaTickets.Result result = ArenaTickets.spend(5, NOW - 5_000_000L, NOW);
        assertNotNull(result);
        assertEquals(4, result.tickets);
        assertEquals(NOW, result.ticketAt);
    }

    @Test
    public void spendKeepsRegenAnchorWhenPartlyFilled() {
        long anchor = NOW - ArenaTickets.REGEN_MS - 60_000L;
        ArenaTickets.Result result = ArenaTickets.spend(2, anchor, NOW);
        assertNotNull(result);
        assertEquals(2, result.tickets);
        assertEquals(anchor + ArenaTickets.REGEN_MS, result.ticketAt);
    }

    @Test
    public void nextTicketTimeIsOneRegenAway() {
        long anchor = NOW - 10L * 60L * 1000L;
        assertEquals(anchor + ArenaTickets.REGEN_MS,
                ArenaTickets.nextTicketAt(1, anchor, NOW));
        assertEquals(0L, ArenaTickets.nextTicketAt(5, NOW, NOW));
    }
}
