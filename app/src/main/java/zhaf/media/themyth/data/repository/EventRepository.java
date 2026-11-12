package zhaf.media.themyth.data.repository;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.event.EventCatalog;
import zhaf.media.themyth.data.event.EventDef;
import zhaf.media.themyth.data.event.EventLogic;
import zhaf.media.themyth.data.event.EventMission;
import zhaf.media.themyth.data.event.EventReward;
import zhaf.media.themyth.data.event.PlayerEvent;

public final class EventRepository {

    public static final String ERROR_ALREADY = "already";
    public static final String ERROR_NOT_READY = "not_ready";
    public static final String ERROR_CLOSED = "closed";
    public static final String ERROR_NETWORK = "network";

    public interface EventCallback {
        void onData(PlayerEvent event);

        void onError(DatabaseError error);
    }

    public interface ActionResult {
        void onSuccess();

        void onError(String reason);
    }

    private EventRepository() {
    }

    public static Handle observe(String uid, String eventId, EventCallback callback) {
        DatabaseReference ref = FirebaseRefs.metaEvent(uid, eventId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(PlayerEvent.from(eventId, snapshot));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void addProgress(String uid, String missionId, int amount, long now) {
        EventDef event = EventLogic.activeEvent(now);
        EventMission mission = EventCatalog.findMission(missionId);
        if (event == null || mission == null || amount <= 0) {
            return;
        }
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData progress = node(player, event.id).child("progress");
                MutableData counter = progress.child(mission.id);
                int current = intOf(counter.getValue(), 0);
                counter.setValue(EventLogic.addProgress(current, amount, mission.target));
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                // Hooks retry naturally with the next action.
            }
        });
    }

    public static void claimMission(String uid, EventDef event, EventMission mission,
                                    long now, ActionResult callback) {
        if (event == null || mission == null) {
            callback.onError(ERROR_NETWORK);
            return;
        }
        final String[] failure = new String[1];
        final boolean[] done = new boolean[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    failure[0] = ERROR_NETWORK;
                    return Transaction.abort();
                }
                if (!EventLogic.claimOpen(event, now)) {
                    failure[0] = ERROR_CLOSED;
                    return Transaction.abort();
                }
                MutableData node = node(player, event.id);
                int progress = intOf(node.child("progress").child(mission.id).getValue(), 0);
                boolean claimed = Boolean.TRUE.equals(
                        node.child("claims").child(mission.id).getValue(Boolean.class));
                if (claimed) {
                    failure[0] = ERROR_ALREADY;
                    return Transaction.abort();
                }
                if (!EventLogic.canClaim(progress, mission.target, false, true)) {
                    failure[0] = ERROR_NOT_READY;
                    return Transaction.abort();
                }
                node.child("coin").setValue(intOf(node.child("coin").getValue(), 0)
                        + mission.eventCoin);
                node.child("claims").child(mission.id).setValue(true);
                done[0] = true;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || !done[0]) {
                    callback.onError(failure[0] == null ? ERROR_NETWORK : failure[0]);
                    return;
                }
                callback.onSuccess();
            }
        });
    }

    public static void redeem(String uid, EventDef event, EventReward reward,
                              long now, ActionResult callback) {
        if (event == null || reward == null) {
            callback.onError(ERROR_NETWORK);
            return;
        }
        final String[] failure = new String[1];
        final boolean[] done = new boolean[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    failure[0] = ERROR_NETWORK;
                    return Transaction.abort();
                }
                if (!EventLogic.claimOpen(event, now)) {
                    failure[0] = ERROR_CLOSED;
                    return Transaction.abort();
                }
                MutableData node = node(player, event.id);
                int balance = intOf(node.child("coin").getValue(), 0);
                int redeemed = intOf(node.child("redeemed").child(reward.id).getValue(), 0);
                if (redeemed >= reward.limit) {
                    failure[0] = ERROR_ALREADY;
                    return Transaction.abort();
                }
                if (balance < reward.cost) {
                    failure[0] = ERROR_NOT_READY;
                    return Transaction.abort();
                }
                node.child("coin").setValue(balance - reward.cost);
                node.child("redeemed").child(reward.id).setValue(redeemed + 1);
                addCurrencies(player.child("currencies"),
                        reward.coin, reward.diamond, reward.tickets);
                done[0] = true;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || !done[0]) {
                    callback.onError(failure[0] == null ? ERROR_NETWORK : failure[0]);
                    return;
                }
                callback.onSuccess();
            }
        });
    }

    private static MutableData node(MutableData player, String eventId) {
        return player.child("meta").child("events").child(eventId);
    }

    private static void addCurrencies(MutableData currencies, int coin, int diamond,
                                      int tickets) {
        addCurrency(currencies, "coin", coin, 999999999L);
        addCurrency(currencies, "diamond", diamond, 9999999L);
        addCurrency(currencies, "ticketSummon", tickets, 999L);
    }

    private static void addCurrency(MutableData currencies, String key, int delta, long max) {
        if (delta <= 0) {
            return;
        }
        long current = longOf(currencies.child(key).getValue(), 0L);
        currencies.child(key).setValue(Math.min(max, current + delta));
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
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
}
