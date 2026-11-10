package zhaf.media.themyth.ui.arena;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.arena.ArenaBattle;
import zhaf.media.themyth.data.arena.ArenaMatchmaker;
import zhaf.media.themyth.data.arena.ArenaRating;
import zhaf.media.themyth.data.arena.ArenaSnapshot;
import zhaf.media.themyth.data.arena.ArenaSnapshotFactory;
import zhaf.media.themyth.data.arena.ArenaState;
import zhaf.media.themyth.data.arena.ArenaTickets;
import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.PlayerProfile;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.repository.ArenaRepository;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.repository.RosterRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.databinding.ActivityArenaBinding;
import zhaf.media.themyth.ui.battle.BattleActivity;
import zhaf.media.themyth.ui.formation.FormationActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class ArenaActivity extends AppCompatActivity {

    private static final long SEARCH_TICK_MS = 1200L;
    private static final long SEARCH_MAX_MS = 60_000L;
    private static final long REGEN_TICK_MS = 1000L;
    private static final long NETWORK_TIMEOUT_MS = 15_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<RosterChar> roster = new ArrayList<>();
    private final Map<String, EquipmentInstance> equipment = new HashMap<>();
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();

    private ActivityArenaBinding binding;
    private CharacterCatalog catalog;
    private EquipmentCatalog equipCatalog;
    private ProgressionRepository progressionRepository;

    private String uid;
    private String username = "Pemanggil";
    private String title;
    private String season;
    private ArenaState state = ArenaState.defaults(0);
    private FormationState formation = FormationState.empty();
    private ArenaSnapshot snapshot;
    private ArenaSnapshot opponent;
    private boolean opponentIsBot;

    private ArenaRepository.Handle stateHandle;
    private ProgressionRepository.Handle playerHandle;
    private RosterRepository.Handle formationHandle;
    private DatabaseReference profileRef;
    private ValueEventListener profileListener;

    private boolean playerLoaded;
    private boolean formationLoaded;
    private boolean stateLoaded;
    private boolean profileLoaded;
    private boolean searching;
    private boolean searchInFlight;
    private boolean busy;
    private long searchStart;

    private final Runnable searchTick = this::searchTick;
    private final Runnable busyTimeout = new Runnable() {
        @Override
        public void run() {
            if (!busy || isFinishing() || isDestroyed()) {
                return;
            }
            setBusy(false);
            showMessage(R.string.arena_battle_failed);
        }
    };
    private final Runnable regenTick = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            renderTickets();
            handler.postDelayed(this, REGEN_TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArenaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        catalog = CharacterCatalog.get(this);
        equipCatalog = EquipmentCatalog.get(this);
        progressionRepository = new ProgressionRepository(this);
        season = ArenaRating.seasonKey(System.currentTimeMillis());

        binding.btnArenaBack.setOnClickListener(v -> finish());
        binding.btnArenaSearch.setOnClickListener(v -> {
            if (searching) {
                stopSearch();
            } else {
                beginSearch();
            }
        });
        binding.btnArenaFight.setOnClickListener(v -> startBattle());
        binding.btnArenaRematch.setOnClickListener(v -> beginSearch());
        binding.btnArenaFormation.setOnClickListener(v ->
                startActivity(new Intent(this, FormationActivity.class)));
        binding.btnArenaRetry.setOnClickListener(v -> retryLoad());

        binding.tvArenaSeason.setText(getString(R.string.arena_season,
                season.length() > 1 ? season.substring(1).replace('_', '.') : season));
        binding.tvArenaName.setText(username);
        binding.tvArenaOpponentName.setText("");
        binding.tvArenaOpponentCp.setText("");
        binding.tvArenaOpponentDelta.setText("");
        binding.btnArenaFight.setVisibility(View.GONE);
        binding.btnArenaRematch.setVisibility(View.GONE);
        binding.btnArenaFormation.setVisibility(View.GONE);
        binding.btnArenaRetry.setVisibility(View.GONE);
        renderState();
        showLoading(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) {
            return;
        }
        clearOpponent();
        attachObservers();
        loadLeaderboard();
        ArenaRepository.claimDefense(uid, System.currentTimeMillis(),
                new ArenaRepository.DefenseCallback() {
                    @Override
                    public void onDone(int applied) {
                        if (isDestroyed() || applied <= 0) {
                            return;
                        }
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.arena_defense_applied, applied),
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String reason) {
                        // Defense results retry on the next visit.
                    }
                });
        handler.postDelayed(regenTick, REGEN_TICK_MS);
    }

    @Override
    protected void onStop() {
        stopSearch();
        handler.removeCallbacks(regenTick);
        detachObservers();
        super.onStop();
    }

    private void attachObservers() {
        if (playerHandle != null || formationHandle != null || stateHandle != null) {
            return;
        }
        profileRef = FirebaseRefs.profile(uid);
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshotData) {
                if (!snapshotData.exists()) {
                    profileLoaded = true;
                    rebuildSnapshot();
                    return;
                }
                PlayerProfile profile = PlayerProfile.from(snapshotData, uid);
                username = profile.username;
                title = Texts.pretty(profile.titleId);
                binding.tvArenaName.setText(username);
                profileLoaded = true;
                rebuildSnapshot();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                profileLoaded = true;
                rebuildSnapshot();
            }
        };
        profileRef.addValueEventListener(profileListener);

        playerHandle = progressionRepository.observe(uid,
                new ProgressionRepository.StateCallback() {
                    @Override
                    public void onData(PlayerState data) {
                        roster.clear();
                        roster.addAll(data.roster.values());
                        equipment.clear();
                        equipment.putAll(data.equipment);
                        playerLoaded = true;
                        rebuildSnapshot();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        showLoadError(UiErrors.database(ArenaActivity.this, error));
                    }
                });

        formationHandle = RosterRepository.observeFormation(uid,
                new RosterRepository.FormationCallback() {
                    @Override
                    public void onData(FormationState data) {
                        formation = data;
                        formationLoaded = true;
                        rebuildSnapshot();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        showLoadError(UiErrors.database(ArenaActivity.this, error));
                    }
                });

        stateHandle = ArenaRepository.observeState(uid, new ArenaRepository.StateCallback() {
            @Override
            public void onData(ArenaState data) {
                state = data;
                stateLoaded = true;
                renderState();
                rebuildSnapshot();
            }

            @Override
            public void onError(DatabaseError error) {
                showLoadError(UiErrors.database(ArenaActivity.this, error));
            }
        });
    }

    private void detachObservers() {
        if (profileRef != null && profileListener != null) {
            profileRef.removeEventListener(profileListener);
        }
        profileRef = null;
        profileListener = null;
        if (playerHandle != null) {
            playerHandle.detach();
            playerHandle = null;
        }
        if (formationHandle != null) {
            formationHandle.detach();
            formationHandle = null;
        }
        if (stateHandle != null) {
            stateHandle.detach();
            stateHandle = null;
        }
    }

    private void rebuildSnapshot() {
        if (!playerLoaded || !formationLoaded || !stateLoaded) {
            return;
        }
        showLoading(false);
        binding.tvArenaError.setVisibility(View.GONE);
        binding.btnArenaRetry.setVisibility(View.GONE);

        ArenaSnapshot built = ArenaSnapshotFactory.build(uid, username, title, formation,
                roster, equipment, catalog, equipCatalog, state.rating,
                System.currentTimeMillis());
        snapshot = built != null && built.isUsable() ? built : null;

        binding.btnArenaFormation.setVisibility(snapshot == null ? View.VISIBLE : View.GONE);
        binding.btnArenaSearch.setEnabled(snapshot != null);
        if (snapshot == null && !searching) {
            binding.tvArenaOpponentTitle.setText(R.string.arena_need_formation);
            binding.tvArenaOpponentName.setText("");
            binding.tvArenaOpponentCp.setText("");
            binding.tvArenaOpponentDelta.setText("");
            binding.btnArenaFight.setVisibility(View.GONE);
            binding.btnArenaRematch.setVisibility(View.GONE);
            return;
        }
        if (snapshot == null) {
            stopSearch();
            return;
        }
        ArenaRepository.publishSnapshot(snapshot, new ArenaRepository.SnapshotCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String reason) {
                // Snapshot is republished on the next state change.
            }
        });
        refreshOpponentPreview();
    }

    private void renderState() {
        binding.tvArenaName.setText(username);
        binding.tvArenaRating.setText(numberFormat.format(state.rating));
        binding.tvArenaTier.setText(Texts.pretty(state.tier()));
        binding.tvArenaRecord.setText(getString(R.string.arena_record,
                state.wins, state.losses));
        binding.tvArenaStreak.setText(getString(R.string.arena_streak, state.streak));
        binding.tvArenaPoints.setText(getString(R.string.arena_points, state.points));
        renderTickets();
        refreshOpponentPreview();
    }

    private void renderTickets() {
        long now = System.currentTimeMillis();
        int tickets = state.effectiveTickets(now);
        binding.tvArenaTickets.setText(getString(R.string.arena_tickets, tickets,
                ArenaTickets.MAX));
        binding.tvArenaTicketsTop.setText(getString(R.string.arena_tickets, tickets,
                ArenaTickets.MAX));
        long next = ArenaTickets.nextTicketAt(state.tickets, state.ticketAt, now);
        if (next <= 0L) {
            binding.tvArenaRegen.setText(R.string.arena_ticket_full);
        } else {
            binding.tvArenaRegen.setText(getString(R.string.arena_ticket_regen,
                    formatCountdown(next - now)));
        }
        binding.btnArenaFight.setEnabled(!busy
                && opponent != null && tickets > 0);
    }

    private void refreshOpponentPreview() {
        if (opponent == null) {
            return;
        }
        boolean bot = opponentIsBot || ArenaMatchmaker.isBot(opponent.uid);
        binding.tvArenaOpponentName.setText(opponent.username);
        binding.tvArenaOpponentCp.setText(bot
                ? getString(R.string.arena_opponent_bot, numberFormat.format(opponent.cp))
                : getString(R.string.arena_opponent_cp, numberFormat.format(opponent.cp),
                opponent.rating));
        int winDelta = ArenaRating.delta(state.rating, opponent.rating, true,
                state.streak, bot);
        int lossDelta = ArenaRating.delta(state.rating, opponent.rating, false,
                state.streak, bot);
        binding.tvArenaOpponentDelta.setText(
                getString(R.string.arena_delta_preview, winDelta, lossDelta));
    }

    private boolean hasNetwork() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) {
            return true;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void beginSearch() {
        if (busy || searching || snapshot == null) {
            return;
        }
        if (state.effectiveTickets(System.currentTimeMillis()) <= 0) {
            showMessage(R.string.arena_no_tickets);
            return;
        }
        if (!hasNetwork()) {
            showMessage(R.string.error_network);
            return;
        }
        clearOpponent();
        searching = true;
        searchStart = SystemClock.elapsedRealtime();
        binding.btnArenaSearch.setText(R.string.arena_search_stop);
        binding.btnArenaSearch.setEnabled(true);
        binding.btnArenaFight.setVisibility(View.GONE);
        binding.btnArenaRematch.setVisibility(View.GONE);
        searchTick();
    }

    private void stopSearch() {
        searching = false;
        searchInFlight = false;
        handler.removeCallbacks(searchTick);
        if (binding == null) {
            return;
        }
        binding.btnArenaSearch.setText(R.string.arena_search);
        binding.btnArenaSearch.setEnabled(!busy && snapshot != null);
        if (opponent == null) {
            binding.tvArenaOpponentTitle.setText(R.string.arena_search);
        }
        binding.btnArenaFight.setEnabled(!busy && opponent != null
                && state.effectiveTickets(System.currentTimeMillis()) > 0);
    }

    private void searchTick() {
        if (!searching || snapshot == null) {
            return;
        }
        long elapsed = SystemClock.elapsedRealtime() - searchStart;
        if (elapsed >= SEARCH_MAX_MS) {
            stopSearch();
            showMessage(R.string.arena_search_timeout);
            return;
        }
        binding.tvArenaOpponentTitle.setText(
                getString(R.string.arena_searching, elapsed / 1000L));
        if (searchInFlight) {
            handler.postDelayed(searchTick, SEARCH_TICK_MS);
            return;
        }
        searchInFlight = true;
        ArenaSnapshot self = snapshot;
        ArenaRepository.findOpponent(self, elapsed, new ArenaRepository.MatchCallback() {
            @Override
            public void onData(ArenaSnapshot found, boolean bot) {
                searchInFlight = false;
                if (isDestroyed() || !searching) {
                    return;
                }
                showOpponent(found, bot);
            }

            @Override
            public void onError(String reason) {
                searchInFlight = false;
                if (isDestroyed() || !searching) {
                    return;
                }
                stopSearch();
                showMessage(R.string.arena_error);
            }
        });
        handler.postDelayed(searchTick, SEARCH_TICK_MS);
    }

    private void showOpponent(ArenaSnapshot found, boolean bot) {
        searching = false;
        handler.removeCallbacks(searchTick);
        opponent = found;
        opponentIsBot = bot;
        binding.tvArenaOpponentTitle.setText(R.string.arena_opponent);
        binding.btnArenaSearch.setText(R.string.arena_search);
        binding.btnArenaSearch.setEnabled(true);
        binding.btnArenaFight.setVisibility(View.VISIBLE);
        binding.btnArenaRematch.setVisibility(View.VISIBLE);
        refreshOpponentPreview();
        renderTickets();
    }

    private void clearOpponent() {
        opponent = null;
        opponentIsBot = false;
        if (binding == null) {
            return;
        }
        binding.tvArenaOpponentTitle.setText(R.string.arena_search);
        binding.tvArenaOpponentName.setText("");
        binding.tvArenaOpponentCp.setText("");
        binding.tvArenaOpponentDelta.setText("");
        binding.btnArenaFight.setVisibility(View.GONE);
        binding.btnArenaRematch.setVisibility(View.GONE);
    }

    private void startBattle() {
        if (busy || snapshot == null || opponent == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (state.effectiveTickets(now) <= 0) {
            showMessage(R.string.arena_no_tickets);
            return;
        }
        setBusy(true);
        ArenaRepository.spendTicket(uid, now, new ArenaRepository.TicketCallback() {
            @Override
            public void onSuccess(int tickets) {
                long seed = System.nanoTime() ^ (System.currentTimeMillis() * 0x9E3779B97F4A7C15L);
                ArenaRepository.createBattle(snapshot, opponent, seed,
                        System.currentTimeMillis(), new ArenaRepository.BattleCallback() {
                            @Override
                            public void onData(ArenaBattle battle) {
                                if (isDestroyed()) {
                                    return;
                                }
                                setBusy(false);
                                Intent intent = new Intent(ArenaActivity.this,
                                        BattleActivity.class);
                                intent.putExtra(BattleActivity.EXTRA_BATTLE_ID, battle.bid);
                                startActivity(intent);
                            }

                            @Override
                            public void onError(String reason) {
                                if (isDestroyed()) {
                                    return;
                                }
                                setBusy(false);
                                showMessage(R.string.arena_battle_failed);
                            }
                        });
            }

            @Override
            public void onError(String reason) {
                if (isDestroyed()) {
                    return;
                }
                setBusy(false);
                showMessage(R.string.arena_ticket_failed);
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        handler.removeCallbacks(busyTimeout);
        if (value) {
            handler.postDelayed(busyTimeout, NETWORK_TIMEOUT_MS);
        }
        binding.progressArena.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.btnArenaSearch.setEnabled(!value && !searching && snapshot != null);
        binding.btnArenaFight.setEnabled(!value && opponent != null
                && state.effectiveTickets(System.currentTimeMillis()) > 0);
        binding.btnArenaRematch.setEnabled(!value);
        binding.btnArenaFormation.setEnabled(!value);
    }

    private void loadLeaderboard() {
        ArenaRepository.loadLeaderboard(season, new ArenaRepository.LeaderboardCallback() {
            @Override
            public void onData(List<ArenaRepository.LeaderRow> rows) {
                if (isDestroyed()) {
                    return;
                }
                if (rows.isEmpty()) {
                    binding.tvArenaLeaderboard.setText("");
                    binding.tvArenaLeaderboardEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                binding.tvArenaLeaderboardEmpty.setVisibility(View.GONE);
                StringBuilder builder = new StringBuilder();
                int rank = 1;
                for (ArenaRepository.LeaderRow row : rows) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    String name = row.uid.equals(uid)
                            ? row.username + " " + getString(R.string.arena_you)
                            : row.username;
                    builder.append(getString(R.string.arena_leader_row, rank++, name,
                            numberFormat.format(row.score)));
                }
                binding.tvArenaLeaderboard.setText(builder.toString());
            }

            @Override
            public void onError(DatabaseError error) {
                if (isDestroyed()) {
                    return;
                }
                binding.tvArenaLeaderboardEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showLoading(boolean visible) {
        binding.progressArena.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            binding.btnArenaSearch.setEnabled(false);
            binding.btnArenaFight.setEnabled(false);
        }
    }

    private void showLoadError(String message) {
        showLoading(false);
        binding.tvArenaError.setText(message);
        binding.tvArenaError.setVisibility(View.VISIBLE);
        binding.btnArenaRetry.setVisibility(View.VISIBLE);
    }

    private void retryLoad() {
        binding.tvArenaError.setVisibility(View.GONE);
        binding.btnArenaRetry.setVisibility(View.GONE);
        showLoading(true);
        playerLoaded = false;
        formationLoaded = false;
        stateLoaded = false;
        profileLoaded = false;
        detachObservers();
        attachObservers();
        loadLeaderboard();
    }

    private void showMessage(int messageRes) {
        Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_SHORT).show();
    }

    private static String formatCountdown(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.getDefault(), "%02d:%02d",
                seconds / 60L, seconds % 60L);
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
