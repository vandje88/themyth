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

    public static DatabaseReference meta(String uid) {
        return player(uid).child("meta");
    }

    public static DatabaseReference metaClaims(String uid) {
        return meta(uid).child("claims");
    }

    public static DatabaseReference roster(String uid) {
        return player(uid).child("roster");
    }

    public static DatabaseReference formation(String uid) {
        return player(uid).child("formation");
    }

    public static DatabaseReference arenaState(String uid) {
        return player(uid).child("arena");
    }

    public static DatabaseReference arenaInbox(String uid) {
        return player(uid).child("arenaInbox");
    }

    public static DatabaseReference arenaSnapshots() {
        return root().child("arena").child("snapshots");
    }

    public static DatabaseReference arenaSnapshot(String uid) {
        return arenaSnapshots().child(uid);
    }

    public static DatabaseReference battles() {
        return root().child("battles");
    }

    public static DatabaseReference battle(String bid) {
        return battles().child(bid);
    }

    public static DatabaseReference rankings(String season, String mode) {
        return root().child("rankings").child(season).child(mode);
    }

    public static DatabaseReference ranking(String season, String mode, String uid) {
        return rankings(season, mode).child(uid);
    }

    public static DatabaseReference mail(String uid) {
        return root().child("mail").child(uid);
    }

    public static DatabaseReference friends(String uid) {
        return root().child("friends").child(uid);
    }

    public static DatabaseReference directory() {
        return root().child("directory");
    }

    public static DatabaseReference directoryEntry(String uid) {
        return directory().child(uid);
    }

    public static DatabaseReference guilds() {
        return root().child("guilds");
    }

    public static DatabaseReference guild(String gid) {
        return guilds().child(gid);
    }

    public static DatabaseReference guildIndex() {
        return root().child("guildIndex");
    }

    public static DatabaseReference guildDirectory() {
        return root().child("guildDirectory");
    }

    public static DatabaseReference guildDirectoryEntry(String gid) {
        return guildDirectory().child(gid);
    }

    public static DatabaseReference guildOf(String uid) {
        return player(uid).child("guild");
    }
}
