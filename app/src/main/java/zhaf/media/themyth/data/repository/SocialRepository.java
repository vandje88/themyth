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
import zhaf.media.themyth.data.social.DirectoryEntry;
import zhaf.media.themyth.data.social.FriendEntry;
import zhaf.media.themyth.data.social.FriendLogic;
import zhaf.media.themyth.data.social.FriendRequest;
import zhaf.media.themyth.data.social.GiftEntry;
import zhaf.media.themyth.data.social.SocialDay;

public final class SocialRepository {

    public interface StateCallback {
        void onData(State state);

        void onError(DatabaseError error);
    }

    public interface DirectoryCallback {
        void onData(DirectoryEntry entry);

        void onError(@StringRes int messageRes);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@StringRes int messageRes);
    }

    public static Handle observe(String uid, StateCallback callback) {
        DatabaseReference ref = FirebaseRefs.friends(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(State.from(snapshot));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void publishDirectory(String uid, String name, int level, String guildId) {
        if (uid == null) {
            return;
        }
        String safeName = name == null || name.trim().isEmpty()
                ? "Pemanggil" : name.trim();
        if (safeName.length() > 16) {
            safeName = safeName.substring(0, 16);
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("uid", uid);
        entry.put("name", safeName);
        entry.put("avatar", safeName.substring(0, 1).toUpperCase());
        entry.put("level", Math.max(1, Math.min(100, level)));
        entry.put("guildId", guildId);
        entry.put("updatedAt", System.currentTimeMillis());
        FirebaseRefs.directoryEntry(uid).updateChildren(entry);
    }

    public void search(String uid, DirectoryCallback callback) {
        if (!FriendLogic.validUid(uid)) {
            callback.onError(R.string.social_error_uid);
            return;
        }
        FirebaseRefs.directoryEntry(uid.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        DirectoryEntry entry = DirectoryEntry.from(snapshot, uid.trim());
                        if (entry == null) {
                            callback.onError(R.string.social_error_missing);
                        } else {
                            callback.onData(entry);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(R.string.social_error_network);
                    }
                });
    }

    public void sendRequest(String myUid, String myName, DirectoryEntry target,
                            ActionCallback callback) {
        long now = System.currentTimeMillis();
        Map<String, Object> request = new HashMap<>();
        request.put("uid", myUid);
        request.put("name", safeName(myName));
        request.put("at", now);

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("uid", target.uid);
        outgoing.put("name", target.name);
        outgoing.put("at", now);

        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + target.uid + "/requests/" + myUid, request);
        updates.put("friends/" + myUid + "/outgoing/" + target.uid, outgoing);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    public void acceptRequest(String myUid, String myName, FriendRequest request,
                              ActionCallback callback) {
        long now = System.currentTimeMillis();
        Map<String, Object> mine = new HashMap<>();
        mine.put("uid", request.uid);
        mine.put("name", request.name);
        mine.put("since", now);

        Map<String, Object> theirs = new HashMap<>();
        theirs.put("uid", myUid);
        theirs.put("name", safeName(myName));
        theirs.put("since", now);

        Map<String, Object> lists = new HashMap<>();
        lists.put("friends/" + myUid + "/list/" + request.uid, mine);
        lists.put("friends/" + request.uid + "/list/" + myUid, theirs);
        FirebaseRefs.root().updateChildren(lists, (error, ref) -> {
            if (error != null) {
                callback.onError(R.string.social_error_action);
                return;
            }
            Map<String, Object> clean = new HashMap<>();
            clean.put("friends/" + myUid + "/requests/" + request.uid, null);
            clean.put("friends/" + request.uid + "/outgoing/" + myUid, null);
            FirebaseRefs.root().updateChildren(clean, (cleanError, cleanRef) -> {
                if (cleanError == null) {
                    callback.onSuccess();
                } else {
                    callback.onError(R.string.social_error_action);
                }
            });
        });
    }

    public void declineRequest(String myUid, FriendRequest request, ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + myUid + "/requests/" + request.uid, null);
        updates.put("friends/" + request.uid + "/outgoing/" + myUid, null);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    public void removeFriend(String myUid, String friendUid, ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + myUid + "/list/" + friendUid, null);
        updates.put("friends/" + friendUid + "/list/" + myUid, null);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    public void sendGift(String myUid, String myName, FriendEntry friend,
                         ActionCallback callback) {
        long now = System.currentTimeMillis();
        String day = SocialDay.dayKey(now);
        if (!FriendLogic.giftReady(friend.lastGiftDay, now)) {
            callback.onError(R.string.social_gift_done);
            return;
        }
        Map<String, Object> gift = new HashMap<>();
        gift.put("uid", myUid);
        gift.put("name", safeName(myName));
        gift.put("day", day);
        gift.put("at", now);

        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + friend.uid + "/gifts/" + myUid, gift);
        updates.put("friends/" + myUid + "/list/" + friend.uid + "/lastGiftDay", day);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    public void claimGifts(String uid, List<GiftEntry> gifts, ActionCallback callback) {
        if (gifts == null || gifts.isEmpty()) {
            callback.onSuccess();
            return;
        }
        claimGiftNext(uid, gifts, 0, callback);
    }

    private void claimGiftNext(String uid, List<GiftEntry> gifts, int index,
                               ActionCallback callback) {
        if (index >= gifts.size()) {
            callback.onSuccess();
            return;
        }
        GiftEntry gift = gifts.get(index);
        String key = gift.key();
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData marker = player.child("social").child("giftClaims").child(key);
                if (marker.getValue() != null) {
                    return Transaction.abort();
                }
                MutableData coin = player.child("currencies").child("coin");
                long balance = coin.getValue() instanceof Number
                        ? ((Number) coin.getValue()).longValue() : 0;
                coin.setValue(balance + FriendLogic.GIFT_COIN);
                marker.setValue(System.currentTimeMillis());
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                FirebaseRefs.friends(uid).child("gifts").child(gift.fromUid).removeValue();
                claimGiftNext(uid, gifts, index + 1, callback);
            }
        });
    }

    private static String safeName(String name) {
        String value = name == null || name.trim().isEmpty() ? "Pemanggil" : name.trim();
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    public static final class State {
        public final List<FriendEntry> friends;
        public final List<FriendRequest> requests;
        public final List<FriendRequest> outgoing;
        public final List<GiftEntry> gifts;

        State(List<FriendEntry> friends, List<FriendRequest> requests,
              List<FriendRequest> outgoing, List<GiftEntry> gifts) {
            this.friends = Collections.unmodifiableList(friends);
            this.requests = Collections.unmodifiableList(requests);
            this.outgoing = Collections.unmodifiableList(outgoing);
            this.gifts = Collections.unmodifiableList(gifts);
        }

        public static State from(DataSnapshot snapshot) {
            List<FriendEntry> friends = new ArrayList<>();
            for (DataSnapshot child : snapshot.child("list").getChildren()) {
                FriendEntry entry = FriendEntry.from(child, child.getKey());
                if (entry != null) {
                    friends.add(entry);
                }
            }
            List<FriendRequest> requests = new ArrayList<>();
            for (DataSnapshot child : snapshot.child("requests").getChildren()) {
                FriendRequest entry = FriendRequest.from(child, child.getKey());
                if (entry != null) {
                    requests.add(entry);
                }
            }
            List<FriendRequest> outgoing = new ArrayList<>();
            for (DataSnapshot child : snapshot.child("outgoing").getChildren()) {
                FriendRequest entry = FriendRequest.from(child, child.getKey());
                if (entry != null) {
                    outgoing.add(entry);
                }
            }
            List<GiftEntry> gifts = new ArrayList<>();
            for (DataSnapshot child : snapshot.child("gifts").getChildren()) {
                GiftEntry entry = GiftEntry.from(child, child.getKey());
                if (entry != null) {
                    gifts.add(entry);
                }
            }
            return new State(friends, requests, outgoing, gifts);
        }

        public boolean isFriend(String uid) {
            for (FriendEntry entry : friends) {
                if (entry.uid.equals(uid)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasOutgoing(String uid) {
            for (FriendRequest entry : outgoing) {
                if (entry.uid.equals(uid)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasIncoming(String uid) {
            for (FriendRequest entry : requests) {
                if (entry.uid.equals(uid)) {
                    return true;
                }
            }
            return false;
        }

        public FriendEntry friendOf(String uid) {
            for (FriendEntry entry : friends) {
                if (entry.uid.equals(uid)) {
                    return entry;
                }
            }
            return null;
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
}
