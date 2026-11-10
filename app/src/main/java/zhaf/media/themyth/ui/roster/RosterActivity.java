package zhaf.media.themyth.ui.roster;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.repository.RosterRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.GameConstants;
import zhaf.media.themyth.databinding.ActivityRosterBinding;
import zhaf.media.themyth.ui.common.GameUi;
import zhaf.media.themyth.ui.formation.FormationActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class RosterActivity extends AppCompatActivity {

    private ActivityRosterBinding binding;
    private CharacterCatalog catalog;
    private CharacterCardAdapter adapter;

    private RosterRepository.Handle rosterHandle;
    private String uid;

    private final List<RosterChar> roster = new ArrayList<>();
    private final Map<String, RosterChar> ownedById = new HashMap<>();
    private String elementFilter;
    private String selectedCharId;

    private boolean loading = true;
    private boolean loadFailed;
    private boolean loadedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRosterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        catalog = CharacterCatalog.get(this);
        adapter = new CharacterCardAdapter(this::selectCharacter);

        binding.rosterGrid.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rosterGrid.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnManageFormation.setOnClickListener(v -> openFormation());
        binding.btnDetailFormation.setOnClickListener(v -> openFormation());
        binding.btnRosterRetry.setOnClickListener(v -> retry());

        setupFilters();
        updateChips();
        binding.tvRosterCount.setText(R.string.roster_count_empty);
        renderStates(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachRoster();
    }

    @Override
    protected void onStop() {
        detachRoster();
        super.onStop();
    }

    private void attachRoster() {
        if (rosterHandle != null || uid == null) {
            return;
        }
        rosterHandle = RosterRepository.observeRoster(uid, new RosterRepository.RosterCallback() {
            @Override
            public void onData(List<RosterChar> data) {
                roster.clear();
                roster.addAll(data);
                ownedById.clear();
                for (RosterChar owned : data) {
                    ownedById.put(owned.charId, owned);
                }
                loading = false;
                loadFailed = false;
                loadedOnce = true;
                ensureSelection();
                applyFilter();
            }

            @Override
            public void onError(DatabaseError error) {
                loading = false;
                loadFailed = true;
                if (roster.isEmpty()) {
                    binding.tvRosterError.setText(UiErrors.database(RosterActivity.this, error));
                    renderStates(true);
                } else {
                    Snackbar.make(binding.getRoot(),
                            UiErrors.database(RosterActivity.this, error),
                            Snackbar.LENGTH_LONG).show();
                    renderStates(false);
                }
            }
        });
    }

    private void detachRoster() {
        if (rosterHandle != null) {
            rosterHandle.detach();
            rosterHandle = null;
        }
    }

    private void retry() {
        detachRoster();
        loadFailed = false;
        loading = roster.isEmpty();
        renderStates(false);
        attachRoster();
    }

    private void ensureSelection() {
        if (selectedCharId != null && ownedById.containsKey(selectedCharId)) {
            return;
        }
        selectedCharId = null;
        CharacterTemplate best = null;
        for (RosterChar owned : roster) {
            CharacterTemplate template = catalog.find(owned.charId);
            if (template == null) {
                continue;
            }
            if (best == null || compareRoster(template, owned, best, ownedById.get(best.id)) < 0) {
                best = template;
            }
        }
        if (best != null) {
            selectedCharId = best.id;
        }
    }

    private void applyFilter() {
        List<CharacterTemplate> filtered = new ArrayList<>();
        int knownCount = 0;
        for (RosterChar owned : roster) {
            CharacterTemplate template = catalog.find(owned.charId);
            if (template == null) {
                continue;
            }
            knownCount++;
            if (elementFilter == null || elementFilter.equals(template.element)) {
                filtered.add(template);
            }
        }
        filtered.sort((left, right) -> compareRoster(
                left, ownedById.get(left.id), right, ownedById.get(right.id)));

        adapter.submit(filtered, ownedById, selectedCharId);

        if (loadedOnce) {
            binding.tvRosterCount.setText(knownCount == 0
                    ? getString(R.string.roster_count_empty)
                    : getResources().getQuantityString(
                    R.plurals.roster_count, knownCount, knownCount));
        }

        boolean emptyRoster = knownCount == 0;
        boolean filteredEmpty = !emptyRoster && filtered.isEmpty();
        binding.tvEmptyTitle.setText(emptyRoster
                ? R.string.roster_empty_title
                : R.string.roster_filter_empty);
        binding.tvEmptyBody.setVisibility(emptyRoster ? View.VISIBLE : View.GONE);

        renderStates(filteredEmpty);
        bindDetail();
    }

    private int compareRoster(CharacterTemplate left, RosterChar leftOwned,
                              CharacterTemplate right, RosterChar rightOwned) {
        int rarity = Integer.compare(
                GameConstants.rarityRank(right.rarity),
                GameConstants.rarityRank(left.rarity));
        if (rarity != 0) {
            return rarity;
        }
        int levelLeft = leftOwned == null ? 1 : leftOwned.level;
        int levelRight = rightOwned == null ? 1 : rightOwned.level;
        int level = Integer.compare(levelRight, levelLeft);
        if (level != 0) {
            return level;
        }
        return left.name.compareToIgnoreCase(right.name);
    }

    private void renderStates(boolean filteredEmpty) {
        binding.progressRoster.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.rosterEmptyState.setVisibility(
                !loading && !loadFailed && filteredEmpty ? View.VISIBLE : View.GONE);
        binding.rosterErrorState.setVisibility(
                !loading && loadFailed && roster.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void selectCharacter(CharacterTemplate template) {
        selectedCharId = template.id;
        adapter.setSelected(template.id);
        bindDetail();
        binding.detailScroll.scrollTo(0, 0);
    }

    private void bindDetail() {
        CharacterTemplate template = catalog.find(selectedCharId);
        RosterChar owned = ownedById.get(selectedCharId);
        if (template == null) {
            binding.tvDetailPlaceholder.setVisibility(View.VISIBLE);
            binding.detailScroll.setVisibility(View.GONE);
            return;
        }
        binding.tvDetailPlaceholder.setVisibility(View.GONE);
        binding.detailScroll.setVisibility(View.VISIBLE);

        int level = owned == null ? 1 : owned.level;
        int stars = owned == null ? 1 : owned.stars;
        CharacterStats stats = template.statsAt(level, stars);

        binding.vDetailSprite.setCharacter(template.sprite, Texts.initial(template.name));
        binding.tvDetailName.setText(template.name);
        binding.tvDetailTitle.setText(template.title);
        binding.tvDetailElement.setText(getString(GameUi.elementLabel(template.element)));
        binding.tvDetailElement.setTextColor(ContextCompat.getColor(
                this, GameUi.elementColor(template.element)));
        binding.tvDetailRole.setText(getString(GameUi.roleLabel(template.role)));
        binding.tvDetailRarity.setText(getString(GameUi.rarityLabel(template.rarity)));
        binding.tvDetailRarity.setTextColor(ContextCompat.getColor(
                this, GameUi.rarityColor(template.rarity)));
        binding.tvDetailLevel.setText(getString(R.string.roster_detail_level, level, stars));
        binding.tvDetailCp.setText(getString(R.string.roster_detail_cp, Texts.number(stats.cp)));

        int awakening = owned == null ? 0 : owned.awakening;
        int breakthrough = owned == null ? 0 : owned.breakthrough;
        binding.tvDetailProgress.setVisibility(
                awakening > 0 || breakthrough > 0 ? View.VISIBLE : View.GONE);
        binding.tvDetailProgress.setText(
                getString(R.string.roster_detail_progress, awakening, breakthrough));

        bindStats(stats);
        bindSkills(owned);
    }

    private void bindStats(CharacterStats stats) {
        binding.statsGrid.removeAllViews();
        addStatRow(getString(R.string.stat_hp), Texts.number(stats.hp),
                getString(R.string.stat_atk), Texts.number(stats.atk));
        addStatRow(getString(R.string.stat_def), Texts.number(stats.def),
                getString(R.string.stat_spd), Texts.number(stats.spd));
        addStatRow(getString(R.string.stat_crit), GameUi.percent(stats.critRate),
                getString(R.string.stat_crit_dmg), GameUi.percent(stats.critDmg));
        addStatRow(getString(R.string.stat_acc), Texts.number(stats.acc),
                getString(R.string.stat_eva), Texts.number(stats.eva));
        addStatRow(getString(R.string.stat_res), Texts.number(stats.res), null, null);
    }

    private void addStatRow(String labelLeft, String valueLeft,
                            String labelRight, String valueRight) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        row.setLayoutParams(params);
        View leftCell = statCell(labelLeft, valueLeft);
        ((LinearLayout.LayoutParams) leftCell.getLayoutParams()).rightMargin = dp(16);
        row.addView(leftCell);
        if (labelRight != null) {
            row.addView(statCell(labelRight, valueRight));
        } else {
            row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        binding.statsGrid.addView(row);
    }

    private View statCell(String label, String value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);

        cell.addView(labelView);
        cell.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
        cell.addView(valueView);
        return cell;
    }

    private void bindSkills(RosterChar owned) {
        int basic = owned == null ? 1 : owned.skillBasic;
        int one = owned == null ? 1 : owned.skillOne;
        int two = owned == null ? 1 : owned.skillTwo;
        int ultimate = owned == null ? 1 : owned.skillUltimate;
        binding.tvDetailSkills.setText(getString(R.string.roster_skills_block,
                getString(R.string.skill_basic), basic,
                getString(R.string.skill_one), one,
                getString(R.string.skill_two), two,
                getString(R.string.skill_ultimate), ultimate));
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> selectFilter(null));
        binding.chipFire.setOnClickListener(v -> selectFilter("fire"));
        binding.chipWater.setOnClickListener(v -> selectFilter("water"));
        binding.chipWind.setOnClickListener(v -> selectFilter("wind"));
        binding.chipEarth.setOnClickListener(v -> selectFilter("earth"));
        binding.chipLightning.setOnClickListener(v -> selectFilter("lightning"));
    }

    private void selectFilter(String element) {
        if (element == null ? elementFilter == null : element.equals(elementFilter)) {
            return;
        }
        elementFilter = element;
        updateChips();
        applyFilter();
    }

    private void updateChips() {
        binding.chipAll.setSelected(elementFilter == null);
        binding.chipFire.setSelected("fire".equals(elementFilter));
        binding.chipWater.setSelected("water".equals(elementFilter));
        binding.chipWind.setSelected("wind".equals(elementFilter));
        binding.chipEarth.setSelected("earth".equals(elementFilter));
        binding.chipLightning.setSelected("lightning".equals(elementFilter));
    }

    private void openFormation() {
        startActivity(new Intent(this, FormationActivity.class));
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
