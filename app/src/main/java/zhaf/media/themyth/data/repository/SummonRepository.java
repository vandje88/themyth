package zhaf.media.themyth.data.repository;

import android.content.Context;

import androidx.annotation.StringRes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.gacha.GachaRoller;
import zhaf.media.themyth.data.meta.MissionCatalog;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.GachaPool;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class SummonRepository {

    public enum Payment {
        DIAMOND, TICKET
    }

    public static final int MAX_HISTORY = 100;

    public interface SnapshotCallback {
        void onData(Snapshot snapshot);

        void onError(DatabaseError error);
    }

    public interface PullCallback {
        void onSuccess(Result result);

        void onError(@StringRes int messageRes);
    }

    public static final class PoolProgress {
        public final int epicPity;
        public final int legendaryPity;
        public final long totalPulls;

        public PoolProgress(int epicPity, int legendaryPity, long totalPulls) {
            this.epicPity = epicPity;
            this.legendaryPity = legendaryPity;
            this.totalPulls = totalPulls;
        }

        public static final PoolProgress EMPTY = new PoolProgress(0, 0, 0);
    }

    public static final class HistoryEntry {
        public final String charId;
        public final String rarity;
        public final boolean isNew;
        public final long at;

        HistoryEntry(String charId, String rarity, boolean isNew, long at) {
            this.charId = charId;
            this.rarity = rarity;
            this.isNew = isNew;
            this.at = at;
        }
    }

    public static final class Snapshot {
        public final long diamond;
        public final long ticket;
        public final Map<String, PoolProgress> progress;
        public final Set<String> owned;
        public final Map<String, List<HistoryEntry>> history;

        Snapshot(long diamond, long ticket, Map<String, PoolProgress> progress,
                 Set<String> owned, Map<String, List<HistoryEntry>> history) {
            this.diamond = diamond;
            this.ticket = ticket;
            this.progress = Collections.unmodifiableMap(progress);
            this.owned = Collections.unmodifiableSet(owned);
            this.history = Collections.unmodifiableMap(history);
        }

        public PoolProgress progressOf(String poolId) {
            PoolProgress value = progress.get(poolId);
            return value == null ? PoolProgress.EMPTY : value;
        }

        public List<HistoryEntry> historyOf(String poolId) {
            List<HistoryEntry> value = history.get(poolId);
            return value == null ? Collections.emptyList() : value;
        }
    }

    public static final class PullResult {
        public final String charId;
        public final String rarity;
        public final boolean isNew;
        public final int shards;

        PullResult(String charId, String rarity, boolean isNew, int shards) {
            this.charId = charId;
            this.rarity = rarity;
            this.isNew = isNew;
            this.shards = shards;
        }
    }

    public static final class Result {
        public final List<PullResult> results;
        public final int newCount;
        public final int shardCount;
        public final long diamond;
        public final long ticket;
        public final PoolProgress progress;

        Result(List<PullResult> results, int newCount, int shardCount,
               long diamond, long ticket, PoolProgress progress) {
            this.results = Collections.unmodifiableList(results);
            this.newCount = newCount;
            this.shardCount = shardCount;
            this.diamond = diamond;
            this.ticket = ticket;
            this.progress = progress;
        }
    }

    private SummonRepository() {
    }

    public static Handle observe(String uid, SnapshotCallback callback) {
        DatabaseReference ref = FirebaseRefs.player(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError(null);
                    return;
                }
                Map<String, PoolProgress> progress = new HashMap<>();
                for (DataSnapshot pool : snapshot.child("summons").getChildren()) {
                    if (pool.getKey() == null) {
                        continue;
                    }
                    progress.put(pool.getKey(), new PoolProgress(
                            intOf(pool.child("epicPity").getValue(), 0),
                            intOf(pool.child("legendaryPity").getValue(), 0),
                            longOf(pool.child("totalPulls").getValue(), 0)));
                }

                Set<String> owned = new HashSet<>();
                for (DataSnapshot child : snapshot.child("roster").getChildren()) {
                    if (child.getKey() != null) {
                        owned.add(child.getKey());
                    }
                }

                Map<String, List<HistoryEntry>> history = new HashMap<>();
                for (DataSnapshot pool : snapshot.child("summons").getChildren()) {
                    if (pool.getKey() == null) {
                        continue;
                    }
                    List<HistoryEntry> entries = new ArrayList<>();
                    for (DataSnapshot item : pool.child("history").getChildren()) {
                        String charId = item.child("charId").getValue(String.class);
                        String rarity = item.child("rarity").getValue(String.class);
                        if (charId == null || rarity == null) {
                            continue;
                        }
                        entries.add(new HistoryEntry(
                                charId,
                                rarity,
                                Boolean.TRUE.equals(item.child("isNew").getValue(Boolean.class)),
                                longOf(item.child("at").getValue(), 0L)));
                    }
                    entries.sort((left, right) -> Long.compare(right.at, left.at));
                    if (entries.size() > 30) {
                        entries = new ArrayList<>(entries.subList(0, 30));
                    }
                    history.put(pool.getKey(), entries);
                }

                callback.onData(new Snapshot(
                        longOf(snapshot.child("currencies").child("diamond").getValue(), 0),
                        longOf(snapshot.child("currencies").child("ticketSummon").getValue(), 0),
                        progress, owned, history));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void pull(Context context, String uid, GachaPool pool, int count,
                            Payment payment, PullCallback callback) {
        CharacterCatalog catalog = CharacterCatalog.get(context.getApplicationContext());
        Map<String, List<CharacterTemplate>> byRarity = new HashMap<>();
        for (CharacterTemplate template : catalog.all()) {
            byRarity.computeIfAbsent(template.rarity, key -> new ArrayList<>()).add(template);
        }

        final List<PullResult>[] rolled = new List[1];
        final PoolProgress[] progressOut = new PoolProgress[1];
        final int[] failure = new int[1];

        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                failure[0] = 0;
                if (player.getValue() == null) {
                    failure[0] = R.string.summon_failed;
                    return Transaction.abort();
                }
                if (pool == null || payment == null || (count != 1 && count != 10)) {
                    failure[0] = R.string.summon_failed;
                    return Transaction.abort();
                }
                for (String rarity : GameConstants.RARITY_LADDER) {
                    if (pool.rateOf(rarity) > 0 && byRarity.get(rarity) == null) {
                        failure[0] = R.string.summon_invalid_pool;
                        return Transaction.abort();
                    }
                }

                MutableData currencies = player.child("currencies");
                boolean diamondPayment = payment == Payment.DIAMOND;
                long cost = diamondPayment ? pool.diamondCost(count) : pool.ticketCost(count);
                MutableData wallet = currencies.child(diamondPayment ? "diamond" : "ticketSummon");
                long balance = longOf(wallet.getValue(), 0L);
                if (balance < cost) {
                    failure[0] = diamondPayment
                            ? R.string.summon_not_enough_diamond
                            : R.string.summon_not_enough_ticket;
                    return Transaction.abort();
                }

                MutableData summon = player.child("summons").child(pool.id);
                GachaRoller.Pity pity = new GachaRoller.Pity(
                        intOf(summon.child("epicPity").getValue(), 0),
                        intOf(summon.child("legendaryPity").getValue(), 0));

                Random rng = new Random();
                long now = System.currentTimeMillis();
                CharacterTemplate focus = catalog.find(pool.rateUpCharId);
                List<PullResult> results = new ArrayList<>(count);
                boolean hasRare = false;

                MutableData history = summon.child("history");
                List<String> historyKeys = new ArrayList<>();
                collectHistoryKeys(history, historyKeys);
                Collections.sort(historyKeys);

                for (int i = 0; i < count; i++) {
                    boolean guarantee = i == count - 1 && count == 10
                            && pool.guaranteeRareIn10 && !hasRare;
                    String rarity = guarantee
                            ? GachaRoller.rollRarePlus(pool, rng, pity)
                            : GachaRoller.roll(pool, rng, pity);
                    if (GameConstants.rarityRank(rarity) >= GameConstants.rarityRank("rare")) {
                        hasRare = true;
                    }

                    CharacterTemplate picked = pickCharacter(
                            byRarity.get(rarity), focus, rarity, rng);
                    if (picked == null) {
                        failure[0] = R.string.summon_invalid_pool;
                        return Transaction.abort();
                    }

                    MutableData owned = player.child("roster").child(picked.id);
                    boolean isNew = owned.getValue() == null;
                    int shards = 0;
                    if (isNew) {
                        writeNewCharacter(owned, picked.id, now);
                    } else {
                        shards = GachaPool.duplicateShards(rarity);
                        MutableData shardNode = player.child("charShards").child(picked.id);
                        shardNode.setValue(longOf(shardNode.getValue(), 0L) + shards);
                    }
                    results.add(new PullResult(picked.id, rarity, isNew, shards));

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("charId", picked.id);
                    entry.put("rarity", rarity);
                    entry.put("isNew", isNew);
                    entry.put("at", now);
                    String key = String.format(java.util.Locale.ROOT,
                            "p_%013d_%02d", now, i);
                    history.child(key).setValue(entry);
                    historyKeys.add(key);
                }

                while (historyKeys.size() > MAX_HISTORY) {
                    String oldest = historyKeys.remove(0);
                    history.child(oldest).setValue(null);
                }

                wallet.setValue(balance - cost);
                summon.child("epicPity").setValue(pity.epic);
                summon.child("legendaryPity").setValue(pity.legendary);
                long totalPulls = longOf(summon.child("totalPulls").getValue(), 0L) + count;
                summon.child("totalPulls").setValue(totalPulls);
                summon.child("lastPullAt").setValue(now);

                rolled[0] = results;
                progressOut[0] = new PoolProgress(pity.epic, pity.legendary, totalPulls);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed || rolled[0] == null) {
                    callback.onError(failure[0] != 0 ? failure[0] : R.string.summon_failed);
                    return;
                }
                int newCount = 0;
                int shardCount = 0;
                for (PullResult result : rolled[0]) {
                    if (result.isNew) {
                        newCount++;
                    }
                    shardCount += result.shards;
                }
                MetaRepository.addMissionProgress(uid, MissionCatalog.DAILY_SUMMON, 1,
                        System.currentTimeMillis());
                callback.onSuccess(new Result(
                        rolled[0],
                        newCount,
                        shardCount,
                        longOf(snapshot.child("currencies").child("diamond").getValue(), 0L),
                        longOf(snapshot.child("currencies").child("ticketSummon").getValue(), 0L),
                        progressOut[0] == null ? PoolProgress.EMPTY : progressOut[0]));
            }
        });
    }

    private static void collectHistoryKeys(MutableData history, List<String> out) {
        for (MutableData child : history.getChildren()) {
            if (child.getKey() != null) {
                out.add(child.getKey());
            }
        }
    }

    private static CharacterTemplate pickCharacter(List<CharacterTemplate> candidates,
                                                   CharacterTemplate focus, String rarity,
                                                   Random rng) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        boolean focusRarity = focus != null && focus.rarity.equals(rarity)
                && GameConstants.rarityRank(rarity) >= GameConstants.rarityRank("legendary");
        if (focusRarity && rng.nextInt(10000) < 5000) {
            return focus;
        }
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private static void writeNewCharacter(MutableData node, String charId, long now) {
        node.child("charId").setValue(charId);
        node.child("level").setValue(1);
        node.child("exp").setValue(0);
        node.child("stars").setValue(1);
        node.child("awakening").setValue(0);
        node.child("breakthrough").setValue(0);
        node.child("locked").setValue(false);
        node.child("obtainedAt").setValue(now);
        MutableData skills = node.child("skillLevels");
        skills.child("basic").setValue(1);
        skills.child("skill1").setValue(1);
        skills.child("skill2").setValue(1);
        skills.child("ultimate").setValue(1);
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
