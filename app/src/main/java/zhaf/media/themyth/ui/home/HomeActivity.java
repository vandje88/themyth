package zhaf.media.themyth.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import zhaf.media.themyth.BuildConfig;
import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.Currencies;
import zhaf.media.themyth.data.model.PlayerProfile;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.databinding.ActivityHomeBinding;
import zhaf.media.themyth.ui.battle.BattleActivity;
import zhaf.media.themyth.ui.formation.FormationActivity;
import zhaf.media.themyth.ui.inventory.InventoryActivity;
import zhaf.media.themyth.ui.login.LoginActivity;
import zhaf.media.themyth.ui.roster.RosterActivity;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private AuthRepository authRepository;
    private String uid;

    private DatabaseReference profileRef;
    private DatabaseReference currenciesRef;
    private ValueEventListener profileListener;
    private ValueEventListener currenciesListener;

    private boolean listenersAttached;
    private boolean profileLoaded;
    private boolean formationLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        FirebaseUser user = authRepository.currentUser();
        if (user == null) {
            goLogin();
            return;
        }

        uid = user.getUid();
        binding.tvAccountBadge.setText(user.isAnonymous()
                ? R.string.home_guest_badge
                : R.string.home_google_badge);
        binding.tvFooterVersion.setText(
                getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
        binding.tvUid.setText(getString(R.string.home_uid, shortUid(uid)));
        binding.tvUid.setContentDescription(getString(R.string.home_copy_uid));
        binding.tvUid.setOnClickListener(v -> copyUid());

        binding.tvAvatar.setText(initial(user.getDisplayName()));
        if (user.getDisplayName() != null) {
            binding.tvUsername.setText(user.getDisplayName());
        }

        binding.btnRetry.setOnClickListener(v -> retryLoad());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        View.OnClickListener comingSoon = v ->
                Snackbar.make(binding.getRoot(), R.string.home_coming_soon,
                        Snackbar.LENGTH_SHORT).show();
        binding.btnPlay.setOnClickListener(
                v -> startActivity(new Intent(this, BattleActivity.class)));
        binding.btnArena.setOnClickListener(comingSoon);
        binding.btnRoster.setOnClickListener(
                v -> startActivity(new Intent(this, RosterActivity.class)));
        binding.btnFormation.setOnClickListener(
                v -> startActivity(new Intent(this, FormationActivity.class)));
        binding.btnInventory.setOnClickListener(
                v -> startActivity(new Intent(this, InventoryActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid != null) {
            attachListeners();
        }
    }

    @Override
    protected void onStop() {
        detachListeners();
        super.onStop();
    }

    private void attachListeners() {
        if (listenersAttached || uid == null) {
            return;
        }
        listenersAttached = true;

        if (!profileLoaded) {
            binding.progressHome.setVisibility(View.VISIBLE);
            binding.tvHomeError.setVisibility(View.GONE);
            binding.btnRetry.setVisibility(View.GONE);
        }

        profileRef = FirebaseRefs.profile(uid);
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showError(getString(R.string.error_profile));
                    return;
                }
                PlayerProfile profile = PlayerProfile.from(snapshot, uid);
                binding.tvUsername.setText(profile.username);
                binding.tvWelcomeName.setText(profile.username);
                binding.tvAvatar.setText(initial(profile.username));
                binding.tvLevel.setText(getString(
                        R.string.home_level,
                        profile.level,
                        Texts.pretty(profile.rank)
                ));
                hideLoading();
                if (!formationLoaded) {
                    formationLoaded = true;
                    loadFormation();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError(UiErrors.database(HomeActivity.this, error));
            }
        };
        profileRef.addValueEventListener(profileListener);

        currenciesRef = FirebaseRefs.currencies(uid);
        currenciesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Currencies currencies = Currencies.from(snapshot);
                binding.tvCoin.setText(formatNumber(currencies.coin));
                binding.tvDiamond.setText(formatNumber(currencies.diamond));
                binding.tvEnergy.setText(formatNumber(currencies.energy));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError(UiErrors.database(HomeActivity.this, error));
            }
        };
        currenciesRef.addValueEventListener(currenciesListener);
    }

    private void detachListeners() {
        if (!listenersAttached) {
            return;
        }
        if (profileRef != null && profileListener != null) {
            profileRef.removeEventListener(profileListener);
        }
        if (currenciesRef != null && currenciesListener != null) {
            currenciesRef.removeEventListener(currenciesListener);
        }
        profileRef = null;
        currenciesRef = null;
        profileListener = null;
        currenciesListener = null;
        listenersAttached = false;
    }

    private void loadFormation() {
        FirebaseRefs.formation(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> names = new ArrayList<>();
                CharacterCatalog catalog = CharacterCatalog.get(HomeActivity.this);
                for (DataSnapshot child : snapshot.child("slots").getChildren()) {
                    String charId = child.getValue(String.class);
                    if (charId != null && !charId.trim().isEmpty()) {
                        CharacterTemplate template = catalog.find(charId);
                        names.add(template == null ? Texts.pretty(charId) : template.name);
                    }
                }
                binding.tvFormation.setText(names.isEmpty()
                        ? getString(R.string.home_formation_empty)
                        : TextUtils.join(" · ", names));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                binding.tvFormation.setText(R.string.home_formation_empty);
            }
        });
    }

    private void retryLoad() {
        detachListeners();
        profileLoaded = false;
        formationLoaded = false;
        attachListeners();
    }

    private void hideLoading() {
        profileLoaded = true;
        binding.progressHome.setVisibility(View.GONE);
        binding.tvHomeError.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.progressHome.setVisibility(View.GONE);
        binding.tvHomeError.setText(message);
        binding.tvHomeError.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(View.VISIBLE);
    }

    private void copyUid() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("uid", uid));
        Snackbar.make(binding.getRoot(), R.string.home_uid_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.home_logout_title)
                .setMessage(R.string.home_logout_message)
                .setNegativeButton(R.string.home_logout_cancel, null)
                .setPositiveButton(R.string.home_logout_confirm,
                        (dialog, which) -> doLogout())
                .show();
    }

    private void doLogout() {
        detachListeners();
        authRepository.signOut();
        try {
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        } catch (RuntimeException ignored) {
            // No Play Services session to clear.
        }
        goLogin();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String shortUid(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        if (value.length() <= 10) {
            return value;
        }
        return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
    }

    private static String initial(String name) {
        if (name == null) {
            return "P";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "P";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance().format(value);
    }
}
