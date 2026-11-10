package zhaf.media.themyth.data.repository;

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
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.social.MailLogic;
import zhaf.media.themyth.data.social.MailMessage;

public final class MailRepository {

    public static final String SEED_WELCOME = "welcome_01";
    public static final String SEED_EVENT = "event_social_01";

    public interface MailCallback {
        void onData(List<MailMessage> mails);

        void onError(DatabaseError error);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@StringRes int messageRes);
    }

    public static Handle observe(String uid, MailCallback callback) {
        DatabaseReference ref = FirebaseRefs.mail(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<MailMessage> mails = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MailMessage mail = MailMessage.from(child, child.getKey());
                    if (mail != null) {
                        mails.add(mail);
                    }
                }
                Collections.sort(mails, (left, right) ->
                        Long.compare(right.createdAt, left.createdAt));
                callback.onData(mails);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public void seedWelcome(String uid) {
        DatabaseReference flag = FirebaseRefs.player(uid).child("mailFlags").child("mailSeed_v1");
        flag.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData data) {
                if (data.getValue() != null) {
                    return Transaction.abort();
                }
                data.setValue(true);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed) {
                    return;
                }
                long now = System.currentTimeMillis();
                Map<String, Object> mails = new HashMap<>();
                mails.put(SEED_WELCOME, welcomeMail(now).toMap());
                mails.put(SEED_EVENT, eventMail(now).toMap());
                FirebaseRefs.mail(uid).updateChildren(mails);
            }
        });
    }

    public void markRead(String uid, String mailId) {
        if (uid == null || mailId == null) {
            return;
        }
        FirebaseRefs.mail(uid).child(mailId).child("read").setValue(true);
    }

    public void claim(String uid, MailMessage mail, ActionCallback callback) {
        if (mail == null) {
            callback.onError(R.string.mail_error_missing);
            return;
        }
        if (!mail.hasAttachment()) {
            callback.onError(R.string.mail_error_empty);
            return;
        }
        long now = System.currentTimeMillis();
        if (MailLogic.isExpired(mail.expiresAt, now)) {
            callback.onError(R.string.mail_error_expired);
            return;
        }
        FirebaseRefs.mail(uid).child(mail.id).child("claimed")
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData data) {
                        if (data.getValue() == null) {
                            return Transaction.abort();
                        }
                        if (Boolean.TRUE.equals(data.getValue(Boolean.class))) {
                            return Transaction.success(data);
                        }
                        data.setValue(true);
                        return Transaction.success(data);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed,
                                           DataSnapshot snapshot) {
                        if (!committed) {
                            callback.onError(R.string.mail_error_missing);
                            return;
                        }
                        deliver(uid, mail, callback);
                    }
                });
    }

    public void claimAll(String uid, List<MailMessage> mails, ActionCallback callback) {
        long now = System.currentTimeMillis();
        List<MailMessage> pending = new ArrayList<>();
        for (MailMessage mail : mails) {
            if (mail.canClaim(now)) {
                pending.add(mail);
            }
        }
        if (pending.isEmpty()) {
            callback.onError(R.string.mail_nothing_claim);
            return;
        }
        claimNext(uid, pending, 0, 0, callback);
    }

    private void claimNext(String uid, List<MailMessage> mails, int index, int claimed,
                           ActionCallback callback) {
        if (index >= mails.size()) {
            if (claimed > 0) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.mail_nothing_claim);
            }
            return;
        }
        MailMessage mail = mails.get(index);
        claim(uid, mail, new ActionCallback() {
            @Override
            public void onSuccess() {
                claimNext(uid, mails, index + 1, claimed + 1, callback);
            }

            @Override
            public void onError(@StringRes int messageRes) {
                claimNext(uid, mails, index + 1, claimed, callback);
            }
        });
    }

    public void deliverPending(String uid, List<MailMessage> mails, ActionCallback callback) {
        List<MailMessage> pending = new ArrayList<>();
        for (MailMessage mail : mails) {
            if (mail.claimed && mail.hasAttachment()) {
                pending.add(mail);
            }
        }
        deliverNext(uid, pending, 0, callback);
    }

    private void deliverNext(String uid, List<MailMessage> mails, int index,
                             ActionCallback callback) {
        if (index >= mails.size()) {
            callback.onSuccess();
            return;
        }
        deliver(uid, mails.get(index), new ActionCallback() {
            @Override
            public void onSuccess() {
                deliverNext(uid, mails, index + 1, callback);
            }

            @Override
            public void onError(@StringRes int messageRes) {
                deliverNext(uid, mails, index + 1, callback);
            }
        });
    }

    public void cleanup(String uid, List<MailMessage> mails) {
        if (uid == null || mails == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> deletes = new HashMap<>();
        int kept = 0;
        List<MailMessage> sorted = new ArrayList<>(mails);
        Collections.sort(sorted, (left, right) ->
                Long.compare(right.createdAt, left.createdAt));
        for (MailMessage mail : sorted) {
            if (MailLogic.isExpired(mail.expiresAt, now) || kept >= MailLogic.MAX_MAIL) {
                deletes.put(mail.id, null);
            } else {
                kept++;
            }
        }
        if (!deletes.isEmpty()) {
            FirebaseRefs.mail(uid).updateChildren(deletes);
        }
    }

    private void deliver(String uid, MailMessage mail, ActionCallback callback) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                if (player.child("mailClaims").child(mail.id).getValue() != null) {
                    return Transaction.abort();
                }
                if (mail.coin > 0) {
                    MutableData coin = player.child("currencies").child("coin");
                    coin.setValue(longOf(coin, 0) + mail.coin);
                }
                if (mail.diamond > 0) {
                    MutableData diamond = player.child("currencies").child("diamond");
                    diamond.setValue(longOf(diamond, 0) + mail.diamond);
                }
                for (Map.Entry<String, Integer> entry : mail.items.entrySet()) {
                    int qty = entry.getValue() == null ? 0 : entry.getValue();
                    if (qty <= 0) {
                        continue;
                    }
                    MutableData item = player.child("inventory").child(entry.getKey());
                    int current = intOf(item.child("qty"), 0);
                    item.child("qty").setValue(current + qty);
                }
                player.child("mailClaims").child(mail.id)
                        .setValue(System.currentTimeMillis());
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (committed || (error == null && snapshot != null
                        && snapshot.child("mailClaims").child(mail.id).exists())) {
                    callback.onSuccess();
                } else {
                    callback.onError(R.string.mail_error_deliver);
                }
            }
        });
    }

    private static MailMessage welcomeMail(long now) {
        Map<String, Integer> items = new HashMap<>();
        items.put("expscroll_s", 3);
        return new MailMessage(SEED_WELCOME, "WELCOME",
                "Selamat datang di The Myth",
                "Welcome to The Myth",
                "Pemanggil baru, terima kasih telah datang. Gunakan bekal ini untuk menguatkan "
                        + "formasi pertamamu.",
                "New Summoner, thank you for coming. Use this supply to strengthen your first "
                        + "formation.",
                1_000, 30, items, now, MailLogic.expiresAt(now), false, false);
    }

    private static MailMessage eventMail(long now) {
        Map<String, Integer> items = new HashMap<>();
        items.put("stone_enhance", 2);
        return new MailMessage(SEED_EVENT, "EVENT",
                "Pembaruan Sosial",
                "Social Update",
                "Guild, teman, dan kotak surel kini terbuka. Kumpulkan poin guild minggu ini!",
                "Guilds, friends, and mail are now open. Collect guild points this week!",
                2_000, 0, items, now - 60_000L, MailLogic.expiresAt(now), false, false);
    }

    private static int intOf(MutableData data, int fallback) {
        Object value = data.getValue();
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(MutableData data, long fallback) {
        Object value = data.getValue();
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
