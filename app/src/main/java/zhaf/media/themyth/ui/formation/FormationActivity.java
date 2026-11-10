package zhaf.media.themyth.ui.formation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.PlayerState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.progression.Progression;
import zhaf.media.themyth.data.repository.ProgressionRepository;
import zhaf.media.themyth.data.repository.RosterRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.GameConstants;
import zhaf.media.themyth.databinding.ActivityFormationBinding;
import zhaf.media.themyth.databinding.ViewFormationSlotBinding;
import zhaf.media.themyth.ui.common.GameUi;
import zhaf.media.themyth.ui.login.LoginActivity;
import zhaf.media.themyth.ui.roster.CharacterCardAdapter;

public class FormationActivity extends AppCompatActivity {

    private ActivityFormationBinding binding;
    private CharacterCatalog catalog;
    private EquipmentCatalog equipCatalog;
    private ProgressionRepository progressionRepository;
    private CharacterCardAdapter adapter;
    private ViewFormationSlotBinding[] slotBindings;

    private RosterRepository.Handle formationHandle;
    private ProgressionRepository.Handle rosterHandle;
    private String uid;

    private final Map<String, RosterChar> ownedById = new HashMap<>();
    private final Map<String, EquipmentInstance> equipmentById = new HashMap<>();
    private final String[] slots = {"", "", ""};
    private String strategy = FormationState.STRATEGY_BALANCED;
    private int activeSlot;
    private long totalCp;

    private boolean dirty;
    private boolean saving;
    private boolean rosterLoading = true;
    private boolean rosterFailed;
    private boolean formationLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFormationBinding.inflate(getLayoutInflater());
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
        adapter = new CharacterCardAdapter(this::assignToActiveSlot);
        slotBindings = new ViewFormationSlotBinding[]{binding.slot0, binding.slot1, binding.slot2};

        binding.pickerGrid.setLayoutManager(new GridLayoutManager(this, 3));
        binding.pickerGrid.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> attemptExit());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnFormationRetry.setOnClickListener(v -> retryFormation());
        binding.btnPickerRetry.setOnClickListener(v -> retryRoster());

        binding.chipBalanced.setOnClickListener(
                v -> selectStrategy(FormationState.STRATEGY_BALANCED));
        binding.chipFocusBackline.setOnClickListener(
                v -> selectStrategy(FormationState.STRATEGY_FOCUS_BACKLINE));
        binding.chipProtectAlly.setOnClickListener(
                v -> selectStrategy(FormationState.STRATEGY_PROTECT_ALLY));
        binding.chipBurst.setOnClickListener(
                v -> selectStrategy(FormationState.STRATEGY_BURST));

        for (int i = 0; i < slotBindings.length; i++) {
            final int index = i;
            slotBindings[i].getRoot().setOnClickListener(v -> setActiveSlot(index));
            slotBindings[i].btnSlotRemove.setOnClickListener(v -> clearSlot(index));
            slotBindings[i].tvSlotLabel.setText(getString(GameUi.slotLabel(i)));
        }

        updateStrategyUi();
        renderSlots();
        updateSaveUi();
        renderPickerStates();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptExit();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachFormation();
        attachRoster();
    }

    @Override
    protected void onStop() {
        detachFormation();
        detachRoster();
        super.onStop();
    }

    private void attachFormation() {
        if (formationHandle != null || uid == null) {
            return;
        }
        formationHandle = RosterRepository.observeFormation(uid,
                new RosterRepository.FormationCallback() {
                    @Override
                    public void onData(FormationState formation) {
                        formationLoaded = true;
                        binding.tvFormationStatus.setVisibility(View.GONE);
                        binding.btnFormationRetry.setVisibility(View.GONE);
                        if (dirty || saving) {
                            return;
                        }
                        for (int i = 0; i < GameConstants.SLOT_COUNT; i++) {
                            slots[i] = formation.slots.get(i);
                        }
                        strategy = formation.strategy;
                        updateStrategyUi();
                        renderSlots();
                        updateSaveUi();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        if (!formationLoaded) {
                            binding.tvFormationStatus.setText(R.string.formation_error);
                            binding.tvFormationStatus.setVisibility(View.VISIBLE);
                            binding.btnFormationRetry.setVisibility(View.VISIBLE);
                        } else {
                            Snackbar.make(binding.getRoot(),
                                    UiErrors.database(FormationActivity.this, error),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void detachFormation() {
        if (formationHandle != null) {
            formationHandle.detach();
            formationHandle = null;
        }
    }

    private void retryFormation() {
        detachFormation();
        binding.tvFormationStatus.setVisibility(View.GONE);
        binding.btnFormationRetry.setVisibility(View.GONE);
        attachFormation();
    }

    private void attachRoster() {
        if (rosterHandle != null || uid == null) {
            return;
        }
        rosterLoading = true;
        renderPickerStates();
        rosterHandle = progressionRepository.observe(uid,
                new ProgressionRepository.StateCallback() {
                    @Override
                    public void onData(PlayerState data) {
                        rosterLoading = false;
                        rosterFailed = false;
                        ownedById.clear();
                        ownedById.putAll(data.roster);
                        equipmentById.clear();
                        equipmentById.putAll(data.equipment);
                        renderPicker();
                        renderSlots();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        rosterLoading = false;
                        rosterFailed = true;
                        if (ownedById.isEmpty()) {
                            binding.tvPickerError.setText(
                                    UiErrors.database(FormationActivity.this, error));
                            renderPickerStates();
                        } else {
                            Snackbar.make(binding.getRoot(),
                                    UiErrors.database(FormationActivity.this, error),
                                    Snackbar.LENGTH_LONG).show();
                            renderPickerStates();
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

    private void retryRoster() {
        detachRoster();
        rosterFailed = false;
        rosterLoading = ownedById.isEmpty();
        renderPickerStates();
        attachRoster();
    }

    private void renderPicker() {
        List<CharacterTemplate> templates = new ArrayList<>();
        for (RosterChar owned : ownedById.values()) {
            CharacterTemplate template = catalog.find(owned.charId);
            if (template != null) {
                templates.add(template);
            }
        }
        templates.sort(Comparator
                .comparingInt((CharacterTemplate t) -> GameConstants.rarityRank(t.rarity))
                .reversed()
                .thenComparing(t -> t.name, String.CASE_INSENSITIVE_ORDER));
        adapter.submit(templates, ownedById, slots[activeSlot]);
        renderPickerStates();
    }

    private void renderPickerStates() {
        binding.progressPicker.setVisibility(rosterLoading ? View.VISIBLE : View.GONE);
        binding.tvPickerEmpty.setVisibility(
                !rosterLoading && !rosterFailed && ownedById.isEmpty()
                        ? View.VISIBLE : View.GONE);
        binding.pickerErrorState.setVisibility(
                !rosterLoading && rosterFailed && ownedById.isEmpty()
                        ? View.VISIBLE : View.GONE);
    }

    private void setActiveSlot(int index) {
        if (activeSlot == index) {
            return;
        }
        activeSlot = index;
        renderSlots();
    }

    private void clearSlot(int index) {
        if (slots[index].isEmpty()) {
            return;
        }
        slots[index] = "";
        dirty = true;
        renderSlots();
        updateSaveUi();
    }

    private void assignToActiveSlot(CharacterTemplate template) {
        String charId = template.id;
        if (charId.equals(slots[activeSlot])) {
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            if (i != activeSlot && charId.equals(slots[i])) {
                slots[i] = "";
            }
        }
        slots[activeSlot] = charId;
        dirty = true;

        for (int offset = 1; offset <= slots.length; offset++) {
            int next = (activeSlot + offset) % slots.length;
            if (slots[next].isEmpty()) {
                activeSlot = next;
                break;
            }
        }
        renderSlots();
        updateSaveUi();
    }

    private void renderSlots() {
        long sum = 0;
        for (int i = 0; i < slotBindings.length; i++) {
            ViewFormationSlotBinding slot = slotBindings[i];
            slot.getRoot().setSelected(i == activeSlot);

            CharacterTemplate template = catalog.find(slots[i]);
            if (template == null) {
                slot.vSlotPortrait.showPlaceholder("+");
                slot.tvSlotName.setText(R.string.formation_slot_empty);
                slot.tvSlotCp.setText("");
                slot.vSlotElement.setVisibility(View.GONE);
                slot.btnSlotRemove.setVisibility(View.GONE);
                continue;
            }

            RosterChar owned = ownedById.get(template.id);
            int level = owned == null ? 1 : owned.level;
            int stars = owned == null ? 1 : owned.stars;
            long charCp = Progression.statsOf(template, owned,
                    equipmentById, equipCatalog).cp;
            sum += charCp;

            slot.vSlotPortrait.setCharacter(template.sprite, Texts.initial(template.name));
            slot.tvSlotName.setText(template.name);
            slot.tvSlotCp.setText(getString(R.string.formation_slot_cp, Texts.number(charCp)));
            slot.vSlotElement.setVisibility(View.VISIBLE);
            slot.vSlotElement.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, GameUi.elementColor(template.element))));
            slot.btnSlotRemove.setVisibility(View.VISIBLE);
        }
        totalCp = sum;
        binding.tvTotalCp.setText(getString(R.string.formation_total_cp, Texts.number(totalCp)));
        renderSynergy();
        adapter.setSelected(slots[activeSlot]);
    }

    private void renderSynergy() {
        String sharedElement = null;
        boolean complete = true;
        for (String charId : slots) {
            CharacterTemplate template = catalog.find(charId);
            if (template == null) {
                complete = false;
                break;
            }
            if (sharedElement == null) {
                sharedElement = template.element;
            } else if (!sharedElement.equals(template.element)) {
                sharedElement = null;
                break;
            }
        }

        if (complete && sharedElement != null) {
            binding.tvSynergy.setText(getString(R.string.formation_synergy,
                    getString(GameUi.elementLabel(sharedElement))));
            binding.tvSynergy.setTextColor(ContextCompat.getColor(this, R.color.cyan));
        } else {
            binding.tvSynergy.setText(R.string.formation_no_synergy);
            binding.tvSynergy.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }
    }

    private void selectStrategy(String value) {
        if (value.equals(strategy)) {
            return;
        }
        strategy = value;
        dirty = true;
        updateStrategyUi();
        updateSaveUi();
    }

    private void updateStrategyUi() {
        binding.chipBalanced.setSelected(FormationState.STRATEGY_BALANCED.equals(strategy));
        binding.chipFocusBackline.setSelected(
                FormationState.STRATEGY_FOCUS_BACKLINE.equals(strategy));
        binding.chipProtectAlly.setSelected(
                FormationState.STRATEGY_PROTECT_ALLY.equals(strategy));
        binding.chipBurst.setSelected(FormationState.STRATEGY_BURST.equals(strategy));
        binding.tvStrategyDesc.setText(GameUi.strategyDescription(strategy));
    }

    private void updateSaveUi() {
        binding.btnSave.setText(saving ? R.string.formation_saving : R.string.action_save);
        binding.btnSave.setEnabled(dirty && !saving);
        binding.btnSave.setAlpha(dirty && !saving ? 1f : 0.45f);

        if (saving) {
            binding.tvFormationSubtitle.setText(R.string.formation_saving);
            binding.tvFormationSubtitle.setTextColor(ContextCompat.getColor(this, R.color.cyan));
        } else if (dirty) {
            binding.tvFormationSubtitle.setText(R.string.formation_dirty_hint);
            binding.tvFormationSubtitle.setTextColor(ContextCompat.getColor(this, R.color.gold));
        } else {
            binding.tvFormationSubtitle.setText(R.string.formation_subtitle);
            binding.tvFormationSubtitle.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }
    }

    private void save() {
        if (saving || !dirty) {
            return;
        }
        saving = true;
        updateSaveUi();

        List<String> payload = new ArrayList<>(GameConstants.SLOT_COUNT);
        payload.add(slots[0]);
        payload.add(slots[1]);
        payload.add(slots[2]);

        RosterRepository.saveFormation(uid, payload, strategy, totalCp,
                new RosterRepository.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        saving = false;
                        dirty = false;
                        updateSaveUi();
                        Snackbar.make(binding.getRoot(), R.string.formation_saved,
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError() {
                        saving = false;
                        updateSaveUi();
                        Snackbar.make(binding.getRoot(), R.string.formation_save_failed,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void attemptExit() {
        if (saving) {
            return;
        }
        if (!dirty) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.formation_discard_title)
                .setMessage(R.string.formation_discard_message)
                .setNegativeButton(R.string.formation_discard_cancel, null)
                .setPositiveButton(R.string.formation_discard_confirm, (dialog, which) -> finish())
                .show();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
