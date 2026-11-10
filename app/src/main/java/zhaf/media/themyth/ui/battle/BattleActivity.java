package zhaf.media.themyth.ui.battle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import zhaf.media.themyth.R;
import zhaf.media.themyth.battle.BattleSetup;
import zhaf.media.themyth.battle.engine.BattleEngine;
import zhaf.media.themyth.battle.engine.BattleEvent;
import zhaf.media.themyth.battle.engine.BattleInput;
import zhaf.media.themyth.battle.engine.BattleResult;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.repository.RosterRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.databinding.ActivityBattleBinding;
import zhaf.media.themyth.ui.formation.FormationActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class BattleActivity extends AppCompatActivity implements BattleFieldView.Listener {

    private static final int MAX_LOG_LINES = 160;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
    private final ArrayDeque<String> logLines = new ArrayDeque<>();
    private final List<RosterChar> roster = new ArrayList<>();
    private final Map<String, EquipmentInstance> equipmentById = new HashMap<>();

    private ActivityBattleBinding binding;
    private CharacterCatalog catalog;
    private EquipmentCatalog equipCatalog;
    private ProgressionRepository progressionRepository;
    private String uid;
    private FormationState formation = FormationState.empty();
    private ProgressionRepository.Handle rosterHandle;
    private RosterRepository.Handle formationHandle;
    private BattleResult currentResult;

    private boolean rosterLoaded;
    private boolean formationLoaded;
    private boolean simulating;
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
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

        binding.battleField.setListener(this);
        binding.battleField.setSpeed(2f);
        selectSpeedChip(binding.chipSpeed2);

        binding.btnBattleBack.setOnClickListener(v -> attemptExit());
        binding.chipSpeed1.setOnClickListener(v -> selectSpeed(1f));
        binding.chipSpeed2.setOnClickListener(v -> selectSpeed(2f));
        binding.chipSpeed3.setOnClickListener(v -> selectSpeed(3f));
        binding.btnBattleRetry.setOnClickListener(v -> retryLoad());
        binding.btnBattleExitError.setOnClickListener(v -> finish());
        binding.btnEmptyFormation.setOnClickListener(v ->
                startActivity(new Intent(this, FormationActivity.class)));
        binding.btnEmptyBack.setOnClickListener(v -> finish());
        binding.btnBattleReplay.setOnClickListener(v -> replay());
        binding.btnBattleResultBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptExit();
            }
        });

        clearLog();
        showLoading(R.string.battle_state_loading);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!started && !simulating) {
            attachObservers();
        }
    }

    @Override
    protected void onStop() {
        detachObservers();
        binding.battleField.pause();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.battleField.resumePlayback();
        if (!started && !simulating && rosterLoaded && formationLoaded) {
            maybeStart();
        }
    }

    @Override
    protected void onDestroy() {
        detachObservers();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void attachObservers() {
        if (uid == null || rosterHandle != null || formationHandle != null) {
            return;
        }
        rosterHandle = progressionRepository.observe(uid,
                new ProgressionRepository.StateCallback() {
                    @Override
                    public void onData(PlayerState data) {
                        if (started) {
                            return;
                        }
                        roster.clear();
                        roster.addAll(data.roster.values());
                        equipmentById.clear();
                        equipmentById.putAll(data.equipment);
                        rosterLoaded = true;
                        maybeStart();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        if (!started) {
                            showLoadError(error);
                        }
                    }
                });
        formationHandle = RosterRepository.observeFormation(uid,
                new RosterRepository.FormationCallback() {
                    @Override
                    public void onData(FormationState data) {
                        if (started) {
                            return;
                        }
                        formation = data;
                        formationLoaded = true;
                        maybeStart();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        if (!started) {
                            showLoadError(error);
                        }
                    }
                });
    }

    private void detachObservers() {
        if (rosterHandle != null) {
            rosterHandle.detach();
            rosterHandle = null;
        }
        if (formationHandle != null) {
            formationHandle.detach();
            formationHandle = null;
        }
    }

    private void maybeStart() {
        if (started || simulating || !rosterLoaded || !formationLoaded) {
            return;
        }
        BattleInput input = BattleSetup.training(catalog, equipCatalog, formation, roster,
                equipmentById, System.nanoTime());
        if (input == null) {
            showEmpty();
            return;
        }
        simulating = true;
        showLoading(R.string.battle_state_preparing);

        executor.execute(() -> {
            BattleResult result;
            try {
                result = BattleEngine.simulate(input);
            } catch (RuntimeException error) {
                mainHandler.post(() -> {
                    if (isDestroyed()) {
                        return;
                    }
                    simulating = false;
                    binding.overlayLoading.setVisibility(View.GONE);
                    binding.tvBattleError.setText(R.string.battle_error);
                    binding.overlayError.setVisibility(View.VISIBLE);
                });
                return;
            }
            mainHandler.post(() -> onSimulationDone(result));
        });
    }

    private void onSimulationDone(BattleResult result) {
        if (isDestroyed()) {
            return;
        }
        simulating = false;
        started = true;
        currentResult = result;
        detachObservers();
        binding.overlayEmpty.setVisibility(View.GONE);
        binding.overlayError.setVisibility(View.GONE);
        binding.battleField.setBattle(result);
    }

    @Override
    public void onReady() {
        if (isDestroyed()) {
            return;
        }
        binding.overlayLoading.setVisibility(View.GONE);
        binding.tvBattleState.setText(R.string.battle_state_running);
        binding.battleField.play();
    }

    @Override
    public void onLog(BattleEvent event) {
        if (isDestroyed() || event.type == BattleEvent.Type.START) {
            return;
        }
        String line = formatEvent(event);
        if (line == null) {
            return;
        }
        if (logLines.size() >= MAX_LOG_LINES) {
            logLines.removeFirst();
        }
        logLines.addLast(line);
        binding.tvBattleLog.setText(TextUtils.join("\n", logLines));
        binding.logScroll.post(() -> binding.logScroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onPlaybackEnd() {
        if (isDestroyed()) {
            return;
        }
        binding.tvBattleState.setText(R.string.battle_state_finished);
        showResult();
    }

    private void showResult() {
        if (currentResult == null) {
            return;
        }
        boolean win = currentResult.playerWin();
        boolean draw = currentResult.draw();
        binding.tvResultTitle.setText(draw ? R.string.battle_result_draw
                : win ? R.string.battle_result_win : R.string.battle_result_lose);
        binding.tvResultTitle.setTextColor(getColor(draw
                ? R.color.cyan : win ? R.color.gold : R.color.danger));

        String seconds = String.format(Locale.getDefault(), "%.1f",
                currentResult.durationMs() / 1000.0);
        binding.tvResultSubtitle.setText(
                getString(R.string.battle_result_subtitle, seconds));

        if (currentResult.mvpName == null) {
            binding.tvResultMvp.setVisibility(View.GONE);
        } else {
            binding.tvResultMvp.setVisibility(View.VISIBLE);
            binding.tvResultMvp.setText(
                    getString(R.string.battle_result_mvp, currentResult.mvpName));
        }

        binding.tvResultDamage.setText(getString(R.string.battle_result_damage,
                numberFormat.format(currentResult.damageDealtBy(0))));
        binding.tvResultHp.setText(getString(R.string.battle_result_hp,
                currentResult.teamHpPctA, currentResult.teamHpPctB));

        String hash = currentResult.hash;
        if (hash == null) {
            hash = "-";
        } else if (hash.length() > 16) {
            hash = hash.substring(0, 16) + "…";
        }
        binding.tvResultHash.setText(getString(R.string.battle_result_hash, hash));

        binding.resultOverlay.setVisibility(View.VISIBLE);
        binding.resultOverlay.setAlpha(0f);
        binding.resultOverlay.animate().alpha(1f).setDuration(220L).start();
    }

    private void replay() {
        if (currentResult == null) {
            return;
        }
        binding.resultOverlay.setVisibility(View.GONE);
        binding.tvBattleState.setText(R.string.battle_state_running);
        clearLog();
        binding.battleField.restart();
    }

    private void selectSpeed(float speed) {
        binding.battleField.setSpeed(speed);
        selectSpeedChip(speed == 1f ? binding.chipSpeed1
                : speed == 2f ? binding.chipSpeed2 : binding.chipSpeed3);
    }

    private void selectSpeedChip(View selected) {
        binding.chipSpeed1.setSelected(selected == binding.chipSpeed1);
        binding.chipSpeed2.setSelected(selected == binding.chipSpeed2);
        binding.chipSpeed3.setSelected(selected == binding.chipSpeed3);
    }

    private String formatEvent(BattleEvent event) {
        switch (event.type) {
            case ACTION:
                return getString(R.string.battle_log_action,
                        safe(event.srcName), safe(event.skillName));
            case HIT: {
                String value = numberFormat.format(event.value);
                return getString(event.crit ? R.string.battle_log_crit : R.string.battle_log_hit,
                        safe(event.dstName), value);
            }
            case DOT:
                return getString(R.string.battle_log_dot, safe(event.dstName),
                        numberFormat.format(event.value), statusName(event.detail));
            case MISS:
                return getString(R.string.battle_log_miss, safe(event.dstName));
            case HEAL:
                return getString(R.string.battle_log_heal, safe(event.dstName),
                        numberFormat.format(event.value));
            case SHIELD:
                return getString(R.string.battle_log_shield, safe(event.dstName),
                        numberFormat.format(event.value));
            case STATUS:
                if (!event.success) {
                    return null;
                }
                return getString(R.string.battle_log_status, safe(event.dstName),
                        statusName(event.detail));
            case DEATH:
                return getString(R.string.battle_log_death, safe(event.dstName));
            case END:
                if (event.value == 0) {
                    return getString(R.string.battle_log_end_win);
                }
                if (event.value == 1) {
                    return getString(R.string.battle_log_end_lose);
                }
                return getString(R.string.battle_log_end_draw);
            default:
                return null;
        }
    }

    private String statusName(String detail) {
        if (detail == null) {
            return "";
        }
        switch (detail) {
            case "BURN":
                return getString(R.string.status_burn);
            case "BLEED":
                return getString(R.string.status_bleed);
            case "WET":
                return getString(R.string.status_wet);
            case "SHOCK":
                return getString(R.string.status_shock);
            case "STUN":
                return getString(R.string.status_stun);
            case "ATK_UP":
                return getString(R.string.status_atk_up);
            case "DEF_DOWN":
                return getString(R.string.status_def_down);
            default:
                return detail;
        }
    }

    private void clearLog() {
        logLines.clear();
        binding.tvBattleLog.setText(R.string.battle_log_empty);
    }

    private void showLoading(@StringRes int message) {
        binding.overlayError.setVisibility(View.GONE);
        binding.overlayEmpty.setVisibility(View.GONE);
        binding.overlayLoading.setVisibility(View.VISIBLE);
        binding.tvLoadingBattle.setText(message);
        binding.tvBattleState.setText(message);
    }

    private void showEmpty() {
        binding.overlayLoading.setVisibility(View.GONE);
        binding.overlayError.setVisibility(View.GONE);
        binding.overlayEmpty.setVisibility(View.VISIBLE);
        binding.tvBattleState.setText(R.string.battle_empty_title);
    }

    private void showLoadError(DatabaseError error) {
        simulating = false;
        binding.overlayLoading.setVisibility(View.GONE);
        binding.overlayEmpty.setVisibility(View.GONE);
        binding.tvBattleError.setText(UiErrors.database(this, error));
        binding.overlayError.setVisibility(View.VISIBLE);
        binding.tvBattleState.setText(R.string.battle_error);
    }

    private void retryLoad() {
        detachObservers();
        started = false;
        simulating = false;
        rosterLoaded = false;
        formationLoaded = false;
        roster.clear();
        formation = FormationState.empty();
        clearLog();
        showLoading(R.string.battle_state_loading);
        attachObservers();
    }

    private void attemptExit() {
        boolean resultShown = binding.resultOverlay.getVisibility() == View.VISIBLE;
        if (!started || resultShown) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battle_back_confirm_title)
                .setMessage(R.string.battle_back_confirm_message)
                .setNegativeButton(R.string.battle_back_confirm_no, null)
                .setPositiveButton(R.string.battle_back_confirm_yes,
                        (dialog, which) -> finish())
                .show();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }
}
