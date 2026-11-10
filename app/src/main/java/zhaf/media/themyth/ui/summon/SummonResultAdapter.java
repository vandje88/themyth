package zhaf.media.themyth.ui.summon;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.repository.SummonRepository;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.databinding.ItemSummonResultBinding;
import zhaf.media.themyth.ui.common.GameUi;

public class SummonResultAdapter extends RecyclerView.Adapter<SummonResultAdapter.Holder> {

    private final List<SummonRepository.PullResult> items = new ArrayList<>();
    private final CharacterCatalog catalog;

    public SummonResultAdapter(CharacterCatalog catalog) {
        this.catalog = catalog;
    }

    public void submit(List<SummonRepository.PullResult> results) {
        items.clear();
        if (results != null) {
            items.addAll(results);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSummonResultBinding binding = ItemSummonResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), catalog);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemSummonResultBinding binding;

        Holder(ItemSummonResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SummonRepository.PullResult result, CharacterCatalog catalog) {
            Context context = binding.getRoot().getContext();
            CharacterTemplate template = catalog.find(result.charId);
            String name = template == null ? Texts.pretty(result.charId) : template.name;
            String sprite = template == null ? null : template.sprite;

            int color = ContextCompat.getColor(context, GameUi.rarityColor(result.rarity));
            binding.vResultAccent.setBackgroundTintList(ColorStateList.valueOf(color));
            binding.vResultPortrait.setCharacter(sprite, Texts.initial(name));
            binding.tvResultName.setText(name);
            binding.tvResultRarity.setText(context.getString(GameUi.rarityLabel(result.rarity)));
            binding.tvResultRarity.setTextColor(color);

            if (result.isNew) {
                binding.tvResultBadge.setText(R.string.summon_new_badge);
                binding.tvResultBadge.setTextColor(
                        ContextCompat.getColor(context, R.color.gold));
            } else {
                binding.tvResultBadge.setText(
                        context.getString(R.string.summon_shard_badge, result.shards));
                binding.tvResultBadge.setTextColor(
                        ContextCompat.getColor(context, R.color.text_muted));
            }
        }
    }
}
