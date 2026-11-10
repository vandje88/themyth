package zhaf.media.themyth.data.repository;

import androidx.annotation.StringRes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.model.ShopLimit;
import zhaf.media.themyth.data.staticdata.ShopOffer;

public final class ShopRepository {

    private static final int RESET_HOUR = 4;

    public interface SnapshotCallback {
        void onData(Snapshot snapshot);

        void onError(DatabaseError error);
    }

    public interface PurchaseCallback {
        void onSuccess(PurchaseResult result);

        void onError(@StringRes int messageRes);
    }

    public static final class Snapshot {
        public final long coin;
        public final long diamond;
        public final Map<String, ShopLimit> limits;

        public Snapshot(long coin, long diamond, Map<String, ShopLimit> limits) {
            this.coin = coin;
            this.diamond = diamond;
            this.limits = Collections.unmodifiableMap(limits);
        }

        public ShopLimit limitOf(String offerId) {
            ShopLimit limit = limits.get(offerId);
            return limit == null ? new ShopLimit(0, 0L) : limit;
        }
    }

    public static final class PurchaseResult {
        public final long coin;
        public final long diamond;
        public final int used;
        public final int limit;
        public final long resetAt;

        PurchaseResult(long coin, long diamond, int used, int limit, long resetAt) {
            this.coin = coin;
            this.diamond = diamond;
            this.used = used;
            this.limit = limit;
            this.resetAt = resetAt;
        }
    }

    private ShopRepository() {
    }

    public static Handle observe(String uid, SnapshotCallback callback) {
        DatabaseReference ref = FirebaseRefs.player(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, ShopLimit> limits = new HashMap<>();
                for (DataSnapshot child : snapshot.child("shopLimits").getChildren()) {
                    if (child.getKey() != null) {
                        limits.put(child.getKey(), ShopLimit.from(child));
                    }
                }
                callback.onData(new Snapshot(
                        longOf(snapshot.child("currencies").child("coin").getValue(), 0L),
                        longOf(snapshot.child("currencies").child("diamond").getValue(), 0L),
                        limits));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void purchase(String uid, ShopOffer offer, PurchaseCallback callback) {
        final int[] failure = new int[1];
        final int[] usedOut = new int[1];
        final long[] resetOut = new long[1];
        final long[] coinOut = new long[1];
        final long[] diamondOut = new long[1];

        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                failure[0] = 0;
                if (player.getValue() == null || offer == null || offer.price <= 0) {
                    failure[0] = R.string.shop_failed;
                    return Transaction.abort();
                }

                long now = System.currentTimeMillis();
                MutableData limitNode = player.child("shopLimits").child(offer.id);
                int used = intOf(limitNode.child("count").getValue(), 0);
                long resetAt = longOf(limitNode.child("resetAt").getValue(), 0L);
                if (resetAt <= 0 || now >= resetAt) {
                    used = 0;
                    resetAt = nextResetAt(now);
                    limitNode.child("resetAt").setValue(resetAt);
                }
                if (used >= offer.dailyLimit) {
                    failure[0] = R.string.shop_limit_reached;
                    return Transaction.abort();
                }

                boolean diamond = offer.costsDiamond();
                MutableData wallet = player.child("currencies").child(diamond ? "diamond" : "coin");
                long balance = longOf(wallet.getValue(), 0L);
                if (balance < offer.price) {
                    failure[0] = diamond
                            ? R.string.shop_not_enough_diamond
                            : R.string.shop_not_enough_coin;
                    return Transaction.abort();
                }
                wallet.setValue(balance - offer.price);

                if (offer.isCurrencyItem()) {
                    MutableData currency = player.child("currencies").child(offer.itemId);
                    currency.setValue(longOf(currency.getValue(), 0L) + offer.qty);
                } else {
                    MutableData item = player.child("inventory").child(offer.itemId);
                    int qty = intOf(item.child("qty").getValue(), 0);
                    item.child("qty").setValue(qty + offer.qty);
                    if (item.child("locked").getValue() == null) {
                        item.child("locked").setValue(false);
                    }
                }

                limitNode.child("count").setValue(used + 1);

                usedOut[0] = used + 1;
                resetOut[0] = resetAt;
                coinOut[0] = diamond
                        ? longOf(player.child("currencies").child("coin").getValue(), 0L)
                        : balance - offer.price;
                diamondOut[0] = diamond
                        ? balance - offer.price
                        : longOf(player.child("currencies").child("diamond").getValue(), 0L);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(failure[0] != 0
                            ? failure[0] : R.string.shop_failed);
                    return;
                }
                callback.onSuccess(new PurchaseResult(
                        coinOut[0], diamondOut[0], usedOut[0],
                        offer.dailyLimit, resetOut[0]));
            }
        });
    }

    public static long nextResetAt(long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
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
