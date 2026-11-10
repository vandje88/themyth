package zhaf.media.themyth.data.repository;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.RosterChar;

public final class RosterRepository {

    public interface RosterCallback {
        void onData(List<RosterChar> roster);

        void onError(DatabaseError error);
    }

    public interface FormationCallback {
        void onData(FormationState formation);

        void onError(DatabaseError error);
    }

    public interface SaveCallback {
        void onSuccess();

        void onError();
    }

    private RosterRepository() {
    }

    public static Handle observeRoster(String uid, RosterCallback callback) {
        DatabaseReference ref = FirebaseRefs.roster(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<RosterChar> roster = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    RosterChar owned = RosterChar.from(child.getKey(), child);
                    if (owned != null) {
                        roster.add(owned);
                    }
                }
                callback.onData(roster);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static Handle observeFormation(String uid, FormationCallback callback) {
        DatabaseReference ref = FirebaseRefs.formation(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(snapshot.exists()
                        ? FormationState.from(snapshot)
                        : FormationState.empty());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static void saveFormation(String uid, List<String> slots, String strategy,
                                     long cp, SaveCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("slots", slots);
        update.put("strategy", strategy);
        update.put("cp", cp);
        update.put("updatedAt", ServerValue.TIMESTAMP);

        FirebaseRefs.formation(uid).updateChildren(update)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError();
                    }
                });
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
