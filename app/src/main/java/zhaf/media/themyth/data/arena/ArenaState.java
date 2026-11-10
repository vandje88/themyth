package zhaf.media.themyth.data.arena;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

public final class ArenaState {

    public final int rating;
    public final int wins;
    public final int losses;
    public final int streak;
    public final int points;
    public final int tickets;
    public final long ticketAt;

    public ArenaState(int rating, int wins, int losses, int streak, int points,
                      int tickets, long ticketAt) {
        this.rating = rating;
        this.wins = wins;
        this.losses = losses;
        this.streak = streak;
        this.points = points;
        this.tickets = tickets;
        this.ticketAt = ticketAt;
    }

    public static ArenaState defaults(long now) {
        return new ArenaState(ArenaRating.START_RATING, 0, 0, 0, 0,
                ArenaTickets.MAX, now);
    }

    public static ArenaState from(DataSnapshot snapshot, long now) {
        if (snapshot == null || !snapshot.exists()) {
            return defaults(now);
        }
        return new ArenaState(
                intOf(snapshot.child("rating").getValue(), ArenaRating.START_RATING),
                intOf(snapshot.child("wins").getValue(), 0),
                intOf(snapshot.child("losses").getValue(), 0),
                intOf(snapshot.child("streak").getValue(), 0),
                intOf(snapshot.child("points").getValue(), 0),
                intOf(snapshot.child("tickets").getValue(), ArenaTickets.MAX),
                longOf(snapshot.child("ticketAt").getValue(), now)
        );
    }

    public int effectiveTickets(long now) {
        return ArenaTickets.effective(tickets, ticketAt, now);
    }

    public String tier() {
        return ArenaRating.tier(rating);
    }

    public ArenaState with(int rating, int wins, int losses, int streak, int points,
                           int tickets, long ticketAt) {
        return new ArenaState(rating, wins, losses, streak, points, tickets, ticketAt);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("rating", rating);
        map.put("wins", wins);
        map.put("losses", losses);
        map.put("streak", streak);
        map.put("points", points);
        map.put("tickets", tickets);
        map.put("ticketAt", ticketAt);
        return map;
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
