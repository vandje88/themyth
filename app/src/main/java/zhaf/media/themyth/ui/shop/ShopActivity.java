package zhaf.media.themyth.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
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
import zhaf.media.themyth.data.model.ShopLimit;
import zhaf.media.themyth.data.repository.ShopRepository;
import zhaf.media.themyth.data.staticdata.ItemCatalog;
import zhaf.media.themyth.data.staticdata.ItemTemplate;
import zhaf.media.themyth.data.staticdata.ShopCatalog;
import zhaf.media.themyth.data.staticdata.ShopOffer;
import zhaf.media.themyth.databinding.ActivityShopBinding;
import zhaf.media.themyth.ui.login.LoginActivity;

public class ShopActivity extends AppCompatActivity {

    private ActivityShopBinding binding;
    private ItemCatalog itemCatalog;
    private ShopOfferAdapter adapter;

    private final List<ShopOffer> offers = new ArrayList<>();
    private ShopRepository.Handle handle;
    private ShopRepository.Snapshot snapshot;

    private String uid;
    private boolean busy;
    private boolean loading = true;
    private boolean loadFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        itemCatalog = ItemCatalog.get(this);
        offers.addAll(ShopCatalog.get(this).all());
        adapter = new ShopOfferAdapter(itemCatalog, this::onBuy);

        binding.shopGrid.setLayoutManager(new GridLayoutManager(this, 2));
        binding.shopGrid.setAdapter(adapter);

        binding.btnShopBack.setOnClickListener(v -> finish());
        binding.btnShopRetry.setOnClickListener(v -> retry());

        if (offers.isEmpty()) {
            loading = false;
            loadFailed = true;
            binding.tvShopError.setText(R.string.shop_error);
        }
        render();
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
        handle = ShopRepository.observe(uid, new ShopRepository.SnapshotCallback() {
            @Override
            public void onData(ShopRepository.Snapshot data) {
                snapshot = data;
                loading = false;
                loadFailed = false;
                render();
            }

            @Override
            public void onError(DatabaseError error) {
                loading = false;
                if (snapshot == null) {
                    loadFailed = true;
                    binding.tvShopError.setText(error == null
                            ? getString(R.string.shop_error)
                            : UiErrors.database(ShopActivity.this, error));
                } else {
                    snackbar(error == null
                            ? getString(R.string.shop_error)
                            : UiErrors.database(ShopActivity.this, error));
                }
                render();
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
        render();
        attach();
    }

    private void render() {
        binding.tvShopCoin.setText(Texts.number(snapshot == null ? 0 : snapshot.coin));
        binding.tvShopDiamond.setText(Texts.number(snapshot == null ? 0 : snapshot.diamond));

        boolean showLoading = loading && snapshot == null;
        binding.progressShop.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        binding.shopErrorState.setVisibility(
                loadFailed && snapshot == null ? View.VISIBLE : View.GONE);
        binding.shopGrid.setVisibility(showLoading ? View.GONE : View.VISIBLE);

        adapter.submit(offers, snapshot, busy);
    }

    private void onBuy(ShopOffer offer) {
        if (offer == null || busy) {
            return;
        }
        long balance = snapshot == null ? 0
                : (offer.costsDiamond() ? snapshot.diamond : snapshot.coin);
        if (balance < offer.price) {
            snackbar(getString(offer.costsDiamond()
                    ? R.string.shop_not_enough_diamond
                    : R.string.shop_not_enough_coin));
            return;
        }

        ShopLimit limit = snapshot == null ? null : snapshot.limitOf(offer.id);
        int used = limit == null || limit.isFresh(System.currentTimeMillis())
                ? 0 : limit.count;
        int remaining = Math.max(0, offer.dailyLimit - used);
        if (remaining <= 0) {
            snackbar(getString(R.string.shop_limit_reached));
            return;
        }

        if (!offer.costsDiamond()) {
            purchase(offer);
            return;
        }
        String priceText = getString(R.string.shop_price_diamond, Texts.number(offer.price));
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.shop_confirm_title, itemName(offer)))
                .setMessage(getString(R.string.shop_confirm_message,
                        itemName(offer), offer.qty, priceText, remaining))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.shop_buy, (dialog, which) -> purchase(offer))
                .show();
    }

    private void purchase(ShopOffer offer) {
        if (busy) {
            return;
        }
        busy = true;
        render();

        ShopRepository.purchase(uid, offer, new ShopRepository.PurchaseCallback() {
            @Override
            public void onSuccess(ShopRepository.PurchaseResult result) {
                busy = false;
                Map<String, ShopLimit> limits = snapshot == null
                        ? new HashMap<>() : new HashMap<>(snapshot.limits);
                limits.put(offer.id, new ShopLimit(result.used, result.resetAt));
                snapshot = new ShopRepository.Snapshot(result.coin, result.diamond, limits);
                render();
                snackbar(getString(R.string.shop_purchase_success,
                        itemName(offer), offer.qty));
            }

            @Override
            public void onError(@StringRes int messageRes) {
                busy = false;
                render();
                snackbar(getString(messageRes));
            }
        });
    }

    private String itemName(ShopOffer offer) {
        if (offer.isCurrencyItem()) {
            return getString(R.string.shop_item_ticket_summon);
        }
        ItemTemplate item = itemCatalog.find(offer.itemId);
        return item == null ? Texts.pretty(offer.itemId) : item.displayName();
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
