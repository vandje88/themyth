package zhaf.media.themyth.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.arena.ArenaRating;
import zhaf.media.themyth.data.arena.ArenaState;
import zhaf.media.themyth.data.meta.BadgeCatalog;
import zhaf.media.themyth.data.meta.BadgeDef;
import zhaf.media.themyth.data.meta.PlayerMeta;
import zhaf.media.themyth.data.meta.PlayerStats;
import zhaf.media.themyth.data.meta.TitleCatalog;
import zhaf.media.themyth.data.meta.TitleDef;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.PlayerProfile;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.repository.ArenaRepository;
import zhaf.media.themyth.data.repository.MetaRepository;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.databinding.ActivityProfileBinding;
import zhaf.media.themyth.ui.login.LoginActivity;
import zhaf.media.themyth.ui.ranking.RankingActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 12_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ActivityProfileBinding binding;
    private CharacterCatalog catalog;
    private String uid;

    private DatabaseReference profileRef;
    private ValueEventListener profileListener;
    private DatabaseReference formationRef;
    private ValueEventListener formationListener;
    private DatabaseReference guildRef;
    private ValueEventListener guildListener;
    private ArenaRepository.Handle arenaHandle;
    private MetaRepository.Handle metaHandle;
    private ProgressionRepository.Handle playerHandle;

    private PlayerProfile profile;
    private ArenaState arena = ArenaState.defaults(0);
    private PlayerMeta meta = PlayerMeta.from(null);
    private FormationState formation = FormationState.empty();
    private PlayerState player;
    private String guildId;

    private boolean profileLoaded;

    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (profileLoaded || isFinishing() || isDestroyed()) {
                return;
            }
            showLoading(false);
            binding.profileErrorBox.setVisibility(View.VISIBLE);
            binding.tvProfileError.setText(R.string.profile_error_load);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = new AuthRepository(this).currentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        catalog = CharacterCatalog.get(this);

        binding.btnProfileBack.setOnClickListener(v -> finish());
        binding.btnProfileRetry.setOnClickListener(v -> retryLoad());
        binding.btnProfileTitle.setOnClickListener(v -> pickTitle());
        binding.btnProfileBadge.setOnClickListener(v -> pickBadge());
        binding.btnProfileRanking.setOnClickListener(v ->
                startActivity(new Intent(this, RankingActivity.class)));
        binding.tvProfileUid.setOnClickListener(v -> copyUid());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) {
            return;
        }
        attach();
    }

    @Override
    protected void onStop() {
        detach();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(loadTimeout);
        super.onDestroy();
    }

    private void attach() {
        showLoading(!profileLoaded);
        binding.profileErrorBox.setVisibility(View.GONE);
        if (!profileLoaded) {
            handler.removeCallbacks(loadTimeout);
            handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);
        }

        profileRef = FirebaseRefs.profile(uid);
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showLoadError(getString(R.string.profile_error_load));
                    return;
                }
                handler.removeCallbacks(loadTimeout);
                profile = PlayerProfile.from(snapshot, uid);
                profileLoaded = true;
                showLoading(false);
                binding.profileErrorBox.setVisibility(View.GONE);
                render();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showLoadError(UiErrors.database(ProfileActivity.this, error));
            }
        };
        profileRef.addValueEventListener(profileListener);

        formationRef = FirebaseRefs.formation(uid);
        formationListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                formation = FormationState.from(snapshot);
                render();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                render();
            }
        };
        formationRef.addValueEventListener(formationListener);

        guildRef = FirebaseRefs.guildOf(uid).child("id");
        guildListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                guildId = snapshot.getValue(String.class);
                render();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                guildId = null;
                render();
            }
        };
        guildRef.addValueEventListener(guildListener);

        arenaHandle = ArenaRepository.observeState(uid, new ArenaRepository.StateCallback() {
            @Override
            public void onData(ArenaState state) {
                arena = state;
                render();
            }

            @Override
            public void onError(DatabaseError error) {
                render();
            }
        });

        metaHandle = MetaRepository.observe(uid, new MetaRepository.MetaCallback() {
            @Override
            public void onData(PlayerMeta data) {
                meta = data;
                render();
            }

            @Override
            public void onError(DatabaseError error) {
                render();
            }
        });

        playerHandle = ProgressionRepository.observe(uid,
                new ProgressionRepository.StateCallback() {
                    @Override
                    public void onData(PlayerState state) {
                        player = state;
                        render();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        render();
                    }
                });
    }

    private void detach() {
        if (profileRef != null && profileListener != null) {
            profileRef.removeEventListener(profileListener);
        }
        if (formationRef != null && formationListener != null) {
            formationRef.removeEventListener(formationListener);
        }
        if (guildRef != null && guildListener != null) {
            guildRef.removeEventListener(guildListener);
        }
        profileRef = null;
        formationRef = null;
        guildRef = null;
        profileListener = null;
        formationListener = null;
        guildListener = null;
        if (arenaHandle != null) {
            arenaHandle.detach();
            arenaHandle = null;
        }
        if (metaHandle != null) {
            metaHandle.detach();
            metaHandle = null;
        }
        if (playerHandle != null) {
            playerHandle.detach();
            playerHandle = null;
        }
        handler.removeCallbacks(loadTimeout);
    }

    private void render() {
        if (profile == null) {
            return;
        }
        binding.profileContent.setVisibility(View.VISIBLE);
        binding.tvProfileAvatar.setText(Texts.initial(profile.username));
        binding.tvProfileName.setText(profile.username);
        binding.tvProfileTitle.setText(getString(R.string.profile_label_value,
                getString(R.string.profile_title_label),
                TitleCatalog.nameOf(profile.titleId)));
        binding.tvProfileBadge.setText(getString(R.string.profile_label_value,
                getString(R.string.profile_badge_label),
                BadgeCatalog.nameOf(profile.badgeId)));
        binding.tvProfileUid.setText(getString(R.string.profile_uid, shortUid(uid)));

        PlayerStats stats = stats();
        binding.tvProfileLevel.setText(String.valueOf(profile.level));
        binding.tvProfileRating.setText(getString(R.string.profile_rating_value,
                Texts.pretty(ArenaRating.tier(arena.rating)), arena.rating));
        binding.tvProfileCp.setText(Texts.number(formation.cp));
        binding.tvProfileRecord.setText(getString(R.string.profile_record_value,
                arena.wins, arena.losses));
        binding.tvProfileWinRate.setText(getString(R.string.profile_win_rate_value,
                stats.winRatePercent()));
        binding.tvProfileGuild.setText(guildId == null || guildId.isEmpty()
                ? getString(R.string.profile_guild_none)
                : getString(R.string.profile_guild_id, shortGuild(guildId)));
        binding.tvProfileFavorite.setText(favoriteName());
        binding.tvProfileLastOnline.setText(lastOnline());
    }

    private PlayerStats stats() {
        int rosterSize = 0;
        int mythicCount = 0;
        if (player != null) {
            rosterSize = player.roster.size();
            for (RosterChar owned : player.roster.values()) {
                CharacterTemplate template = catalog.find(owned.charId);
                if (template != null && "mythic".equals(template.rarity)) {
                    mythicCount++;
                }
            }
        }
        return new PlayerStats(profile.level, arena.rating, arena.wins, arena.losses,
                rosterSize, mythicCount, meta.bossAttacks,
                guildId != null && !guildId.isEmpty());
    }

    private String favoriteName() {
        for (String charId : formation.slots) {
            if (charId == null || charId.trim().isEmpty()) {
                continue;
            }
            CharacterTemplate template = catalog.find(charId);
            return template == null ? Texts.pretty(charId) : template.name;
        }
        return getString(R.string.profile_favorite_none);
    }

    private String lastOnline() {
        long at = meta.lastSeen > 0 ? meta.lastSeen : profile.lastOnline;
        if (at <= 0) {
            return getString(R.string.profile_never);
        }
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        return format.format(new Date(at));
    }

    private void pickTitle() {
        if (profile == null) {
            return;
        }
        PlayerStats stats = stats();
        List<TitleDef> defs = TitleCatalog.all();
        String[] items = new String[defs.size()];
        for (int i = 0; i < defs.size(); i++) {
            TitleDef def = defs.get(i);
            boolean unlocked = TitleCatalog.unlocked(def.id, stats);
            String marker = def.id.equals(profile.titleId) ? "✓ " : "";
            items[i] = marker + def.name()
                    + (unlocked ? " · " + def.description()
                    : " · " + getString(R.string.profile_locked));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_pick_title)
                .setItems(items, (dialog, which) -> {
                    TitleDef def = defs.get(which);
                    if (!TitleCatalog.unlocked(def.id, stats)) {
                        Snackbar.make(binding.getRoot(), R.string.profile_locked_pick,
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    MetaRepository.setTitle(uid, def.id);
                    Snackbar.make(binding.getRoot(), R.string.profile_title_saved,
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickBadge() {
        if (profile == null) {
            return;
        }
        PlayerStats stats = stats();
        List<BadgeDef> defs = BadgeCatalog.all();
        String[] items = new String[defs.size() + 1];
        items[0] = (BadgeCatalog.NONE.equals(profile.badgeId) ? "✓ " : "")
                + getString(R.string.profile_no_badge);
        for (int i = 0; i < defs.size(); i++) {
            BadgeDef def = defs.get(i);
            boolean unlocked = BadgeCatalog.unlocked(def.id, stats);
            String marker = def.id.equals(profile.badgeId) ? "✓ " : "";
            items[i + 1] = marker + def.name()
                    + (unlocked ? " · " + def.description()
                    : " · " + getString(R.string.profile_locked));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_pick_badge)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        MetaRepository.setBadge(uid, BadgeCatalog.NONE);
                        Snackbar.make(binding.getRoot(), R.string.profile_badge_saved,
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    BadgeDef def = defs.get(which - 1);
                    if (!BadgeCatalog.unlocked(def.id, stats)) {
                        Snackbar.make(binding.getRoot(), R.string.profile_locked_pick,
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    MetaRepository.setBadge(uid, def.id);
                    Snackbar.make(binding.getRoot(), R.string.profile_badge_saved,
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void retryLoad() {
        detach();
        profileLoaded = false;
        binding.profileErrorBox.setVisibility(View.GONE);
        attach();
    }

    private void copyUid() {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("uid", uid));
        Snackbar.make(binding.getRoot(), R.string.home_uid_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void showLoading(boolean visible) {
        binding.progressProfile.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            binding.profileContent.setVisibility(View.GONE);
        }
    }

    private void showLoadError(String message) {
        handler.removeCallbacks(loadTimeout);
        showLoading(false);
        binding.tvProfileError.setText(message);
        binding.profileErrorBox.setVisibility(View.VISIBLE);
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

    private static String shortGuild(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        if (value.length() <= 12) {
            return value;
        }
        return value.substring(0, 8) + "…";
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
