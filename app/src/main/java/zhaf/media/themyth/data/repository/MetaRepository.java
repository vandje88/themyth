package zhaf.media.themyth.data.repository;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.meta.DailyLoginLogic;
import zhaf.media.themyth.data.meta.IdleReward;
import zhaf.media.themyth.data.meta.IdleRewardLogic;
import zhaf.media.themyth.data.meta.LoginReward;
import zhaf.media.themyth.data.meta.MetaClock;
import zhaf.media.themyth.data.meta.MissionCatalog;
import zhaf.media.themyth.data.meta.MissionDef;
import zhaf.media.themyth.data.meta.MissionLogic;
import zhaf.media.themyth.data.meta.PlayerMeta;

public final class MetaRepository {

    public static final String ERROR_ALREADY = "already";
    public static final String ERROR_NOT_READY = "not_ready";
    public static final String ERROR_NETWORK = "network";

    public interface MetaCallback {
        void onData(PlayerMeta meta);

        void onError(DatabaseError error);
    }

    public interface ClaimCallback {
        void onSuccess(ClaimResult result);

        void onError(String reason);
    }

    public static final class ClaimResult {
        public final int coin;
        public final int diamond;
        public final int energy;
        public final int tickets;
        public final int streak;

        ClaimResult(int coin, int diamond, int energy, int tickets, int streak) {
            this.coin = coin;
            this.diamond = diamond;
            this.energy = energy;
            this.tickets = tickets;
            this.streak = streak;
        }
    }

    private MetaRepository() {
    }

    public static Handle observe(String uid, MetaCallback callback) {
        DatabaseReference ref = FirebaseRefs.meta(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(PlayerMeta.from(snapshot));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void touch(String uid, long now) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData meta = player.child("meta");
                meta.child("lastSeen").setValue(now);
                if (meta.child("idle").child("at").getValue() == null) {
                    meta.child("idle").child("at").setValue(now);
                }
                player.child("profile").child("lastOnline").setValue(now);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                // Best-effort presence update.
            }
        });
    }

    public static void claimLogin(String uid, long now, ClaimCallback callback) {
        final String[] failure = new String[1];
        final LoginReward[] rewardOut = new LoginReward[1];
        final int[] streakOut = new int[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    failure[0] = ERROR_NETWORK;
                    return Transaction.abort();
                }
                MutableData login = player.child("meta").child("login");
                String lastDay = login.child("lastDay").getValue(String.class);
                int streak = intOf(login.child("streak").getValue(), 0);
                if (!DailyLoginLogic.canClaim(lastDay, now)) {
                    failure[0] = ERROR_ALREADY;
                    return Transaction.abort();
                }
                int nextStreak = DailyLoginLogic.nextStreak(lastDay, streak, now);
                LoginReward reward = DailyLoginLogic.rewardFor(nextStreak);
                addCurrencies(player.child("currencies"),
                        reward.coin, reward.diamond, reward.energy, reward.tickets);
                login.child("lastDay").setValue(MetaClock.dayKey(now));
                login.child("streak").setValue(nextStreak);
                login.child("lastClaimAt").setValue(now);
                rewardOut[0] = reward;
                streakOut[0] = nextStreak;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || rewardOut[0] == null) {
                    callback.onError(failure[0] == null ? ERROR_NETWORK : failure[0]);
                    return;
                }
                LoginReward reward = rewardOut[0];
                callback.onSuccess(new ClaimResult(reward.coin, reward.diamond,
                        reward.energy, reward.tickets, streakOut[0]));
            }
        });
    }

    public static void claimIdle(String uid, long now, int level, ClaimCallback callback) {
        final String[] failure = new String[1];
        final IdleReward[] rewardOut = new IdleReward[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    failure[0] = ERROR_NETWORK;
                    return Transaction.abort();
                }
                MutableData idle = player.child("meta").child("idle");
                Object atValue = idle.child("at").getValue();
                if (!(atValue instanceof Number)) {
                    idle.child("at").setValue(now);
                    failure[0] = ERROR_NOT_READY;
                    return Transaction.abort();
                }
                long since = ((Number) atValue).longValue();
                long accrued = IdleRewardLogic.accruedMs(since, now, level);
                if (!IdleRewardLogic.ready(accrued)) {
                    failure[0] = ERROR_NOT_READY;
                    return Transaction.abort();
                }
                IdleReward reward = IdleRewardLogic.reward(accrued, level);
                addCurrencies(player.child("currencies"), reward.coin, 0, reward.energy, 0);
                long cap = IdleRewardLogic.capMs(level);
                long nextAt = accrued >= cap ? now : now - (accrued % 60000L);
                idle.child("at").setValue(nextAt);
                idle.child("lastClaim").setValue(now);
                rewardOut[0] = reward;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || rewardOut[0] == null) {
                    callback.onError(failure[0] == null ? ERROR_NETWORK : failure[0]);
                    return;
                }
                IdleReward reward = rewardOut[0];
                callback.onSuccess(new ClaimResult(reward.coin, 0, reward.energy, 0, 0));
            }
        });
    }

    public static void addMissionProgress(String uid, String missionId, int amount, long now) {
        MissionDef def = MissionCatalog.find(missionId);
        if (def == null || amount <= 0) {
            return;
        }
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData progress = progressNode(player, def, now);
                MutableData mission = progress.child(def.id);
                int current = intOf(mission.getValue(), 0);
                mission.setValue(MissionLogic.addProgress(current, amount, def.target));
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                // Hooks retry naturally with the next action.
            }
        });
    }

    public static void claimMission(String uid, MissionDef def, long now,
                                    ClaimCallback callback) {
        if (def == null) {
            callback.onError(ERROR_NETWORK);
            return;
        }
        final String[] failure = new String[1];
        final boolean[] claimed = new boolean[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    failure[0] = ERROR_NETWORK;
                    return Transaction.abort();
                }
                MutableData progress = progressNode(player, def, now);
                int current = intOf(progress.child(def.id).getValue(), 0);
                String claimKey = PlayerMeta.claimKey(def, now);
                MutableData claim = player.child("meta").child("claims").child(claimKey);
                if (claim.getValue() != null || !MissionLogic.canClaim(current, def.target, false)) {
                    failure[0] = claim.getValue() != null ? ERROR_ALREADY : ERROR_NOT_READY;
                    return Transaction.abort();
                }
                addCurrencies(player.child("currencies"),
                        def.coin, def.diamond, def.energy, def.tickets);
                claim.setValue(true);
                claimed[0] = true;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || !claimed[0]) {
                    callback.onError(failure[0] == null ? ERROR_NETWORK : failure[0]);
                    return;
                }
                callback.onSuccess(new ClaimResult(def.coin, def.diamond,
                        def.energy, def.tickets, 0));
            }
        });
    }

    public static void recordBossAttack(String uid) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData meta = player.child("meta");
                meta.child("bossAttacks").setValue(
                        intOf(meta.child("bossAttacks").getValue(), 0) + 1);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                // Best-effort badge counter.
            }
        });
    }

    public static void recordRating(String uid, int rating) {
        FirebaseRefs.meta(uid).child("bestRating").runTransaction(
                new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData best) {
                        int current = intOf(best.getValue(), 0);
                        best.setValue(Math.max(current, rating));
                        return Transaction.success(best);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed,
                                           DataSnapshot snapshot) {
                        // Best-effort high score.
                    }
                });
    }

    public static void setTitle(String uid, String titleId) {
        FirebaseRefs.profile(uid).child("titleId").setValue(titleId);
    }

    public static void setBadge(String uid, String badgeId) {
        FirebaseRefs.profile(uid).child("badgeId").setValue(badgeId);
    }

    private static MutableData progressNode(MutableData player, MissionDef def, long now) {
        MutableData missions = player.child("meta").child("missions");
        return def.daily()
                ? missions.child("d").child(MetaClock.dayKey(now))
                : missions.child("w").child(MetaClock.weekKey(now));
    }

    private static void addCurrencies(MutableData currencies, int coin, int diamond,
                                      int energy, int tickets) {
        addCurrency(currencies, "coin", coin, 999999999L);
        addCurrency(currencies, "diamond", diamond, 9999999L);
        addCurrency(currencies, "energy", energy, 999L);
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
