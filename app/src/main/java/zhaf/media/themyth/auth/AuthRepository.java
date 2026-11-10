package zhaf.media.themyth.auth;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;

public final class AuthRepository {

    private static final String TAG = "AuthRepository";
    private static final String STARTER_ID = "gatotkaca";
    private static final String STARTER_MAGE_ID = "nyai_roro_kidul";
    private static final String FLAG_STARTER_V2 = "starterV2";

    public interface Callback {
        void onSuccess(FirebaseUser user);

        void onError(String message);

        default void onAccountConflict() {
        }
    }

    private static final Random RANDOM = new Random();

    private final Context appContext;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private String ensuredUid;

    public AuthRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public void signInAsGuest(Callback callback) {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            ensureProfile(current, callback);
            return;
        }

        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError(messageFor(task.getException(), R.string.login_guest_failed));
                return;
            }
            ensureProfile(auth.getCurrentUser(), callback);
        });
    }

    public void signInWithGoogleToken(String idToken, Callback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseUser current = auth.getCurrentUser();

        if (current == null || !current.isAnonymous()) {
            signInWithCredential(credential, callback);
            return;
        }

        current.linkWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ensureProfile(auth.getCurrentUser(), callback);
                return;
            }
            Exception error = task.getException();
            if (error instanceof FirebaseAuthUserCollisionException) {
                callback.onAccountConflict();
                return;
            }
            callback.onError(messageFor(error, R.string.login_google_failed));
        });
    }

    public void signInWithGoogleTokenForce(String idToken, Callback callback) {
        signInWithCredential(GoogleAuthProvider.getCredential(idToken, null), callback);
    }

    public void ensureProfile(FirebaseUser user, Callback callback) {
        if (user == null) {
            callback.onError(appContext.getString(R.string.error_session));
            return;
        }
        if (user.getUid().equals(ensuredUid)) {
            callback.onSuccess(user);
            return;
        }

        DatabaseReference playerRef = FirebaseRefs.player(user.getUid());
        playerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    grantStarterHeroes(user, snapshot, playerRef, callback);
                    return;
                }
                createProfile(user, playerRef, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(UiErrors.database(appContext, error));
            }
        });
    }

    private void grantStarterHeroes(FirebaseUser user, DataSnapshot player,
                                    DatabaseReference playerRef, Callback callback) {
        Boolean migrated = player.child("flags").child(FLAG_STARTER_V2)
                .getValue(Boolean.class);
        if (Boolean.TRUE.equals(migrated)) {
            ensuredUid = user.getUid();
            callback.onSuccess(user);
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();
        if (!player.child("roster").child(STARTER_ID).exists()) {
            update.put("roster/" + STARTER_ID, starterEntry(STARTER_ID, now));
        }
        if (!player.child("roster").child(STARTER_MAGE_ID).exists()) {
            update.put("roster/" + STARTER_MAGE_ID, starterEntry(STARTER_MAGE_ID, now));
        }

        CharacterCatalog catalog = CharacterCatalog.get(appContext);
        DataSnapshot slots = player.child("formation").child("slots");
        boolean formationHasKnown = false;
        if (slots.exists()) {
            for (DataSnapshot slot : slots.getChildren()) {
                String charId = slot.getValue(String.class);
                if (charId != null && catalog.find(charId) != null) {
                    formationHasKnown = true;
                    break;
                }
            }
        }
        if (!formationHasKnown) {
            update.put("formation/slots", Arrays.asList(STARTER_ID, "", STARTER_MAGE_ID));
            update.put("formation/strategy", "BALANCED");
            update.put("formation/cp", 0);
            update.put("formation/updatedAt", now);
        }

        String favorite = player.child("profile").child("favoriteCharId").getValue(String.class);
        if (favorite != null && catalog.find(favorite) == null) {
            update.put("profile/favoriteCharId", STARTER_ID);
        }

        update.put("flags/" + FLAG_STARTER_V2, true);

        playerRef.updateChildren(update).addOnCompleteListener(task -> {
            ensuredUid = user.getUid();
            if (!task.isSuccessful()) {
                Log.w(TAG, "Starter hero migration failed", task.getException());
            }
            callback.onSuccess(user);
        });
    }

    private Map<String, Object> starterEntry(String charId, long now) {
        Map<String, Object> skillLevels = new HashMap<>();
        skillLevels.put("basic", 1);
        skillLevels.put("skill1", 1);
        skillLevels.put("skill2", 1);
        skillLevels.put("ultimate", 1);

        Map<String, Object> entry = new HashMap<>();
        entry.put("charId", charId);
        entry.put("level", 1);
        entry.put("exp", 0);
        entry.put("stars", 1);
        entry.put("awakening", 0);
        entry.put("breakthrough", 0);
        entry.put("skillLevels", skillLevels);
        entry.put("locked", false);
        entry.put("obtainedAt", now);
        return entry;
    }

    private void createProfile(FirebaseUser user, DatabaseReference playerRef, Callback callback) {
        playerRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() != null) {
                    return Transaction.success(currentData);
                }
                currentData.setValue(buildNewPlayer(user));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    callback.onError(UiErrors.database(appContext, error));
                    return;
                }
                if (!committed && !snapshot.exists()) {
                    callback.onError(appContext.getString(R.string.error_profile));
                    return;
                }
                ensuredUid = user.getUid();
                callback.onSuccess(user);
            }
        });
    }

    public void signOut() {
        ensuredUid = null;
        auth.signOut();
    }

    private void signInWithCredential(AuthCredential credential, Callback callback) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError(messageFor(task.getException(), R.string.login_google_failed));
                return;
            }
            ensuredUid = null;
            ensureProfile(auth.getCurrentUser(), callback);
        });
    }

    private String messageFor(Exception error, int fallbackRes) {
        if (error == null) {
            return appContext.getString(fallbackRes);
        }
        return UiErrors.auth(appContext, error);
    }

    private Map<String, Object> buildNewPlayer(FirebaseUser user) {
        long now = System.currentTimeMillis();
        String username = resolveUsername(user);

        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("username", username);
        profile.put("usernameLower", username.toLowerCase(Locale.ROOT));
        profile.put("avatarId", "av_default_1");
        profile.put("frameId", "frame_bronze");
        profile.put("level", 1);
        profile.put("exp", 0);
        profile.put("rank", "bronze");
        profile.put("rating", 1000);
        profile.put("cpBest", 0);
        profile.put("cpCurrent", 0);
        profile.put("totalBattles", 0);
        profile.put("wins", 0);
        profile.put("losses", 0);
        profile.put("highestRank", "bronze");
        profile.put("favoriteCharId", STARTER_ID);
        profile.put("titleId", "rookie_myth");
        profile.put("createdAt", now);
        profile.put("lastOnline", now);

        Map<String, Object> settings = new HashMap<>();
        settings.put("language", Locale.getDefault().getLanguage().startsWith("en") ? "en" : "id");
        settings.put("bgm", 70);
        settings.put("sfx", 80);
        settings.put("battleSpeed", 2);
        settings.put("notifications", true);
        settings.put("strategy", "BALANCED");

        Map<String, Object> currencies = new HashMap<>();
        currencies.put("coin", 1000);
        currencies.put("diamond", 300);
        currencies.put("energy", 120);
        currencies.put("ticketSummon", 1);
        currencies.put("ticketPvp", 5);
        currencies.put("ticketDungeon", 2);
        currencies.put("arenaPoint", 0);
        currencies.put("guildCoin", 0);
        currencies.put("eventCoin", 0);

        Map<String, Object> roster = new HashMap<>();
        roster.put(STARTER_ID, starterEntry(STARTER_ID, now));
        roster.put(STARTER_MAGE_ID, starterEntry(STARTER_MAGE_ID, now));

        Map<String, Object> formation = new HashMap<>();
        formation.put("slots", Arrays.asList(STARTER_ID, "", STARTER_MAGE_ID));
        formation.put("strategy", "BALANCED");
        formation.put("updatedAt", now);
        formation.put("cp", 0);

        Map<String, Object> progress = new HashMap<>();
        progress.put("chapter", 1);
        progress.put("stage", 1);
        progress.put("highestStage", "1_1");
        progress.put("towerFloor", 0);

        Map<String, Object> idle = new HashMap<>();
        idle.put("lastClaimAt", now);
        idle.put("stageIndex", 1);

        Map<String, Object> flags = new HashMap<>();
        flags.put("tutorialDone", false);
        flags.put("starterClaimed", false);
        flags.put(FLAG_STARTER_V2, true);
        flags.put("banned", false);

        Map<String, Object> player = new HashMap<>();
        player.put("profile", profile);
        player.put("settings", settings);
        player.put("currencies", currencies);
        player.put("energyUpdatedAt", now);
        player.put("roster", roster);
        player.put("formation", formation);
        player.put("progress", progress);
        player.put("idle", idle);
        player.put("flags", flags);
        return player;
    }

    private String resolveUsername(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (displayName != null) {
            String cleaned = displayName.trim().replaceAll("\\s+", " ");
            if (cleaned.length() > 16) {
                cleaned = cleaned.substring(0, 16).trim();
            }
            if (cleaned.length() >= 3) {
                return cleaned;
            }
        }
        return "Pemanggil" + (1000 + RANDOM.nextInt(9000));
    }
}
