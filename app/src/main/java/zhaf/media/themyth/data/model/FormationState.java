package zhaf.media.themyth.data.model;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import zhaf.media.themyth.data.staticdata.GameConstants;

public final class FormationState {

    public static final String STRATEGY_BALANCED = "BALANCED";
    public static final String STRATEGY_FOCUS_BACKLINE = "FOCUS_BACKLINE";
    public static final String STRATEGY_PROTECT_ALLY = "PROTECT_ALLY";
    public static final String STRATEGY_BURST = "BURST";

    public final List<String> slots;
    public final String strategy;
    public final long cp;

    public FormationState(List<String> slots, String strategy, long cp) {
        List<String> padded = new ArrayList<>(GameConstants.SLOT_COUNT);
        for (int i = 0; i < GameConstants.SLOT_COUNT; i++) {
            String value = slots != null && i < slots.size() ? slots.get(i) : null;
            padded.add(value == null ? "" : value);
        }
        this.slots = Collections.unmodifiableList(padded);
        this.strategy = strategy == null || strategy.isEmpty()
                ? STRATEGY_BALANCED
                : strategy;
        this.cp = cp;
    }

    public static FormationState empty() {
        return new FormationState(null, STRATEGY_BALANCED, 0);
    }

    public static FormationState from(DataSnapshot snapshot) {
        List<String> slots = new ArrayList<>();
        DataSnapshot slotsSnapshot = snapshot.child("slots");
        for (int i = 0; i < GameConstants.SLOT_COUNT; i++) {
            String value = slotsSnapshot.child(String.valueOf(i)).getValue(String.class);
            slots.add(value == null ? "" : value);
        }
        String strategy = snapshot.child("strategy").getValue(String.class);
        Long cp = snapshot.child("cp").getValue(Long.class);
        return new FormationState(slots, strategy, cp == null ? 0 : cp);
    }
}
