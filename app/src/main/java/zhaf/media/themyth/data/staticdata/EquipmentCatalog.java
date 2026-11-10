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

public final class EquipmentCatalog {

    private static final String TAG = "EquipmentCatalog";
    private static final String ASSET = "seed/equipment.json";

    private static EquipmentCatalog instance;

    private final List<EquipmentTemplate> templates;
    private final Map<String, EquipmentTemplate> byId;

    private EquipmentCatalog(List<EquipmentTemplate> templates) {
        this.templates = Collections.unmodifiableList(templates);
        Map<String, EquipmentTemplate> map = new HashMap<>();
        for (EquipmentTemplate template : templates) {
            map.put(template.id, template);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static EquipmentCatalog of(List<EquipmentTemplate> templates) {
        return new EquipmentCatalog(templates);
    }

    public static EquipmentCatalog get(Context context) {
        if (instance == null) {
            synchronized (EquipmentCatalog.class) {
                if (instance == null) {
                    instance = new EquipmentCatalog(load(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public List<EquipmentTemplate> all() {
        return templates;
    }

    public EquipmentTemplate find(String templateId) {
        return templateId == null ? null : byId.get(templateId);
    }

    private static List<EquipmentTemplate> load(Context context) {
        List<EquipmentTemplate> result = new ArrayList<>();
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONObject(builder.toString()).getJSONArray("equipment");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new EquipmentTemplate(
                        item.getString("id"),
                        item.getString("name"),
                        item.optString("nameEn", ""),
                        item.getString("slot"),
                        item.getString("rarity"),
                        item.getString("mainKey"),
                        item.getInt("mainValue"),
                        item.optInt("sellPrice", 0)
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Failed to load " + ASSET, error);
        }
        return result;
    }
}
