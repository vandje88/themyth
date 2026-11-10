package zhaf.media.themyth.data.staticdata;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopCatalog {

    private static final String TAG = "ShopCatalog";
    private static final String ASSET = "seed/shop.json";

    private static ShopCatalog instance;

    private final List<ShopOffer> offers;
    private final Map<String, ShopOffer> byId;

    private ShopCatalog(List<ShopOffer> offers) {
        this.offers = Collections.unmodifiableList(offers);
        Map<String, ShopOffer> map = new HashMap<>();
        for (ShopOffer offer : offers) {
            map.put(offer.id, offer);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static ShopCatalog get(Context context) {
        if (instance == null) {
            synchronized (ShopCatalog.class) {
                if (instance == null) {
                    instance = new ShopCatalog(load(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public List<ShopOffer> all() {
        return offers;
    }

    public ShopOffer find(String offerId) {
        return offerId == null ? null : byId.get(offerId);
    }

    private static List<ShopOffer> load(Context context) {
        List<ShopOffer> result = new ArrayList<>();
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONObject(builder.toString()).getJSONArray("offers");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new ShopOffer(
                        item.getString("id"),
                        item.getString("itemId"),
                        Math.max(1, item.optInt("qty", 1)),
                        item.optString("currency", "coin"),
                        item.optLong("price", 0),
                        Math.max(1, item.optInt("dailyLimit", 1))
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Failed to load " + ASSET, error);
        }
        return result;
    }
}
