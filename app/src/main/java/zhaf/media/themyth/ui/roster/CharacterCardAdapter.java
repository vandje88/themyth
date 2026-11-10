package zhaf.media.themyth.ui.roster;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.databinding.ItemCharacterCardBinding;
import zhaf.media.themyth.ui.common.GameUi;

public class CharacterCardAdapter extends RecyclerView.Adapter<CharacterCardAdapter.Holder> {

    public interface Listener {
        void onCharacterClick(CharacterTemplate template);
    }

    private final List<CharacterTemplate> items = new ArrayList<>();
    private final Map<String, RosterChar> owned = new HashMap<>();
    private final Listener listener;
    private String selectedId;

    public CharacterCardAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<CharacterTemplate> newItems, Map<String, RosterChar> ownedById,
                       String selectedId) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        owned.clear();
        if (ownedById != null) {
            owned.putAll(ownedById);
        }
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    public void setSelected(String charId) {
        if (charId == null ? selectedId == null : charId.equals(selectedId)) {
            return;
        }
        int previous = indexOf(selectedId);
        selectedId = charId;
        int next = indexOf(selectedId);
        if (previous >= 0) {
            notifyItemChanged(previous);
        }
        if (next >= 0) {
            notifyItemChanged(next);
        }
    }

    private int indexOf(String charId) {
        if (charId == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (charId.equals(items.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id.hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCharacterCardBinding binding = ItemCharacterCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CharacterTemplate template = items.get(position);
        holder.bind(template, owned.get(template.id), template.id.equals(selectedId), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemCharacterCardBinding binding;

        Holder(ItemCharacterCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CharacterTemplate template, RosterChar rosterChar, boolean selected,
                  Listener listener) {
            Context context = binding.getRoot().getContext();
            binding.getRoot().setSelected(selected);

            int level = rosterChar == null ? 1 : rosterChar.level;
            int stars = rosterChar == null ? 1 : rosterChar.stars;

            binding.vAccentBar.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, GameUi.elementColor(template.element))));
            binding.vPortrait.setCharacter(template.sprite, Texts.initial(template.name));
            binding.tvName.setText(template.name);
            binding.tvLevel.setText(context.getString(R.string.roster_card_level, level, stars));
            binding.tvRarity.setText(context.getString(GameUi.rarityLabel(template.rarity)));
            binding.tvRarity.setTextColor(ContextCompat.getColor(
                    context, GameUi.rarityColor(template.rarity)));

            binding.getRoot().setOnClickListener(v -> listener.onCharacterClick(template));
        }
    }
}
