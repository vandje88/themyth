package zhaf.media.themyth.ui.roster;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.progression.Progression;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentTemplate;
import zhaf.media.themyth.data.staticdata.GameConstants;
import zhaf.media.themyth.data.staticdata.ItemCatalog;
import zhaf.media.themyth.data.staticdata.ItemTemplate;
import zhaf.media.themyth.databinding.ActivityRosterBinding;
import zhaf.media.themyth.databinding.ViewEquipmentRowBinding;
import zhaf.media.themyth.databinding.ViewSkillRowBinding;
import zhaf.media.themyth.ui.common.GameUi;
import zhaf.media.themyth.ui.formation.FormationActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class RosterActivity extends AppCompatActivity {

    private ActivityRosterBinding binding;
    private CharacterCatalog catalog;
    private EquipmentCatalog equipCatalog;
    private ItemCatalog itemCatalog;
    private CharacterCardAdapter adapter;
    private ProgressionRepository repository;

    private ProgressionRepository.Handle playerHandle;
    private String uid;

    private final List<RosterChar> roster = new ArrayList<>();
    private final Map<String, RosterChar> ownedById = new HashMap<>();
    private PlayerState state = new PlayerState(0, 0,
            new HashMap<>(), new HashMap<>(), new HashMap<>());
    private String elementFilter;
    private String selectedCharId;

    private boolean loading = true;
    private boolean loadFailed;
    private boolean loadedOnce;
    private boolean busy;

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
        equipCatalog = EquipmentCatalog.get(this);
        itemCatalog = ItemCatalog.get(this);
        repository = new ProgressionRepository(this);
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
        binding.tvRosterCoin.setText(getString(R.string.roster_coin, Texts.number(0)));
        renderStates(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachPlayer();
    }

    @Override
    protected void onStop() {
        detachPlayer();
        super.onStop();
    }

    private void attachPlayer() {
        if (playerHandle != null || uid == null) {
            return;
        }
        playerHandle = ProgressionRepository.observe(uid,
                new ProgressionRepository.StateCallback() {
                    @Override
                    public void onData(PlayerState data) {
                        state = data;
                        roster.clear();
                        roster.addAll(data.roster.values());
                        ownedById.clear();
                        ownedById.putAll(data.roster);
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
                            binding.tvRosterError.setText(
                                    UiErrors.database(RosterActivity.this, error));
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

    private void detachPlayer() {
        if (playerHandle != null) {
            playerHandle.detach();
            playerHandle = null;
        }
    }

    private void retry() {
        detachPlayer();
        loadFailed = false;
        loading = roster.isEmpty();
        renderStates(false);
        attachPlayer();
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
        binding.tvRosterCoin.setText(getString(R.string.roster_coin,
                Texts.number(state.coin)));

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
        int breakthrough = owned == null ? 0 : owned.breakthrough;
        long exp = owned == null ? 0 : owned.exp;
        int cap = Progression.levelCap(template.rarity, breakthrough);
        long need = Progression.expToNext(level);
        CharacterStats stats = Progression.statsOf(
                template, owned, state.equipment, equipCatalog);

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

        if (level >= cap) {
            binding.progressExp.setProgress(100);
            binding.tvDetailExp.setText(R.string.roster_level_max);
        } else {
            int pct = (int) Math.min(100, exp * 100 / Math.max(1, need));
            binding.progressExp.setProgress(pct);
            binding.tvDetailExp.setText(getString(R.string.roster_exp_progress,
                    Texts.number(exp), Texts.number(need)));
        }

        int awakening = owned == null ? 0 : owned.awakening;
        binding.tvDetailProgress.setVisibility(
                awakening > 0 || breakthrough > 0 ? View.VISIBLE : View.GONE);
        binding.tvDetailProgress.setText(
                getString(R.string.roster_detail_progress, awakening, breakthrough));

        bindActions(template, owned, level, cap, exp, need);
        bindStats(stats);
        bindSkills(owned);
        bindEquipment(owned);
    }

    private void bindActions(CharacterTemplate template, RosterChar owned,
                             int level, int cap, long exp, long need) {
        boolean canLevel = !busy && owned != null && level < cap
                && exp >= need
                && state.coin >= Progression.levelUpCoin(level);
        binding.btnLevelUp.setEnabled(canLevel);
        binding.btnLevelUp.setAlpha(canLevel ? 1f : 0.45f);
        binding.btnLevelUp.setOnClickListener(v -> confirmLevelUp(template, owned));

        boolean canUseExp = !busy && owned != null && level < cap && hasExpScroll();
        binding.btnUseExp.setEnabled(canUseExp);
        binding.btnUseExp.setAlpha(canUseExp ? 1f : 0.45f);
        binding.btnUseExp.setOnClickListener(v -> pickExpScroll(template, owned));

        int tier = owned == null ? 0 : owned.breakthrough;
        boolean clickable = !busy && owned != null && Progression.canBreakthrough(tier)
                && level >= Progression.breakthroughLevelReq(tier)
                && state.coin >= Progression.breakthroughCoin(tier)
                && state.itemQty(Progression.elementStoneItemId(template.element))
                >= Progression.breakthroughStones(tier);
        if (owned != null && !Progression.canBreakthrough(tier)) {
            binding.btnBreakthrough.setText(R.string.roster_breakthrough_max);
        } else if (owned != null && level < Progression.breakthroughLevelReq(tier)) {
            binding.btnBreakthrough.setText(getString(R.string.roster_breakthrough_req,
                    Progression.breakthroughLevelReq(tier)));
        } else {
            binding.btnBreakthrough.setText(R.string.roster_breakthrough_button);
        }
        binding.btnBreakthrough.setEnabled(clickable);
        binding.btnBreakthrough.setAlpha(clickable ? 1f : 0.45f);
        binding.btnBreakthrough.setOnClickListener(v -> confirmBreakthrough(template, owned));
    }

    private boolean hasExpScroll() {
        for (ItemTemplate item : itemCatalog.all()) {
            if (item.isExpMaterial() && state.itemQty(item.id) > 0) {
                return true;
            }
        }
        return false;
    }

    private void confirmLevelUp(CharacterTemplate template, RosterChar owned) {
        if (owned == null || busy) {
            return;
        }
        int level = owned.level;
        int next = level + 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.roster_level_up_title, template.name))
                .setMessage(getString(R.string.roster_level_up_message,
                        level, next,
                        Texts.number(Progression.levelUpCoin(level)),
                        Texts.number(owned.exp),
                        Texts.number(Progression.expToNext(level))))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.roster_level_up, (dialog, which) ->
                        levelUp(owned.charId, next))
                .show();
    }

    private void levelUp(String charId, int nextLevel) {
        if (busy) {
            return;
        }
        busy = true;
        bindDetail();
        repository.levelUp(uid, charId, new ProgressionRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                busy = false;
                bindDetail();
                snackbar(getString(R.string.roster_level_success, nextLevel));
            }

            @Override
            public void onError(@StringRes int messageRes) {
                busy = false;
                bindDetail();
                snackbar(getString(messageRes));
            }
        });
    }

    private void pickExpScroll(CharacterTemplate template, RosterChar owned) {
        if (owned == null || busy) {
            return;
        }
        List<ItemTemplate> scrolls = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (ItemTemplate item : itemCatalog.all()) {
            int qty = state.itemQty(item.id);
            if (item.isExpMaterial() && qty > 0) {
                scrolls.add(item);
                labels.add(item.displayName() + " ×" + qty);
            }
        }
        if (scrolls.isEmpty()) {
            snackbar(getString(R.string.progression_no_item));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.roster_use_exp_title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    ItemTemplate item = scrolls.get(which);
                    confirmExpScroll(template, owned, item);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmExpScroll(CharacterTemplate template, RosterChar owned,
                                  ItemTemplate item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.roster_use_exp_title))
                .setMessage(getString(R.string.roster_use_exp_message,
                        item.displayName(), template.name, state.itemQty(item.id)))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.inventory_use, (dialog, which) ->
                        useExpScroll(template, owned.charId, item))
                .show();
    }

    private void useExpScroll(CharacterTemplate template, String charId, ItemTemplate item) {
        if (busy) {
            return;
        }
        busy = true;
        bindDetail();
        repository.useExpItem(uid, charId, item.id, 1,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        busy = false;
                        bindDetail();
                        snackbar(getString(R.string.roster_exp_used,
                                Texts.number(item.expValue), template.name));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        busy = false;
                        bindDetail();
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void confirmBreakthrough(CharacterTemplate template, RosterChar owned) {
        if (owned == null || busy || !Progression.canBreakthrough(owned.breakthrough)) {
            return;
        }
        int tier = owned.breakthrough;
        String stoneId = Progression.elementStoneItemId(template.element);
        ItemTemplate stone = itemCatalog.find(stoneId);
        String stoneName = stone == null ? stoneId : stone.displayName();
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.roster_breakthrough_title, template.name))
                .setMessage(getString(R.string.roster_breakthrough_message,
                        tier + 1, tier + 2,
                        Texts.number(Progression.breakthroughCoin(tier)),
                        Progression.breakthroughStones(tier),
                        state.itemQty(stoneId)) + "\n" + stoneName)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.roster_breakthrough_button, (dialog, which) ->
                        breakthrough(owned.charId, tier + 2))
                .show();
    }

    private void breakthrough(String charId, int newTier) {
        if (busy) {
            return;
        }
        busy = true;
        bindDetail();
        repository.breakthrough(uid, charId, new ProgressionRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                busy = false;
                bindDetail();
                snackbar(getString(R.string.roster_breakthrough_success, newTier));
            }

            @Override
            public void onError(@StringRes int messageRes) {
                busy = false;
                bindDetail();
                snackbar(getString(messageRes));
            }
        });
    }

    private void bindSkills(RosterChar owned) {
        binding.skillsGrid.removeAllViews();
        for (String key : Progression.SKILL_KEYS) {
            ViewSkillRowBinding row = ViewSkillRowBinding.inflate(
                    getLayoutInflater(), binding.skillsGrid, false);
            int level = owned == null ? 1 : owned.skillLevel(key);
            row.tvSkillName.setText(getString(GameUi.skillLabel(key)) + " · Lv " + level);

            boolean max = level >= Progression.MAX_SKILL_LEVEL;
            boolean canUpgrade = !max && !busy && owned != null
                    && state.coin >= Progression.skillUpgradeCoin(level)
                    && state.itemQty("scroll_skill") >= Progression.skillUpgradeScrolls(level);
            row.btnSkillUpgrade.setText(max ? R.string.roster_skill_max : R.string.roster_upgrade);
            row.btnSkillUpgrade.setEnabled(canUpgrade);
            row.btnSkillUpgrade.setAlpha(canUpgrade ? 1f : 0.45f);
            row.btnSkillUpgrade.setOnClickListener(v ->
                    confirmSkillUpgrade(owned, key, level));
            binding.skillsGrid.addView(row.getRoot());
        }
    }

    private void confirmSkillUpgrade(RosterChar owned, String key, int level) {
        if (owned == null || busy) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.roster_skill_title,
                        getString(GameUi.skillLabel(key))))
                .setMessage(getString(R.string.roster_skill_message,
                        level, level + 1,
                        Texts.number(Progression.skillUpgradeCoin(level)),
                        Progression.skillUpgradeScrolls(level),
                        state.itemQty("scroll_skill")))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.roster_upgrade, (dialog, which) ->
                        upgradeSkill(owned.charId, key, level + 1))
                .show();
    }

    private void upgradeSkill(String charId, String key, int newLevel) {
        if (busy) {
            return;
        }
        busy = true;
        bindDetail();
        repository.upgradeSkill(uid, charId, key,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        busy = false;
                        bindDetail();
                        snackbar(getString(R.string.roster_skill_success,
                                getString(GameUi.skillLabel(key)), newLevel));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        busy = false;
                        bindDetail();
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void bindEquipment(RosterChar owned) {
        binding.equipmentGrid.removeAllViews();
        for (String slot : EquipmentTemplate.SLOTS) {
            ViewEquipmentRowBinding row = ViewEquipmentRowBinding.inflate(
                    getLayoutInflater(), binding.equipmentGrid, false);
            row.tvEquipSlot.setText(getString(GameUi.equipSlotLabel(slot)));

            EquipmentInstance instance = instanceInSlot(owned, slot);
            EquipmentTemplate equip = instance == null ? null
                    : equipCatalog.find(instance.templateId);
            if (equip == null) {
                row.tvEquipName.setText(R.string.equip_empty);
                row.tvEquipName.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
                row.tvEquipLevel.setText("");
            } else {
                row.tvEquipName.setText(equip.displayName());
                row.tvEquipName.setTextColor(ContextCompat.getColor(
                        this, GameUi.rarityColor(equip.rarity)));
                row.tvEquipLevel.setText(getString(R.string.equip_level, instance.level));
            }
            row.getRoot().setOnClickListener(v -> openEquipPicker(owned, slot));
            binding.equipmentGrid.addView(row.getRoot());
        }
    }

    private EquipmentInstance instanceInSlot(RosterChar owned, String slot) {
        if (owned == null) {
            return null;
        }
        String instanceId = owned.equipmentAt(slot);
        return instanceId == null ? null : state.equipment.get(instanceId);
    }

    private void openEquipPicker(RosterChar owned, String slot) {
        if (owned == null || busy) {
            return;
        }
        List<EquipmentInstance> options = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        EquipmentInstance current = instanceInSlot(owned, slot);
        if (current != null) {
            options.add(null);
            labels.add(getString(R.string.roster_equipment_unequip));
        }
        for (EquipmentInstance instance : state.equipment.values()) {
            EquipmentTemplate template = equipCatalog.find(instance.templateId);
            if (template == null || !slot.equals(template.slot)) {
                continue;
            }
            if (instance.isEquipped() && !instance.equippedBy.equals(owned.charId)) {
                continue;
            }
            options.add(instance);
            labels.add(template.displayName() + " " + getString(R.string.equip_level,
                    instance.level));
        }
        if (labels.isEmpty()) {
            snackbar(getString(R.string.roster_equipment_none));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.roster_equipment_pick,
                        getString(GameUi.equipSlotLabel(slot))))
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    EquipmentInstance picked = options.get(which);
                    if (picked == null) {
                        unequip(owned.charId, slot);
                    } else {
                        equip(owned.charId, picked.instanceId, equipCatalog.find(picked.templateId));
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void equip(String charId, String instanceId, EquipmentTemplate template) {
        if (busy || template == null) {
            return;
        }
        busy = true;
        bindDetail();
        repository.equip(uid, charId, instanceId,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        busy = false;
                        bindDetail();
                        snackbar(getString(R.string.roster_equip_success,
                                template.displayName()));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        busy = false;
                        bindDetail();
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void unequip(String charId, String slot) {
        if (busy) {
            return;
        }
        busy = true;
        bindDetail();
        repository.unequip(uid, charId, slot,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        busy = false;
                        bindDetail();
                        snackbar(getString(R.string.roster_unequip_success));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        busy = false;
                        bindDetail();
                        snackbar(getString(messageRes));
                    }
                });
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
        addStatRow(getString(R.string.stat_res), Texts.number(stats.res),
                getString(R.string.stat_elem_power), Texts.number(stats.elemPower));
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

    private void snackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
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
