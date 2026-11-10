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

public final class CharacterCatalog {

    private static final String TAG = "CharacterCatalog";
    private static final String ASSET = "seed/characters.json";

    private static CharacterCatalog instance;

    private final List<CharacterTemplate> characters;
    private final Map<String, CharacterTemplate> byId;

    private CharacterCatalog(List<CharacterTemplate> characters) {
        this.characters = Collections.unmodifiableList(characters);
        Map<String, CharacterTemplate> map = new HashMap<>();
        for (CharacterTemplate template : characters) {
            map.put(template.id, template);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public static CharacterCatalog get(Context context) {
        if (instance == null) {
            synchronized (CharacterCatalog.class) {
                if (instance == null) {
                    instance = new CharacterCatalog(load(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public List<CharacterTemplate> all() {
        return characters;
    }

    public CharacterTemplate find(String charId) {
        return charId == null ? null : byId.get(charId);
    }

    private static List<CharacterTemplate> load(Context context) {
        List<CharacterTemplate> result = new ArrayList<>();
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONArray array = new JSONObject(builder.toString()).getJSONArray("characters");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new CharacterTemplate(
                        item.getString("id"),
                        item.getString("name"),
                        item.optString("title", ""),
                        item.getString("element"),
                        item.getString("role"),
                        item.getString("rarity"),
                        item.has("sprite") ? item.getString("sprite") : null
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Failed to load " + ASSET, error);
        }
        return result;
    }
}
