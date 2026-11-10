package zhaf.media.themyth.ui.shop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.model.ShopLimit;
import zhaf.media.themyth.data.repository.ShopRepository;
import zhaf.media.themyth.data.staticdata.ItemCatalog;
import zhaf.media.themyth.data.staticdata.ItemTemplate;
import zhaf.media.themyth.data.staticdata.ShopOffer;
import zhaf.media.themyth.databinding.ItemShopOfferBinding;
import zhaf.media.themyth.ui.common.GameUi;

public class ShopOfferAdapter extends RecyclerView.Adapter<ShopOfferAdapter.Holder> {

    public interface Listener {
        void onBuy(ShopOffer offer);
    }

    private final List<ShopOffer> items = new ArrayList<>();
    private final ItemCatalog itemCatalog;
    private final Listener listener;
    private ShopRepository.Snapshot snapshot;
    private boolean busy;

    public ShopOfferAdapter(ItemCatalog itemCatalog, Listener listener) {
        this.itemCatalog = itemCatalog;
        this.listener = listener;
    }

    public void submit(List<ShopOffer> offers, ShopRepository.Snapshot snapshot, boolean busy) {
        items.clear();
        if (offers != null) {
            items.addAll(offers);
        }
        this.snapshot = snapshot;
        this.busy = busy;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShopOfferBinding binding = ItemShopOfferBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), itemCatalog, snapshot, busy, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemShopOfferBinding binding;

        Holder(ItemShopOfferBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ShopOffer offer, ItemCatalog itemCatalog, ShopRepository.Snapshot snapshot,
                  boolean busy, Listener listener) {
            Context context = binding.getRoot().getContext();
            ItemTemplate item = itemCatalog.find(offer.itemId);
            String name;
            String description;
            int nameColor;
            if (offer.isCurrencyItem()) {
                name = context.getString(R.string.shop_item_ticket_summon);
                description = context.getString(R.string.shop_ticket_desc);
                nameColor = ContextCompat.getColor(context, R.color.cyan);
            } else {
                name = item == null ? Texts.pretty(offer.itemId) : item.displayName();
                description = item == null ? "" : item.description();
                nameColor = ContextCompat.getColor(context,
                        item == null ? R.color.text_primary : GameUi.rarityColor(item.rarity));
            }

            binding.tvOfferName.setText(name);
            binding.tvOfferName.setTextColor(nameColor);
            binding.tvOfferQty.setText(context.getString(R.string.shop_qty, offer.qty));
            binding.tvOfferDesc.setText(description);
            binding.tvOfferPrice.setText(Texts.number(offer.price));
            binding.ivPriceIcon.setImageResource(
                    offer.costsDiamond() ? R.drawable.ic_diamond : R.drawable.ic_coin);

            ShopLimit limit = snapshot == null ? null : snapshot.limitOf(offer.id);
            int used = limit == null || limit.isFresh(System.currentTimeMillis())
                    ? 0 : limit.count;
            int remaining = Math.max(0, offer.dailyLimit - used);
            if (remaining <= 0) {
                binding.tvOfferLimit.setText(R.string.shop_sold_out);
                binding.tvOfferLimit.setTextColor(
                        ContextCompat.getColor(context, R.color.danger));
            } else {
                binding.tvOfferLimit.setText(context.getString(
                        R.string.shop_remaining, remaining, offer.dailyLimit));
                binding.tvOfferLimit.setTextColor(
                        ContextCompat.getColor(context, R.color.text_muted));
            }

            long balance = snapshot == null ? 0
                    : (offer.costsDiamond() ? snapshot.diamond : snapshot.coin);
            boolean canBuy = !busy && remaining > 0 && balance >= offer.price;
            binding.btnBuyOffer.setEnabled(canBuy);
            binding.btnBuyOffer.setAlpha(canBuy ? 1f : 0.45f);
            binding.btnBuyOffer.setOnClickListener(v -> listener.onBuy(offer));
        }
    }
}
