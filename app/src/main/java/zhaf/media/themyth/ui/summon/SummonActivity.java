package zhaf.media.themyth.ui.summon;

import android.content.Intent;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.repository.SummonRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.GachaCatalog;
import zhaf.media.themyth.data.staticdata.GachaPool;
import zhaf.media.themyth.data.staticdata.GameConstants;
import zhaf.media.themyth.databinding.ActivitySummonBinding;
import zhaf.media.themyth.ui.common.GameUi;
import zhaf.media.themyth.ui.login.LoginActivity;

public class SummonActivity extends AppCompatActivity {

    private static final SimpleDateFormat HISTORY_TIME =
            new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    private ActivitySummonBinding binding;
    private CharacterCatalog characterCatalog;
    private SummonResultAdapter adapter;

    private final List<GachaPool> pools = new ArrayList<>();
    private int selectedPool;
    private SummonRepository.Handle handle;
    private SummonRepository.Snapshot snapshot;
    private SummonRepository.PoolProgress progress = new SummonRepository.PoolProgress(0, 0, 0);

    private String uid;
    private long diamond;
    private long ticket;
    private boolean busy;
    private boolean loading = true;
    private boolean loadFailed;
    private boolean payWithTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySummonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        characterCatalog = CharacterCatalog.get(this);
        pools.addAll(GachaCatalog.get(this).all());
        adapter = new SummonResultAdapter(characterCatalog);

        binding.resultsGrid.setLayoutManager(new GridLayoutManager(this, 5));
        binding.resultsGrid.setAdapter(adapter);

        binding.btnSummonBack.setOnClickListener(v -> finish());
        binding.btnSummonRetry.setOnClickListener(v -> retry());
        binding.btnSummonHistory.setOnClickListener(v -> showHistory());
        binding.tvPayToggle.setOnClickListener(v -> togglePayment());
        binding.btnPull1.setOnClickListener(v -> confirmPull(1));
        binding.btnPull10.setOnClickListener(v -> confirmPull(10));

        buildPoolChips();
        if (pools.isEmpty()) {
            loading = false;
            loadFailed = true;
            binding.tvSummonError.setText(R.string.summon_invalid_pool);
        }
        renderAll();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attach();
    }

    @Override
    protected void onStop() {
        detach();
        super.onStop();
    }

    private void attach() {
        if (handle != null || uid == null) {
            return;
        }
        handle = SummonRepository.observe(uid, new SummonRepository.SnapshotCallback() {
            @Override
            public void onData(SummonRepository.Snapshot data) {
                snapshot = data;
                diamond = data.diamond;
                ticket = data.ticket;
                progress = data.progressOf(currentPoolId());
                loading = false;
                loadFailed = false;
                renderAll();
            }

            @Override
            public void onError(DatabaseError error) {
                loading = false;
                if (snapshot == null) {
                    loadFailed = true;
                    binding.tvSummonError.setText(error == null
                            ? getString(R.string.summon_error)
                            : UiErrors.database(SummonActivity.this, error));
                } else {
                    snackbar(error == null
                            ? getString(R.string.summon_error)
                            : UiErrors.database(SummonActivity.this, error));
                }
                renderAll();
            }
        });
    }

    private void detach() {
        if (handle != null) {
            handle.detach();
            handle = null;
        }
    }

    private void retry() {
        detach();
        loading = true;
        loadFailed = false;
        snapshot = null;
        renderAll();
        attach();
    }

    private void buildPoolChips() {
        binding.poolChips.removeAllViews();
        for (int i = 0; i < pools.size(); i++) {
            final int index = i;
            TextView chip = createChip(pools.get(i).displayName());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                params.setMarginStart(dp(8));
            }
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> selectPool(index));
            binding.poolChips.addView(chip);
        }
    }

    private TextView createChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setGravity(Gravity.CENTER);
        chip.setMaxLines(1);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setBackgroundResource(R.drawable.bg_filter_chip);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_state));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setPadding(dp(10), dp(8), dp(10), dp(8));
        return chip;
    }

    private void selectPool(int index) {
        if (busy || index == selectedPool || index < 0 || index >= pools.size()) {
            return;
        }
        selectedPool = index;
        adapter.submit(null);
        binding.tvResultsTitle.setText(R.string.summon_results_heading);
        binding.tvResultsSummary.setText("");
        progress = snapshot == null
                ? new SummonRepository.PoolProgress(0, 0, 0)
                : snapshot.progressOf(currentPoolId());
        renderAll();
    }

    private void togglePayment() {
        if (busy) {
            return;
        }
        payWithTicket = !payWithTicket;
        renderAll();
    }

    private void renderAll() {
        binding.tvSummonDiamond.setText(Texts.number(diamond));
        binding.tvSummonTicket.setText(Texts.number(ticket));
        binding.tvSummonSubtitle.setText(getString(R.string.summon_subtitle,
                Texts.number(progress.totalPulls)));

        renderChips();
        renderBanner();
        renderPity();
        renderStates();
        updateButtons();
    }

    private void renderChips() {
        for (int i = 0; i < binding.poolChips.getChildCount(); i++) {
            binding.poolChips.getChildAt(i).setSelected(i == selectedPool);
        }
    }

    private void renderBanner() {
        GachaPool pool = currentPool();
        if (pool == null) {
            binding.tvBannerName.setText("");
            binding.tvBannerDesc.setText("");
            binding.ratesGrid.removeAllViews();
            return;
        }
        binding.tvBannerName.setText(pool.displayName());
        binding.tvBannerDesc.setText(pool.description());
        renderRates(pool);
    }

    private void renderRates(GachaPool pool) {
        binding.ratesGrid.removeAllViews();
        for (String rarity : GameConstants.RARITY_LADDER) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView label = new TextView(this);
            label.setText(getString(GameUi.rarityLabel(rarity)));
            label.setTextColor(ContextCompat.getColor(this, GameUi.rarityColor(rarity)));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView value = new TextView(this);
            value.setText(GameUi.percent(pool.rateOf(rarity)));
            value.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            value.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(label);
            row.addView(value);
            binding.ratesGrid.addView(row);
        }
    }

    private void renderPity() {
        GachaPool pool = currentPool();
        if (pool == null) {
            return;
        }
        binding.progressPityEpic.setMax(pool.pityEpic);
        binding.progressPityEpic.setProgress(Math.min(progress.epicPity, pool.pityEpic));
        binding.tvPityEpic.setText(getString(R.string.summon_pity_epic,
                progress.epicPity, pool.pityEpic));

        binding.progressPityLegendary.setMax(pool.pityLegendary);
        binding.progressPityLegendary.setProgress(
                Math.min(progress.legendaryPity, pool.pityLegendary));
        binding.tvPityLegendary.setText(getString(R.string.summon_pity_legendary,
                progress.legendaryPity, pool.pityLegendary));
    }

    private void renderStates() {
        boolean showLoading = (loading && snapshot == null) || busy;
        binding.progressSummon.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        binding.summonErrorState.setVisibility(
                loadFailed && snapshot == null && !busy ? View.VISIBLE : View.GONE);
        boolean empty = adapter.getItemCount() == 0 && !showLoading
                && !(loadFailed && snapshot == null);
        binding.tvSummonEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.resultsGrid.setVisibility(View.VISIBLE);
    }

    private void updateButtons() {
        GachaPool pool = currentPool();
        boolean hasPool = pool != null;
        if (!hasPool) {
            binding.btnPull1.setEnabled(false);
            binding.btnPull10.setEnabled(false);
            binding.tvPayToggle.setEnabled(false);
            return;
        }

        int cost1 = payWithTicket ? pool.ticketCost(1) : (int) pool.diamondCost(1);
        int cost10 = payWithTicket ? pool.ticketCost(10) : (int) pool.diamondCost(10);
        binding.btnPull1.setText(getString(R.string.summon_pull_short, 1,
                Texts.number(cost1)));
        binding.btnPull10.setText(getString(R.string.summon_pull_short, 10,
                Texts.number(cost10)));

        long balance = payWithTicket ? ticket : diamond;
        boolean can1 = !busy && balance >= cost1;
        boolean can10 = !busy && balance >= cost10;
        binding.btnPull1.setEnabled(can1);
        binding.btnPull1.setAlpha(can1 ? 1f : 0.45f);
        binding.btnPull10.setEnabled(can10);
        binding.btnPull10.setAlpha(can10 ? 1f : 0.45f);

        binding.tvPayToggle.setSelected(payWithTicket);
        binding.tvPayToggle.setEnabled(!busy);
        binding.tvPayToggle.setText(payWithTicket
                ? getString(R.string.summon_pay_ticket, Texts.number(ticket))
                : getString(R.string.summon_pay_diamond));
    }

    private void confirmPull(int count) {
        GachaPool pool = currentPool();
        if (pool == null || busy) {
            return;
        }
        long balance = payWithTicket ? ticket : diamond;
        int cost = payWithTicket ? pool.ticketCost(count) : (int) pool.diamondCost(count);
        if (balance < cost) {
            snackbar(getString(payWithTicket
                    ? R.string.summon_not_enough_ticket
                    : R.string.summon_not_enough_diamond));
            return;
        }
        String costText = payWithTicket
                ? getString(R.string.summon_cost_ticket, Texts.number(cost))
                : getString(R.string.summon_cost_diamond, Texts.number(cost));
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.summon_confirm_title, count))
                .setMessage(getString(R.string.summon_confirm_message,
                        pool.displayName(), costText,
                        progress.epicPity, pool.pityEpic,
                        progress.legendaryPity, pool.pityLegendary))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.summon_confirm_yes,
                        (dialog, which) -> performPull(count))
                .show();
    }

    private void performPull(int count) {
        GachaPool pool = currentPool();
        if (pool == null || busy) {
            return;
        }
        busy = true;
        renderStates();
        updateButtons();

        SummonRepository.pull(this, uid, pool, count,
                payWithTicket ? SummonRepository.Payment.TICKET
                        : SummonRepository.Payment.DIAMOND,
                new SummonRepository.PullCallback() {
                    @Override
                    public void onSuccess(SummonRepository.Result result) {
                        busy = false;
                        diamond = result.diamond;
                        ticket = result.ticket;
                        progress = result.progress;
                        adapter.submit(result.results);
                        binding.tvResultsTitle.setText(
                                getString(R.string.summon_results_title, count));
                        binding.tvResultsSummary.setText(getString(
                                R.string.summon_results_summary,
                                result.newCount,
                                result.results.size() - result.newCount,
                                result.shardCount));
                        binding.resultsGrid.scrollToPosition(0);
                        renderAll();
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        busy = false;
                        renderAll();
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void showHistory() {
        GachaPool pool = currentPool();
        if (pool == null || snapshot == null) {
            return;
        }
        List<SummonRepository.HistoryEntry> entries = snapshot.historyOf(pool.id);
        if (entries.isEmpty()) {
            snackbar(getString(R.string.summon_history_empty));
            return;
        }
        List<String> labels = new ArrayList<>();
        for (SummonRepository.HistoryEntry entry : entries) {
            CharacterTemplate template = characterCatalog.find(entry.charId);
            String name = template == null ? Texts.pretty(entry.charId) : template.name;
            labels.add(getString(R.string.summon_history_line,
                    name,
                    getString(GameUi.rarityLabel(entry.rarity)),
                    HISTORY_TIME.format(new Date(entry.at))));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.summon_history_title, pool.displayName()))
                .setItems(labels.toArray(new String[0]), null)
                .setPositiveButton(R.string.login_ok, null)
                .show();
    }

    private GachaPool currentPool() {
        if (selectedPool < 0 || selectedPool >= pools.size()) {
            return null;
        }
        return pools.get(selectedPool);
    }

    private String currentPoolId() {
        GachaPool pool = currentPool();
        return pool == null ? "" : pool.id;
    }

    private void snackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
