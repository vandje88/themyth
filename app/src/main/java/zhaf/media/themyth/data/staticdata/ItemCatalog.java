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

public final class ItemCatalog {

    private static final String TAG = "ItemCatalog";
    private static final String ASSET = "seed/items.json";

    private static ItemCatalog instance;

    private final List<ItemTemplate> items;
    private final Map<String, ItemTemplate> byId;

    private ItemCatalog(List<ItemTemplate> items) {
        this.items = Collections.unmodifiableList(items);
        Map<String, ItemTemplate> map = new HashMap<>();
        for (ItemTemplate item : items) {
            map.put(item.id, item);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static ItemCatalog get(Context context) {
        if (instance == null) {
            synchronized (ItemCatalog.class) {
                if (instance == null) {
                    instance = new ItemCatalog(load(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public List<ItemTemplate> all() {
        return items;
    }

    public ItemTemplate find(String itemId) {
        return itemId == null ? null : byId.get(itemId);
    }

    private static List<ItemTemplate> load(Context context) {
        List<ItemTemplate> result = new ArrayList<>();
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONObject(builder.toString()).getJSONArray("items");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new ItemTemplate(
                        item.getString("id"),
                        item.getString("name"),
                        item.optString("nameEn", ""),
                        item.getString("category"),
                        item.getString("rarity"),
                        item.optInt("stack", 999),
                        item.optInt("expValue", 0),
                        item.optInt("sellPrice", 0),
                        item.optString("desc", ""),
                        item.optString("descEn", "")
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Failed to load " + ASSET, error);
        }
        return result;
    }
}
