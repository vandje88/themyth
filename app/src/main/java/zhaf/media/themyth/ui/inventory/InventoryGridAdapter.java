package zhaf.media.themyth.ui.inventory;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import zhaf.media.themyth.databinding.ItemInventoryCardBinding;

public class InventoryGridAdapter extends RecyclerView.Adapter<InventoryGridAdapter.Holder> {

    public interface Listener {
        void onEntryClick(Entry entry);
    }

    public static final class Entry {
        public final String id;
        public final boolean equipment;
        public final String title;
        public final String subtitle;
        public final String badge;
        public final String qty;
        @ColorRes
        public final int accent;

        public Entry(String id, boolean equipment, String title, String subtitle,
                     String badge, String qty, @ColorRes int accent) {
            this.id = id;
            this.equipment = equipment;
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
            this.qty = qty;
            this.accent = accent;
        }

        String stableKey() {
            return (equipment ? "e:" : "i:") + id;
        }
    }

    private final List<Entry> items = new ArrayList<>();
    private final Listener listener;
    private String selectedId;
    private boolean selectedEquipment;

    public InventoryGridAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<Entry> entries, boolean equipment, String selectedId) {
        items.clear();
        if (entries != null) {
            items.addAll(entries);
        }
        this.selectedEquipment = equipment;
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    public void setSelected(boolean equipment, String id) {
        if (selectedEquipment == equipment
                && (id == null ? selectedId == null : id.equals(selectedId))) {
            return;
        }
        int previous = indexOf(selectedEquipment, selectedId);
        selectedEquipment = equipment;
        selectedId = id;
        int next = indexOf(selectedEquipment, selectedId);
        if (previous >= 0) {
            notifyItemChanged(previous);
        }
        if (next >= 0) {
            notifyItemChanged(next);
        }
    }

    private int indexOf(boolean equipment, String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            Entry entry = items.get(i);
            if (entry.equipment == equipment && id.equals(entry.id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).stableKey().hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryCardBinding binding = ItemInventoryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Entry entry = items.get(position);
        holder.bind(entry,
                entry.equipment == selectedEquipment && entry.id.equals(selectedId),
                listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemInventoryCardBinding binding;

        Holder(ItemInventoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Entry entry, boolean selected, Listener listener) {
            Context context = binding.getRoot().getContext();
            binding.getRoot().setSelected(selected);
            binding.vAccentBar.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, entry.accent)));
            binding.tvBadge.setText(entry.badge);
            binding.tvTitle.setText(entry.title);
            binding.tvSubtitle.setText(entry.subtitle);
            binding.tvQty.setText(entry.qty);
            binding.getRoot().setOnClickListener(v -> listener.onEntryClick(entry));
        }
    }
}
