package zhaf.media.themyth.data.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import zhaf.media.themyth.data.model.EquipmentInstance;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.progression.Progression;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.EquipmentCatalog;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class ArenaSnapshotFactory {

    private ArenaSnapshotFactory() {
    }

    public static ArenaSnapshot build(String uid, String username, String title,
                                      FormationState formation, List<RosterChar> roster,
                                      Map<String, EquipmentInstance> equipment,
                                      CharacterCatalog catalog, EquipmentCatalog equipCatalog,
                                      int rating, long now) {
        if (uid == null || formation == null || catalog == null) {
            return null;
        }
        Map<String, RosterChar> owned = new HashMap<>();
        if (roster != null) {
            for (RosterChar entry : roster) {
                if (entry != null && entry.charId != null) {
                    owned.put(entry.charId, entry);
                }
            }
        }

        List<ArenaUnitSpec> units = new ArrayList<>();
        long cp = 0L;
        for (int slot = 0; slot < GameConstants.SLOT_COUNT; slot++) {
            String charId = slot < formation.slots.size() ? formation.slots.get(slot) : "";
            if (charId == null || charId.trim().isEmpty()) {
                continue;
            }
            CharacterTemplate template = catalog.find(charId);
            if (template == null) {
                continue;
            }
            RosterChar state = owned.get(charId);
            CharacterStats stats = Progression.statsOf(template, state, equipment, equipCatalog);
            cp += stats.cp;
            units.add(new ArenaUnitSpec(
                    slot, template.id, template.name, template.sprite,
                    template.element, template.role,
                    state == null ? 1 : Math.max(1, state.level),
                    (int) Math.max(1L, Math.round(stats.hp)),
                    (int) Math.max(1L, Math.round(stats.atk)),
                    (int) Math.max(0L, Math.round(stats.def)),
                    (int) Math.max(1L, Math.round(stats.spd)),
                    stats.critRate, stats.critDmg, stats.acc, stats.eva, stats.res,
                    stats.elemPower,
                    state == null ? 1 : state.skillBasic,
                    state == null ? 1 : state.skillOne,
                    state == null ? 1 : state.skillTwo,
                    state == null ? 1 : state.skillUltimate));
        }
        if (units.isEmpty()) {
            return null;
        }

        String name = username == null || username.trim().isEmpty() ? "Pemanggil" : username.trim();
        String avatar = name.isEmpty() ? "P" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        return new ArenaSnapshot(uid, name, avatar, title, cp, rating,
                formation.strategy, units, now);
    }
}
