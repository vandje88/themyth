package zhaf.media.themyth.ui.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.progression.Progression;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.EquipmentTemplate;
import zhaf.media.themyth.data.staticdata.GameConstants;
import zhaf.media.themyth.data.staticdata.ItemCatalog;
import zhaf.media.themyth.data.staticdata.ItemTemplate;
import zhaf.media.themyth.databinding.ActivityInventoryBinding;
import zhaf.media.themyth.ui.common.GameUi;
import zhaf.media.themyth.ui.login.LoginActivity;

public class InventoryActivity extends AppCompatActivity {

    private ActivityInventoryBinding binding;
    private ItemCatalog itemCatalog;
    private EquipmentCatalog equipCatalog;
    private CharacterCatalog charCatalog;
    private ProgressionRepository repository;
    private InventoryGridAdapter adapter;

    private ProgressionRepository.Handle playerHandle;
    private String uid;

    private PlayerState state = new PlayerState(0, 0,
            new HashMap<>(), new HashMap<>(), new HashMap<>());
    private boolean equipmentTab;
    private String selectedItemId;
    private String selectedInstanceId;

    private boolean loading = true;
    private boolean loadFailed;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        itemCatalog = ItemCatalog.get(this);
        equipCatalog = EquipmentCatalog.get(this);
        charCatalog = CharacterCatalog.get(this);
        repository = new ProgressionRepository(this);
        adapter = new InventoryGridAdapter(this::selectEntry);

        binding.inventoryGrid.setLayoutManager(new GridLayoutManager(this, 3));
        binding.inventoryGrid.setAdapter(adapter);
        binding.inventoryGrid.setHasFixedSize(true);

        binding.btnInventoryBack.setOnClickListener(v -> finish());
        binding.btnInventoryRetry.setOnClickListener(v -> retry());
        binding.chipItems.setOnClickListener(v -> switchTab(false));
        binding.chipEquipment.setOnClickListener(v -> switchTab(true));

        binding.tvInventoryCoin.setText(getString(R.string.inventory_coin, Texts.number(0)));
        updateTabs();
        renderStates(true);
        bindDetail();
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
                        loading = false;
                        loadFailed = false;
                        refreshAll();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        loading = false;
                        loadFailed = true;
                        if (state.inventory.isEmpty() && state.equipment.isEmpty()) {
                            binding.tvInventoryError.setText(
                                    UiErrors.database(InventoryActivity.this, error));
                            renderStates(true);
                        } else {
                            snackbar(UiErrors.database(InventoryActivity.this, error));
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
        loading = state.inventory.isEmpty() && state.equipment.isEmpty();
        renderStates(false);
        attachPlayer();
    }

    private void switchTab(boolean equipment) {
        if (equipmentTab == equipment) {
            return;
        }
        equipmentTab = equipment;
        updateTabs();
        refreshAll();
    }

    private void updateTabs() {
        binding.chipItems.setSelected(!equipmentTab);
        binding.chipEquipment.setSelected(equipmentTab);
    }

    private void refreshAll() {
        List<InventoryGridAdapter.Entry> entries = equipmentTab
                ? equipmentEntries() : itemEntries();
        ensureSelection(entries);
        adapter.submit(entries, equipmentTab,
                equipmentTab ? selectedInstanceId : selectedItemId);
        binding.tvInventoryCoin.setText(getString(R.string.inventory_coin,
                Texts.number(state.coin)));
        binding.tvInventorySubtitle.setText(getString(equipmentTab
                        ? R.string.inventory_equipment_count : R.string.inventory_items_count,
                entries.size()));
        renderStates(entries.isEmpty());
        bindDetail();
    }

    private void ensureSelection(List<InventoryGridAdapter.Entry> entries) {
        if (equipmentTab) {
            selectedInstanceId = contains(entries, true, selectedInstanceId)
                    ? selectedInstanceId
                    : entries.isEmpty() ? null : entries.get(0).id;
        } else {
            selectedItemId = contains(entries, false, selectedItemId)
                    ? selectedItemId
                    : entries.isEmpty() ? null : entries.get(0).id;
        }
    }

    private boolean contains(List<InventoryGridAdapter.Entry> entries,
                             boolean equipment, String id) {
        if (id == null) {
            return false;
        }
        for (InventoryGridAdapter.Entry entry : entries) {
            if (entry.equipment == equipment && id.equals(entry.id)) {
                return true;
            }
        }
        return false;
    }

    private List<InventoryGridAdapter.Entry> itemEntries() {
        List<ItemTemplate> owned = new ArrayList<>();
        for (ItemTemplate item : itemCatalog.all()) {
            if (state.itemQty(item.id) > 0) {
                owned.add(item);
            }
        }
        owned.sort((left, right) -> {
            int rarity = Integer.compare(GameConstants.rarityRank(right.rarity),
                    GameConstants.rarityRank(left.rarity));
            if (rarity != 0) {
                return rarity;
            }
            int exp = Integer.compare(right.expValue, left.expValue);
            if (exp != 0) {
                return exp;
            }
            return left.displayName().compareToIgnoreCase(right.displayName());
        });

        List<InventoryGridAdapter.Entry> entries = new ArrayList<>();
        for (ItemTemplate item : owned) {
            entries.add(new InventoryGridAdapter.Entry(
                    item.id,
                    false,
                    item.displayName(),
                    getString(categoryLabel(item.category)) + " · "
                            + getString(GameUi.rarityLabel(item.rarity)),
                    Texts.initial(item.displayName()),
                    "×" + Texts.number(state.itemQty(item.id)),
                    GameUi.rarityColor(item.rarity)));
        }
        return entries;
    }

    private List<InventoryGridAdapter.Entry> equipmentEntries() {
        List<EquipmentInstance> instances = new ArrayList<>();
        for (EquipmentInstance instance : state.equipment.values()) {
            if (equipCatalog.find(instance.templateId) != null) {
                instances.add(instance);
            }
        }
        instances.sort((left, right) -> {
            EquipmentTemplate leftTemplate = equipCatalog.find(left.templateId);
            EquipmentTemplate rightTemplate = equipCatalog.find(right.templateId);
            int rarity = Integer.compare(GameConstants.rarityRank(rightTemplate.rarity),
                    GameConstants.rarityRank(leftTemplate.rarity));
            if (rarity != 0) {
                return rarity;
            }
            int level = Integer.compare(right.level, left.level);
            if (level != 0) {
                return level;
            }
            return leftTemplate.displayName().compareToIgnoreCase(
                    rightTemplate.displayName());
        });

        List<InventoryGridAdapter.Entry> entries = new ArrayList<>();
        for (EquipmentInstance instance : instances) {
            EquipmentTemplate template = equipCatalog.find(instance.templateId);
            entries.add(new InventoryGridAdapter.Entry(
                    instance.instanceId,
                    true,
                    template.displayName(),
                    getString(GameUi.equipSlotLabel(template.slot)) + " · "
                            + getString(GameUi.rarityLabel(template.rarity)),
                    Texts.initial(template.displayName()),
                    getString(R.string.equip_level, instance.level),
                    GameUi.rarityColor(template.rarity)));
        }
        return entries;
    }

    private void renderStates(boolean empty) {
        binding.progressInventory.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.inventoryEmptyState.setVisibility(
                !loading && !loadFailed && empty ? View.VISIBLE : View.GONE);
        binding.inventoryErrorState.setVisibility(
                !loading && loadFailed && empty ? View.VISIBLE : View.GONE);

        if (!equipmentTab) {
            binding.tvInventoryEmptyTitle.setText(R.string.inventory_empty_title);
            binding.tvInventoryEmptyBody.setVisibility(View.VISIBLE);
        } else {
            binding.tvInventoryEmptyTitle.setText(R.string.inventory_equipment_empty);
            binding.tvInventoryEmptyBody.setVisibility(View.GONE);
        }
    }

    private void selectEntry(InventoryGridAdapter.Entry entry) {
        if (entry.equipment) {
            selectedInstanceId = entry.id;
        } else {
            selectedItemId = entry.id;
        }
        adapter.setSelected(entry.equipment, entry.id);
        bindDetail();
        binding.detailScroll.scrollTo(0, 0);
    }

    private void bindDetail() {
        if (equipmentTab) {
            bindEquipmentDetail();
        } else {
            bindItemDetail();
        }
    }

    private void bindItemDetail() {
        ItemTemplate item = itemCatalog.find(selectedItemId);
        if (item == null) {
            showPlaceholder();
            return;
        }
        int qty = state.itemQty(item.id);
        boolean locked = state.inventory.containsKey(item.id)
                && state.inventory.get(item.id).locked;

        showDetail();
        binding.tvDetailBadge.setText(Texts.initial(item.displayName()));
        binding.tvDetailName.setText(item.displayName());
        binding.tvDetailCategory.setText(getString(categoryLabel(item.category)));
        binding.tvDetailRarity.setText(getString(GameUi.rarityLabel(item.rarity)));
        binding.tvDetailRarity.setTextColor(ContextCompat.getColor(
                this, GameUi.rarityColor(item.rarity)));
        binding.tvDetailQty.setText(getString(R.string.inventory_qty, qty));
        binding.tvDetailDesc.setVisibility(View.VISIBLE);
        binding.tvDetailDesc.setText(item.description());
        binding.tvDetailMain.setText(item.isExpMaterial()
                ? getString(R.string.inventory_exp_value, Texts.number(item.expValue))
                : getString(R.string.inventory_sell_price, Texts.number(item.sellPrice)));
        binding.tvDetailStatus.setVisibility(locked ? View.VISIBLE : View.GONE);
        binding.tvDetailStatus.setText(R.string.inventory_locked);

        boolean canUse = item.isExpMaterial() && !busy && !locked
                && qty > 0 && !state.roster.isEmpty();
        binding.btnUse.setVisibility(item.isExpMaterial() ? View.VISIBLE : View.GONE);
        binding.btnUse.setEnabled(canUse);
        binding.btnUse.setAlpha(canUse ? 1f : 0.45f);
        binding.btnUse.setOnClickListener(v -> pickCharacterForItem(item));

        binding.btnEquip.setVisibility(View.GONE);
        binding.btnEquip.setOnClickListener(null);

        binding.btnEnhance.setVisibility(View.GONE);
        binding.btnEnhance.setOnClickListener(null);

        boolean canSell = !busy && !locked && qty > 0 && item.sellPrice > 0;
        binding.btnSell.setVisibility(item.sellPrice > 0 ? View.VISIBLE : View.GONE);
        binding.btnSell.setEnabled(canSell);
        binding.btnSell.setAlpha(canSell ? 1f : 0.45f);
        binding.btnSell.setOnClickListener(v -> sellItem(item, qty));
    }

    private void bindEquipmentDetail() {
        EquipmentInstance instance = state.equipment.get(selectedInstanceId);
        EquipmentTemplate template = instance == null ? null
                : equipCatalog.find(instance.templateId);
        if (template == null) {
            showPlaceholder();
            return;
        }

        showDetail();
        int value = Progression.enhancedValue(template.mainValue, instance.level);
        binding.tvDetailBadge.setText(Texts.initial(template.displayName()));
        binding.tvDetailName.setText(template.displayName());
        binding.tvDetailCategory.setText(getString(GameUi.equipSlotLabel(template.slot)));
        binding.tvDetailRarity.setText(getString(GameUi.rarityLabel(template.rarity)));
        binding.tvDetailRarity.setTextColor(ContextCompat.getColor(
                this, GameUi.rarityColor(template.rarity)));
        binding.tvDetailQty.setText(getString(R.string.equip_level, instance.level));
        binding.tvDetailDesc.setVisibility(View.GONE);
        binding.tvDetailMain.setText(getString(R.string.equip_main_stat,
                getString(GameUi.statLabel(template.mainKey)), Texts.number(value)));
        binding.tvDetailStatus.setVisibility(View.VISIBLE);
        binding.tvDetailStatus.setText(instance.isEquipped()
                ? getString(R.string.inventory_equipped_by,
                characterName(instance.equippedBy))
                : getString(R.string.inventory_not_equipped));

        binding.btnUse.setVisibility(View.GONE);
        binding.btnUse.setOnClickListener(null);

        boolean canEquip = !busy && !instance.locked;
        binding.btnEquip.setVisibility(View.VISIBLE);
        binding.btnEquip.setText(instance.isEquipped()
                ? R.string.inventory_unequip : R.string.inventory_equip);
        binding.btnEquip.setEnabled(canEquip);
        binding.btnEquip.setAlpha(canEquip ? 1f : 0.45f);
        binding.btnEquip.setOnClickListener(v -> onEquipAction(instance, template));

        boolean canEnhance = !busy && instance.level < Progression.MAX_ENHANCE_LEVEL;
        binding.btnEnhance.setVisibility(View.VISIBLE);
        binding.btnEnhance.setEnabled(canEnhance);
        binding.btnEnhance.setAlpha(canEnhance ? 1f : 0.45f);
        binding.btnEnhance.setOnClickListener(v -> confirmEnhance(instance, template));

        boolean canSell = !busy && !instance.locked && !instance.isEquipped();
        binding.btnSell.setVisibility(View.VISIBLE);
        binding.btnSell.setEnabled(canSell);
        binding.btnSell.setAlpha(canSell ? 1f : 0.45f);
        binding.btnSell.setOnClickListener(v -> confirmSellEquipment(template));
    }

    private void showPlaceholder() {
        binding.tvDetailPlaceholder.setVisibility(View.VISIBLE);
        binding.detailHeader.setVisibility(View.GONE);
        binding.detailScroll.setVisibility(View.GONE);
    }

    private void showDetail() {
        binding.tvDetailPlaceholder.setVisibility(View.GONE);
        binding.detailHeader.setVisibility(View.VISIBLE);
        binding.detailScroll.setVisibility(View.VISIBLE);
    }

    private void pickCharacterForItem(ItemTemplate item) {
        if (busy) {
            return;
        }
        List<RosterChar> options = sortedRoster();
        if (options.isEmpty()) {
            snackbar(getString(R.string.inventory_no_char));
            return;
        }
        List<String> labels = new ArrayList<>();
        for (RosterChar owned : options) {
            CharacterTemplate template = charCatalog.find(owned.charId);
            labels.add(template == null ? owned.charId : template.name);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.inventory_pick_char)
                .setItems(labels.toArray(new String[0]), (dialog, which) ->
                        confirmUseItem(item, options.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmUseItem(ItemTemplate item, RosterChar owned) {
        CharacterTemplate template = charCatalog.find(owned.charId);
        String charName = template == null ? owned.charId : template.name;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_use_title, item.displayName()))
                .setMessage(getString(R.string.inventory_use_message,
                        item.displayName(), charName, state.itemQty(item.id)))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.inventory_use, (dialog, which) ->
                        useItem(item, owned, charName))
                .show();
    }

    private void useItem(ItemTemplate item, RosterChar owned, String charName) {
        if (busy) {
            return;
        }
        setBusy(true);
        repository.useExpItem(uid, owned.charId, item.id, 1,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.inventory_use_success,
                                item.displayName(), charName));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void onEquipAction(EquipmentInstance instance, EquipmentTemplate template) {
        if (busy) {
            return;
        }
        if (instance.isEquipped()) {
            unequip(instance, template);
            return;
        }
        List<RosterChar> options = sortedRoster();
        if (options.isEmpty()) {
            snackbar(getString(R.string.inventory_no_char));
            return;
        }
        List<String> labels = new ArrayList<>();
        for (RosterChar owned : options) {
            CharacterTemplate character = charCatalog.find(owned.charId);
            labels.add(character == null ? owned.charId : character.name);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_equip_title,
                        template.displayName()))
                .setItems(labels.toArray(new String[0]), (dialog, which) ->
                        confirmEquip(instance, template, options.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmEquip(EquipmentInstance instance, EquipmentTemplate template,
                              RosterChar owned) {
        CharacterTemplate character = charCatalog.find(owned.charId);
        String charName = character == null ? owned.charId : character.name;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_equip_title, template.displayName()))
                .setMessage(getString(R.string.inventory_equip_message, charName))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.inventory_equip, (dialog, which) ->
                        equip(instance, template, owned.charId, charName))
                .show();
    }

    private void equip(EquipmentInstance instance, EquipmentTemplate template,
                       String charId, String charName) {
        if (busy) {
            return;
        }
        setBusy(true);
        repository.equip(uid, charId, instance.instanceId,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.inventory_equip_success,
                                template.displayName(), charName));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void unequip(EquipmentInstance instance, EquipmentTemplate template) {
        if (busy || instance.equippedBy.isEmpty()) {
            return;
        }
        setBusy(true);
        repository.unequip(uid, instance.equippedBy, template.slot,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.roster_unequip_success));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void confirmEnhance(EquipmentInstance instance, EquipmentTemplate template) {
        if (busy) {
            return;
        }
        int next = instance.level + 1;
        if (next > Progression.MAX_ENHANCE_LEVEL) {
            snackbar(getString(R.string.progression_enhance_max));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_enhance_title,
                        template.displayName()))
                .setMessage(getString(R.string.inventory_enhance_message,
                        instance.level,
                        next,
                        Texts.number(Progression.enhanceCoin(next)),
                        Progression.enhanceStones(next),
                        state.itemQty("stone_enhance")))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.inventory_enhance, (dialog, which) ->
                        enhance(instance, template, next))
                .show();
    }

    private void enhance(EquipmentInstance instance, EquipmentTemplate template, int next) {
        if (busy) {
            return;
        }
        setBusy(true);
        repository.enhance(uid, instance.instanceId,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.inventory_enhance_success,
                                template.displayName(), next));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void sellItem(ItemTemplate item, int qty) {
        if (busy || qty <= 0) {
            return;
        }
        long one = (long) item.sellPrice;
        long all = one * qty;
        String sellOne = getString(R.string.inventory_sell_one) + " · +"
                + Texts.number(one);
        String sellAll = getString(R.string.inventory_sell_all) + " · +"
                + Texts.number(all);
        String[] options = qty > 1
                ? new String[]{sellOne, sellAll}
                : new String[]{sellOne};
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_sell_title, item.displayName()))
                .setItems(options, (dialog, which) -> {
                    int count = which == 1 ? qty : 1;
                    long amount = one * count;
                    doSellItem(item, count, amount);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void doSellItem(ItemTemplate item, int count, long amount) {
        if (busy) {
            return;
        }
        setBusy(true);
        repository.sellItem(uid, item.id, count,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.inventory_sell_success,
                                Texts.number(amount)));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void confirmSellEquipment(EquipmentTemplate template) {
        if (busy) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.inventory_sell_title, template.displayName()))
                .setMessage(getString(R.string.inventory_sell_message,
                        Texts.number(template.sellPrice)))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.inventory_sell, (dialog, which) ->
                        sellEquipment(template))
                .show();
    }

    private void sellEquipment(EquipmentTemplate template) {
        if (busy || selectedInstanceId == null) {
            return;
        }
        setBusy(true);
        repository.sellEquipment(uid, selectedInstanceId,
                new ProgressionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setBusy(false);
                        snackbar(getString(R.string.inventory_sell_success,
                                Texts.number(template.sellPrice)));
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setBusy(false);
                        snackbar(getString(messageRes));
                    }
                });
    }

    private void setBusy(boolean value) {
        busy = value;
        bindDetail();
    }

    private List<RosterChar> sortedRoster() {
        List<RosterChar> options = new ArrayList<>(state.roster.values());
        options.sort((left, right) -> {
            CharacterTemplate leftTemplate = charCatalog.find(left.charId);
            CharacterTemplate rightTemplate = charCatalog.find(right.charId);
            String leftName = leftTemplate == null ? left.charId : leftTemplate.name;
            String rightName = rightTemplate == null ? right.charId : rightTemplate.name;
            return leftName.compareToIgnoreCase(rightName);
        });
        return options;
    }

    private String characterName(String charId) {
        CharacterTemplate template = charCatalog.find(charId);
        return template == null ? charId : template.name;
    }

    @StringRes
    private static int categoryLabel(String category) {
        if ("SKILL_SCROLL".equals(category)) {
            return R.string.item_category_skill;
        }
        if ("UPGRADE_MATERIAL".equals(category)) {
            return R.string.item_category_upgrade;
        }
        if ("EVOLUTION_STONE".equals(category)) {
            return R.string.item_category_stone;
        }
        return R.string.item_category_exp;
    }

    private void snackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
