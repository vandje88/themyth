package zhaf.media.themyth.core;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseRefs {

    private FirebaseRefs() {
    }

    public static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseReference player(String uid) {
        return root().child("players").child(uid);
    }

    public static DatabaseReference profile(String uid) {
        return player(uid).child("profile");
    }

    public static DatabaseReference currencies(String uid) {
        return player(uid).child("currencies");
    }

    public static DatabaseReference roster(String uid) {
        return player(uid).child("roster");
    }

    public static DatabaseReference formation(String uid) {
        return player(uid).child("formation");
    }
}
