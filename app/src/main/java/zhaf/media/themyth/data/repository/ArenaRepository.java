package zhaf.media.themyth.data.repository;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.battle.engine.BattleResult;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.arena.ArenaBattle;
import zhaf.media.themyth.data.arena.ArenaMatchmaker;
import zhaf.media.themyth.data.arena.ArenaRating;
import zhaf.media.themyth.data.arena.ArenaResultGuard;
import zhaf.media.themyth.data.arena.ArenaRewards;
import zhaf.media.themyth.data.arena.ArenaSnapshot;
import zhaf.media.themyth.data.arena.ArenaState;
import zhaf.media.themyth.data.arena.ArenaTickets;

public final class ArenaRepository {

    public static final String ERROR_NO_TICKETS = "no_tickets";
    public static final String ERROR_NETWORK = "network";
    public static final String ERROR_INVALID_RESULT = "invalid_result";
    public static final String ERROR_NO_BATTLE = "no_battle";

    public interface StateCallback {
        void onData(ArenaState state);

        void onError(DatabaseError error);
    }

    public interface SnapshotCallback {
        void onSuccess();

        void onError(String reason);
    }

    public interface MatchCallback {
        void onData(ArenaSnapshot opponent, boolean bot);

        void onError(String reason);
    }

    public interface TicketCallback {
        void onSuccess(int tickets);

        void onError(String reason);
    }

    public interface BattleCallback {
        void onData(ArenaBattle battle);

        void onError(String reason);
    }

    public interface SettlementCallback {
        void onSuccess(Settlement settlement);

        void onError(String reason);
    }

    public interface DefenseCallback {
        void onDone(int applied);

        void onError(String reason);
    }

    public interface LeaderboardCallback {
        void onData(List<LeaderRow> rows);

        void onError(DatabaseError error);
    }

    private ArenaRepository() {
    }

    public static Handle observeState(String uid, StateCallback callback) {
        DatabaseReference ref = FirebaseRefs.arenaState(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(ArenaState.from(snapshot, System.currentTimeMillis()));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void publishSnapshot(ArenaSnapshot snapshot, SnapshotCallback callback) {
        if (snapshot == null) {
            callback.onError(ERROR_NETWORK);
            return;
        }
        FirebaseRefs.arenaSnapshot(snapshot.uid).setValue(snapshot.toMap())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(ERROR_NETWORK);
                    }
                });
    }

    public static void findOpponent(ArenaSnapshot self, long searchingMs,
                                    MatchCallback callback) {
        if (self == null) {
            callback.onError(ERROR_NO_BATTLE);
            return;
        }
        int window = ArenaMatchmaker.window(searchingMs);
        FirebaseRefs.arenaSnapshots()
                .orderByChild("rating")
                .startAt(self.rating - window)
                .endAt(self.rating + window)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<ArenaSnapshot> candidates = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            candidates.add(ArenaSnapshot.from(child));
                        }
                        ArenaSnapshot opponent = ArenaMatchmaker.pick(candidates, self.uid,
                                self.rating, (int) Math.min(Integer.MAX_VALUE, self.cp),
                                searchingMs);
                        if (opponent == null) {
                            ArenaSnapshot bot = ArenaMatchmaker.botFor(self);
                            if (bot == null) {
                                callback.onError(ERROR_NO_BATTLE);
                            } else {
                                callback.onData(bot, true);
                            }
                            return;
                        }
                        callback.onData(opponent, false);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(ERROR_NETWORK);
                    }
                });
    }

    public static void spendTicket(String uid, long now, TicketCallback callback) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData arena = player.child("arena");
                int tickets = intOf(arena.child("tickets").getValue(), ArenaTickets.MAX);
                long ticketAt = longOf(arena.child("ticketAt").getValue(), now);
                ArenaTickets.Result spent = ArenaTickets.spend(tickets, ticketAt, now);
                if (spent == null) {
                    return Transaction.abort();
                }
                arena.child("tickets").setValue(spent.tickets);
                arena.child("ticketAt").setValue(spent.ticketAt);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(ERROR_NO_TICKETS);
                    return;
                }
                callback.onSuccess(intOf(
                        snapshot.child("arena").child("tickets").getValue(), 0));
            }
        });
    }

    public static void createBattle(ArenaSnapshot self, ArenaSnapshot opponent, long seed,
                                    long now, BattleCallback callback) {
        if (self == null || opponent == null || self.uid == null || opponent.uid == null) {
            callback.onError(ERROR_NO_BATTLE);
            return;
        }
        String bid = FirebaseRefs.battles().push().getKey();
        if (bid == null) {
            callback.onError(ERROR_NETWORK);
            return;
        }
        ArenaBattle battle = new ArenaBattle(bid, self.uid, opponent.uid, seed, "PENDING",
                self, opponent, false, -2, 0, 0, 0, null);
        FirebaseRefs.battle(bid).setValue(battle.toCreateMap(now))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onData(battle);
                    } else {
                        callback.onError(ERROR_NETWORK);
                    }
                });
    }

    public static void loadBattle(String uid, String bid, BattleCallback callback) {
        if (uid == null || bid == null) {
            callback.onError(ERROR_NO_BATTLE);
            return;
        }
        FirebaseRefs.battle(bid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArenaBattle battle = ArenaBattle.from(snapshot, bid);
                if (battle == null || battle.a == null || battle.b == null
                        || (!uid.equals(battle.aUid) && !uid.equals(battle.bUid))) {
                    callback.onError(ERROR_NO_BATTLE);
                    return;
                }
                callback.onData(battle);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(ERROR_NETWORK);
            }
        });
    }

    public static void settleBattle(String uid, ArenaBattle battle, BattleResult result,
                                    long now, SettlementCallback callback) {
        if (battle == null || result == null || battle.a == null || battle.b == null
                || !uid.equals(battle.aUid)) {
            callback.onError(ERROR_NO_BATTLE);
            return;
        }
        ArenaResultGuard.Verdict verdict = ArenaResultGuard.validate("PENDING",
                result.winner, result.ticks, result.teamHpPctA, result.teamHpPctB);
        if (verdict != ArenaResultGuard.Verdict.OK) {
            callback.onError(ERROR_INVALID_RESULT);
            return;
        }

        boolean win = result.winner == 0;
        boolean draw = result.winner < 0;
        boolean bot = ArenaMatchmaker.isBot(battle.b.uid);
        double score = win ? 1.0 : draw ? 0.5 : 0.0;
        String season = ArenaRating.seasonKey(now);
        SettleMath math = new SettleMath();

        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData arena = player.child("arena");
                if (arena.child("settled").child(battle.bid).getValue() != null) {
                    math.already = true;
                    return Transaction.success(player);
                }
                int rating = intOf(arena.child("rating").getValue(), ArenaRating.START_RATING);
                int wins = intOf(arena.child("wins").getValue(), 0);
                int losses = intOf(arena.child("losses").getValue(), 0);
                int streak = intOf(arena.child("streak").getValue(), 0);
                int points = intOf(arena.child("points").getValue(), 0);

                int delta = ArenaRating.deltaScore(rating, battle.b.rating, score, streak, bot);
                int nextRating = ArenaRating.apply(rating, delta);
                int gainedPoints = ArenaRewards.points(win, delta);
                int gainedCoin;
                if (win) {
                    wins++;
                    streak++;
                    gainedCoin = ArenaRewards.WIN_COIN;
                } else if (draw) {
                    streak = 0;
                    gainedCoin = (ArenaRewards.WIN_COIN + ArenaRewards.LOSS_COIN) / 2;
                } else {
                    losses++;
                    streak = 0;
                    gainedCoin = ArenaRewards.LOSS_COIN;
                }

                arena.child("rating").setValue(nextRating);
                arena.child("wins").setValue(wins);
                arena.child("losses").setValue(losses);
                arena.child("streak").setValue(streak);
                arena.child("points").setValue(points + gainedPoints);
                arena.child("settled").child(battle.bid).setValue(true);

                MutableData currencies = player.child("currencies");
                long coin = longOf(currencies.child("coin").getValue(), 0L);
                currencies.child("coin").setValue(coin + gainedCoin);

                math.delta = delta;
                math.coin = gainedCoin;
                math.points = gainedPoints;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(ERROR_NETWORK);
                    return;
                }
                int rating = intOf(snapshot.child("arena").child("rating").getValue(),
                        ArenaRating.START_RATING);
                int wins = intOf(snapshot.child("arena").child("wins").getValue(), 0);
                int losses = intOf(snapshot.child("arena").child("losses").getValue(), 0);
                Settlement settlement = new Settlement(
                        rating, math.already ? 0 : math.delta,
                        math.already ? 0 : math.coin,
                        math.already ? 0 : math.points, win, draw, bot);

                writeSettlement(uid, battle, result, season, rating, wins, losses,
                        now, settlement, callback);
            }
        });
    }

    private static void writeSettlement(String uid, ArenaBattle battle, BattleResult result,
                                        String season, int rating, int wins, int losses,
                                        long now, Settlement settlement,
                                        SettlementCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("battles/" + battle.bid + "/result", battle.toResultMap(
                result.winner, result.ticks, result.teamHpPctA, result.teamHpPctB,
                result.hash, now));
        update.put("battles/" + battle.bid + "/status", "SETTLED");

        Map<String, Object> ranking = new HashMap<>();
        ranking.put("uid", uid);
        ranking.put("username", battle.a.username);
        ranking.put("score", rating);
        ranking.put("wins", wins);
        ranking.put("losses", losses);
        ranking.put("cp", battle.a.cp);
        ranking.put("updatedAt", now);
        update.put("rankings/" + season + "/arena/" + uid, ranking);

        if (!ArenaMatchmaker.isBot(battle.b.uid)) {
            boolean defenderWin = result.winner == 1;
            int defenderDelta = ArenaRating.delta(battle.b.rating, battle.a.rating,
                    defenderWin, 0, false);
            Map<String, Object> inbox = new HashMap<>();
            inbox.put("winner", result.winner);
            inbox.put("ratingDelta", defenderDelta);
            inbox.put("by", uid);
            inbox.put("at", now);
            inbox.put("rating", battle.b.rating);
            update.put("players/" + battle.b.uid + "/arenaInbox/" + battle.bid, inbox);
        }

        FirebaseRefs.root().updateChildren(update, (error, ref) -> {
            if (error == null || battle.settled) {
                callback.onSuccess(settlement);
            } else {
                callback.onError(ERROR_NETWORK);
            }
        });
    }

    public static void claimDefense(String uid, long now, DefenseCallback callback) {
        FirebaseRefs.arenaInbox(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<DataSnapshot> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    entries.add(child);
                }
                claimNext(uid, entries.iterator(), now, 0, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(ERROR_NETWORK);
            }
        });
    }

    private static void claimNext(String uid, Iterator<DataSnapshot> entries, long now,
                                  int applied, DefenseCallback callback) {
        if (!entries.hasNext()) {
            callback.onDone(applied);
            return;
        }
        DataSnapshot entry = entries.next();
        String bid = entry.getKey();
        String by = entry.child("by").getValue(String.class);
        int winner = intOf(entry.child("winner").getValue(), -9);
        int delta = intOf(entry.child("ratingDelta").getValue(), 0);
        if (bid == null || by == null || winner < -1 || winner > 1 || delta == 0) {
            dropEntry(uid, bid, () -> claimNext(uid, entries, now, applied, callback));
            return;
        }
        FirebaseRefs.battle(bid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean valid = snapshot.exists()
                        && by.equals(snapshot.child("aUid").getValue(String.class))
                        && uid.equals(snapshot.child("bUid").getValue(String.class))
                        && by.equals(snapshot.child("result").child("by").getValue(String.class))
                        && intOf(snapshot.child("result").child("winner").getValue(), -9) == winner;
                if (!valid) {
                    dropEntry(uid, bid, () -> claimNext(uid, entries, now, applied, callback));
                    return;
                }
                applyDefense(uid, bid, winner, delta, applied, entries, now, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(ERROR_NETWORK);
            }
        });
    }

    private static void applyDefense(String uid, String bid, int winner, int delta, int applied,
                                     Iterator<DataSnapshot> entries, long now,
                                     DefenseCallback callback) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData arena = player.child("arena");
                if (arena.child("defended").child(bid).getValue() != null) {
                    return Transaction.success(player);
                }
                int rating = intOf(arena.child("rating").getValue(), ArenaRating.START_RATING);
                int wins = intOf(arena.child("wins").getValue(), 0);
                int losses = intOf(arena.child("losses").getValue(), 0);
                int streak = intOf(arena.child("streak").getValue(), 0);

                if (winner == 1) {
                    wins++;
                    streak++;
                } else if (winner == 0) {
                    losses++;
                    streak = 0;
                } else {
                    streak = 0;
                }
                arena.child("rating").setValue(ArenaRating.apply(rating, delta));
                arena.child("wins").setValue(wins);
                arena.child("losses").setValue(losses);
                arena.child("streak").setValue(streak);
                arena.child("defended").child(bid).setValue(true);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(ERROR_NETWORK);
                    return;
                }
                dropEntry(uid, bid, () -> claimNext(uid, entries, now, applied + 1, callback));
            }
        });
    }

    private static void dropEntry(String uid, String bid, Runnable next) {
        if (bid == null) {
            next.run();
            return;
        }
        FirebaseRefs.arenaInbox(uid).child(bid).removeValue()
                .addOnCompleteListener(task -> next.run());
    }

    public static void loadLeaderboard(String season, LeaderboardCallback callback) {
        FirebaseRefs.rankings(season, "arena")
                .orderByChild("score")
                .limitToLast(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<LeaderRow> rows = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String uid = child.child("uid").getValue(String.class);
                            if (uid == null) {
                                continue;
                            }
                            rows.add(new LeaderRow(uid,
                                    child.child("username").getValue(String.class),
                                    intOf(child.child("score").getValue(), 0)));
                        }
                        List<LeaderRow> ordered = new ArrayList<>();
                        for (int i = rows.size() - 1; i >= 0; i--) {
                            ordered.add(rows.get(i));
                        }
                        callback.onData(ordered);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    private static final class SettleMath {
        boolean already;
        int delta;
        int coin;
        int points;
    }

    public static final class Settlement {
        public final int rating;
        public final int delta;
        public final int coin;
        public final int points;
        public final boolean win;
        public final boolean draw;
        public final boolean bot;

        Settlement(int rating, int delta, int coin, int points,
                   boolean win, boolean draw, boolean bot) {
            this.rating = rating;
            this.delta = delta;
            this.coin = coin;
            this.points = points;
            this.win = win;
            this.draw = draw;
            this.bot = bot;
        }
    }

    public static final class LeaderRow {
        public final String uid;
        public final String username;
        public final int score;

        LeaderRow(String uid, String username, int score) {
            this.uid = uid;
            this.username = username == null ? "Pemanggil" : username;
            this.score = score;
        }
    }

    public static final class Handle {
        private final DatabaseReference ref;
        private final ValueEventListener listener;

        Handle(DatabaseReference ref, ValueEventListener listener) {
            this.ref = ref;
            this.listener = listener;
        }

        public void detach() {
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
