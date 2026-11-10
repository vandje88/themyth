package zhaf.media.themyth.data.arena;

public final class ArenaTickets {

    public static final int MAX = 5;
    public static final long REGEN_MS = 30L * 60L * 1000L;

    private ArenaTickets() {
    }

    public static int effective(int tickets, long ticketAt, long now) {
        int safe = Math.max(0, Math.min(MAX, tickets));
        if (safe >= MAX || ticketAt <= 0L) {
            return safe;
        }
        long elapsed = Math.max(0L, now - ticketAt);
        return (int) Math.min(MAX, safe + elapsed / REGEN_MS);
    }

    public static long anchorAfterRegen(int tickets, long ticketAt, long now) {
        int safe = Math.max(0, Math.min(MAX, tickets));
        if (safe >= MAX || ticketAt <= 0L) {
            return now;
        }
        long elapsed = Math.max(0L, now - ticketAt);
        return ticketAt + (elapsed / REGEN_MS) * REGEN_MS;
    }

    public static long nextTicketAt(int tickets, long ticketAt, long now) {
        int safe = Math.max(0, Math.min(MAX, tickets));
        if (safe >= MAX) {
            return 0L;
        }
        return anchorAfterRegen(safe, ticketAt, now) + REGEN_MS;
    }

    public static Result spend(int tickets, long ticketAt, long now) {
        int available = effective(tickets, ticketAt, now);
        if (available <= 0) {
            return null;
        }
        long anchor = available >= MAX ? now : anchorAfterRegen(tickets, ticketAt, now);
        return new Result(available - 1, anchor);
    }

    public static final class Result {
        public final int tickets;
        public final long ticketAt;

        public Result(int tickets, long ticketAt) {
            this.tickets = tickets;
            this.ticketAt = ticketAt;
        }
    }
}
