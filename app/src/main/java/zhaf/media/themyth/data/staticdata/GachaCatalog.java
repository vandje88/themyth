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

public final class GachaCatalog {

    private static final String TAG = "GachaCatalog";
    private static final String ASSET = "seed/gacha.json";

    private static GachaCatalog instance;

    private final List<GachaPool> pools;
    private final Map<String, GachaPool> byId;

    private GachaCatalog(List<GachaPool> pools) {
        this.pools = Collections.unmodifiableList(pools);
        Map<String, GachaPool> map = new HashMap<>();
        for (GachaPool pool : pools) {
            map.put(pool.id, pool);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static GachaCatalog get(Context context) {
        if (instance == null) {
            synchronized (GachaCatalog.class) {
                if (instance == null) {
                    instance = new GachaCatalog(load(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public List<GachaPool> all() {
        return pools;
    }

    public GachaPool find(String poolId) {
        return poolId == null ? null : byId.get(poolId);
    }

    private static List<GachaPool> load(Context context) {
        List<GachaPool> result = new ArrayList<>();
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONObject(builder.toString()).getJSONArray("pools");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                JSONObject ratesJson = item.getJSONObject("rates");
                Map<String, Integer> rates = new HashMap<>();
                for (String rarity : GameConstants.RARITY_LADDER) {
                    if (ratesJson.has(rarity)) {
                        rates.put(rarity, ratesJson.getInt(rarity));
                    }
                }
                result.add(new GachaPool(
                        item.getString("id"),
                        item.getString("name"),
                        item.optString("nameEn", ""),
                        item.optString("desc", ""),
                        item.optString("descEn", ""),
                        item.optLong("costDiamond1", 300),
                        item.optLong("costDiamond10", 2700),
                        item.optInt("costTicket1", 1),
                        item.optInt("costTicket10", 10),
                        Math.max(1, item.optInt("pityEpic", 20)),
                        Math.max(1, item.optInt("pityLegendary", 80)),
                        item.optBoolean("guaranteeRareIn10", true),
                        item.optString("rateUpCharId", ""),
                        rates
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Failed to load " + ASSET, error);
        }
        return result;
    }
}
