package zhaf.media.themyth.ui.missions;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.meta.DailyLoginLogic;
import zhaf.media.themyth.data.meta.IdleReward;
import zhaf.media.themyth.data.meta.IdleRewardLogic;
import zhaf.media.themyth.data.meta.LoginReward;
import zhaf.media.themyth.data.meta.MissionCatalog;
import zhaf.media.themyth.data.meta.MissionDef;
import zhaf.media.themyth.data.meta.MissionLogic;
import zhaf.media.themyth.data.meta.PlayerMeta;
import zhaf.media.themyth.data.repository.MetaRepository;
import zhaf.media.themyth.databinding.ActivityMissionsBinding;
import zhaf.media.themyth.databinding.ItemMissionBinding;
import zhaf.media.themyth.ui.login.LoginActivity;

public class MissionsActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 12_000L;
    private static final long TICK_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> claimingMissions = new HashSet<>();

    private ActivityMissionsBinding binding;
    private MetaRepository.Handle metaHandle;
    private DatabaseReference profileRef;
    private ValueEventListener profileListener;
    private String uid;

    private PlayerMeta meta;
    private int level = 1;
    private boolean metaLoaded;
    private boolean claimingLogin;
    private boolean claimingIdle;

    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (metaLoaded || isFinishing() || isDestroyed()) {
                return;
            }
            showLoading(false);
            binding.missionsErrorBox.setVisibility(View.VISIBLE);
            binding.tvMissionsError.setText(R.string.missions_error_load);
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (metaLoaded) {
                render();
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = new AuthRepository(this).currentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        binding.btnMissionsBack.setOnClickListener(v -> finish());
        binding.btnMissionsRetry.setOnClickListener(v -> retryLoad());
        binding.btnClaimLogin.setOnClickListener(v -> claimLogin());
        binding.btnClaimIdle.setOnClickListener(v -> claimIdle());
        buildLoginDays();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) {
            return;
        }
        attach();
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    protected void onStop() {
        detach();
        handler.removeCallbacks(tick);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(loadTimeout);
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void attach() {
        showLoading(!metaLoaded);
        binding.missionsErrorBox.setVisibility(View.GONE);
        if (!metaLoaded) {
            handler.removeCallbacks(loadTimeout);
            handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);
        }
        metaHandle = MetaRepository.observe(uid, new MetaRepository.MetaCallback() {
            @Override
            public void onData(PlayerMeta data) {
                handler.removeCallbacks(loadTimeout);
                meta = data;
                metaLoaded = true;
                showLoading(false);
                binding.missionsErrorBox.setVisibility(View.GONE);
                render();
            }

            @Override
            public void onError(DatabaseError error) {
                handler.removeCallbacks(loadTimeout);
                showLoading(false);
                binding.missionsErrorBox.setVisibility(View.VISIBLE);
                binding.tvMissionsError.setText(UiErrors.database(MissionsActivity.this, error));
            }
        });

        profileRef = FirebaseRefs.profile(uid);
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long value = snapshot.child("level").getValue(Long.class);
                level = value == null ? 1 : Math.max(1, value.intValue());
                if (metaLoaded) {
                    render();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Level falls back to 1 for idle caps.
            }
        };
        profileRef.addValueEventListener(profileListener);
    }

    private void detach() {
        if (metaHandle != null) {
            metaHandle.detach();
            metaHandle = null;
        }
        if (profileRef != null && profileListener != null) {
            profileRef.removeEventListener(profileListener);
        }
        profileRef = null;
        profileListener = null;
        handler.removeCallbacks(loadTimeout);
    }

    private void retryLoad() {
        detach();
        metaLoaded = false;
        binding.missionsErrorBox.setVisibility(View.GONE);
        attach();
    }

    private void render() {
        if (meta == null) {
            return;
        }
        long now = System.currentTimeMillis();
        binding.missionsContent.setVisibility(View.VISIBLE);
        renderLogin(now);
        renderIdle(now);
        renderMissions();
    }

    private void renderLogin(long now) {
        boolean canClaim = DailyLoginLogic.canClaim(meta.loginDay, now);
        int nextDay = DailyLoginLogic.nextStreak(meta.loginDay, meta.loginStreak, now);
        int shownDay = canClaim ? nextDay : Math.max(1, meta.loginStreak);
        LoginReward reward = DailyLoginLogic.rewardFor(shownDay);
        binding.tvLoginStreak.setText(getString(R.string.missions_login_streak,
                shownDay, DailyLoginLogic.CYCLE_DAYS));
        binding.tvLoginReward.setText(formatReward(reward.coin, reward.diamond,
                reward.energy, reward.tickets));
        binding.tvLoginHint.setText(canClaim
                ? R.string.missions_login_hint
                : R.string.missions_login_claimed);
        binding.btnClaimLogin.setEnabled(canClaim && !claimingLogin);
        binding.btnClaimLogin.setAlpha(canClaim && !claimingLogin ? 1f : 0.45f);
        binding.btnClaimLogin.setText(canClaim ? R.string.missions_claim
                : R.string.missions_claimed);
        highlightLoginDay(shownDay);
    }

    private void renderIdle(long now) {
        long accrued = IdleRewardLogic.accruedMs(meta.idleAt, now, level);
        int capMinutes = IdleRewardLogic.capMinutes(level);
        IdleReward reward = IdleRewardLogic.reward(accrued, level);
        binding.tvIdleInfo.setText(getString(R.string.missions_idle_info,
                formatMinutes(accrued / 60000L), formatMinutes(capMinutes)));
        binding.tvIdleReward.setText(reward.isEmpty()
                ? getString(R.string.missions_idle_not_ready)
                : formatReward(reward.coin, 0, reward.energy, 0));
        boolean ready = IdleRewardLogic.ready(accrued);
        binding.tvIdleHint.setText(ready
                ? R.string.missions_idle_hint
                : R.string.missions_idle_not_ready);
        binding.btnClaimIdle.setEnabled(ready && !claimingIdle);
        binding.btnClaimIdle.setAlpha(ready && !claimingIdle ? 1f : 0.45f);
        binding.btnClaimIdle.setText(ready ? R.string.missions_claim
                : R.string.missions_idle_wait);
    }

    private void renderMissions() {
        binding.dailyList.removeAllViews();
        for (MissionDef def : MissionCatalog.daily()) {
            binding.dailyList.addView(buildMissionRow(def, binding.dailyList));
        }
        binding.weeklyList.removeAllViews();
        for (MissionDef def : MissionCatalog.weekly()) {
            binding.weeklyList.addView(buildMissionRow(def, binding.weeklyList));
        }
    }

    private View buildMissionRow(MissionDef def, View parent) {
        ItemMissionBinding row = ItemMissionBinding.inflate(
                LayoutInflater.from(this), (android.view.ViewGroup) parent, false);
        long now = System.currentTimeMillis();
        int progress = meta.progressOf(def, now);
        boolean claimed = meta.claimed(def, now);
        boolean canClaim = MissionLogic.canClaim(progress, def.target, claimed);
        row.tvMissionTitle.setText(def.title());
        row.tvMissionDesc.setText(def.description());
        row.tvMissionProgress.setText(getString(R.string.missions_progress,
                Math.min(progress, def.target), def.target));
        row.tvMissionReward.setText(formatReward(def.coin, def.diamond,
                def.energy, def.tickets));
        row.btnMissionClaim.setText(claimed ? R.string.missions_claimed
                : R.string.missions_claim);
        row.btnMissionClaim.setEnabled(canClaim && !claimingMissions.contains(def.id));
        row.btnMissionClaim.setAlpha(
                canClaim && !claimingMissions.contains(def.id) ? 1f : 0.45f);
        row.btnMissionClaim.setOnClickListener(v -> claimMission(def));
        return row.getRoot();
    }

    private void claimLogin() {
        if (claimingLogin || meta == null) {
            return;
        }
        claimingLogin = true;
        binding.btnClaimLogin.setEnabled(false);
        MetaRepository.claimLogin(uid, System.currentTimeMillis(),
                new MetaRepository.ClaimCallback() {
                    @Override
                    public void onSuccess(MetaRepository.ClaimResult result) {
                        claimingLogin = false;
                        if (isDestroyed()) {
                            return;
                        }
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.missions_login_success, result.streak),
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String reason) {
                        claimingLogin = false;
                        if (isDestroyed()) {
                            return;
                        }
                        int message = MetaRepository.ERROR_ALREADY.equals(reason)
                                ? R.string.missions_login_claimed : R.string.error_network;
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        render();
                    }
                });
    }

    private void claimIdle() {
        if (claimingIdle || meta == null) {
            return;
        }
        claimingIdle = true;
        binding.btnClaimIdle.setEnabled(false);
        MetaRepository.claimIdle(uid, System.currentTimeMillis(), level,
                new MetaRepository.ClaimCallback() {
                    @Override
                    public void onSuccess(MetaRepository.ClaimResult result) {
                        claimingIdle = false;
                        if (isDestroyed()) {
                            return;
                        }
                        Snackbar.make(binding.getRoot(), R.string.missions_idle_success,
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String reason) {
                        claimingIdle = false;
                        if (isDestroyed()) {
                            return;
                        }
                        int message = MetaRepository.ERROR_NOT_READY.equals(reason)
                                ? R.string.missions_idle_not_ready : R.string.error_network;
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        render();
                    }
                });
    }

    private void claimMission(MissionDef def) {
        if (def == null || meta == null || claimingMissions.contains(def.id)) {
            return;
        }
        claimingMissions.add(def.id);
        renderMissions();
        MetaRepository.claimMission(uid, def, System.currentTimeMillis(),
                new MetaRepository.ClaimCallback() {
                    @Override
                    public void onSuccess(MetaRepository.ClaimResult result) {
                        claimingMissions.remove(def.id);
                        if (isDestroyed()) {
                            return;
                        }
                        Snackbar.make(binding.getRoot(), R.string.missions_claim_success,
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String reason) {
                        claimingMissions.remove(def.id);
                        if (isDestroyed()) {
                            return;
                        }
                        int message;
                        if (MetaRepository.ERROR_ALREADY.equals(reason)) {
                            message = R.string.missions_claimed_already;
                        } else if (MetaRepository.ERROR_NOT_READY.equals(reason)) {
                            message = R.string.missions_not_ready;
                        } else {
                            message = R.string.error_network;
                        }
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        renderMissions();
                    }
                });
    }

    private void buildLoginDays() {
        binding.loginDaysRow.removeAllViews();
        for (int day = 1; day <= DailyLoginLogic.CYCLE_DAYS; day++) {
            LoginReward reward = DailyLoginLogic.rewardFor(day);
            android.widget.TextView chip = new android.widget.TextView(this);
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginEnd(dp(4));
            chip.setLayoutParams(params);
            chip.setBackgroundResource(R.drawable.bg_card);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(4), dp(8), dp(4), dp(8));
            chip.setTextSize(11f);
            chip.setTextColor(getColor(R.color.text_muted));
            String shortCoin = reward.coin >= 1000
                    ? String.format(Locale.ROOT, "%dk", reward.coin / 1000)
                    : String.valueOf(reward.coin);
            chip.setText(getString(R.string.missions_login_day_chip, day, shortCoin));
            chip.setTag(day);
            binding.loginDaysRow.addView(chip);
        }
    }

    private void highlightLoginDay(int day) {
        for (int i = 0; i < binding.loginDaysRow.getChildCount(); i++) {
            View child = binding.loginDaysRow.getChildAt(i);
            boolean active = child.getTag() instanceof Integer
                    && ((Integer) child.getTag()) == day;
            child.setBackgroundResource(active
                    ? R.drawable.bg_card_selected : R.drawable.bg_card);
            if (child instanceof android.widget.TextView) {
                ((android.widget.TextView) child).setTextColor(getColor(active
                        ? R.color.gold : R.color.text_muted));
            }
        }
    }

    private String formatReward(int coin, int diamond, int energy, int tickets) {
        StringBuilder builder = new StringBuilder();
        if (coin > 0) {
            appendLine(builder, getString(R.string.missions_reward_coin, Texts.number(coin)));
        }
        if (diamond > 0) {
            appendLine(builder, getString(R.string.missions_reward_diamond,
                    Texts.number(diamond)));
        }
        if (energy > 0) {
            appendLine(builder, getString(R.string.missions_reward_energy,
                    Texts.number(energy)));
        }
        if (tickets > 0) {
            appendLine(builder, getString(R.string.missions_reward_ticket,
                    Texts.number(tickets)));
        }
        return builder.length() == 0 ? getString(R.string.missions_claimed) : builder.toString();
    }

    private static void appendLine(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(value);
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return getString(R.string.missions_idle_hours,
                    minutes / 60L, minutes % 60L);
        }
        return getString(R.string.missions_idle_minutes, minutes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLoading(boolean visible) {
        binding.progressMissions.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            binding.missionsContent.setVisibility(View.GONE);
        }
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
