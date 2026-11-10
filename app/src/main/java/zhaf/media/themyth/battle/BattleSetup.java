package zhaf.media.themyth.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.battle.engine.BattleInput;
import zhaf.media.themyth.battle.engine.BattleUnit;
import zhaf.media.themyth.battle.engine.Strategy;
import zhaf.media.themyth.data.model.FormationState;
import zhaf.media.themyth.data.model.RosterChar;
import zhaf.media.themyth.data.staticdata.CharacterCatalog;
import zhaf.media.themyth.data.staticdata.CharacterStats;
import zhaf.media.themyth.data.staticdata.CharacterTemplate;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class BattleSetup {

    private static final double TRAINING_ENEMY_MULT = 0.9;
    private static final int SYNERGY_TWO_BP = 400;
    private static final int SYNERGY_THREE_BP = 800;

    private BattleSetup() {
    }

    public static BattleInput training(CharacterCatalog catalog, FormationState formation,
                                       List<RosterChar> roster, long seed) {
        if (catalog == null || formation == null) {
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

        List<BattleUnit> teamA = new ArrayList<>();
        List<BattleUnit> teamB = new ArrayList<>();
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
            int level = state == null ? 1 : Math.max(1, state.level);
            int stars = state == null ? 1 : Math.max(1, state.stars);

            teamA.add(build(template, level, stars, 0, slot,
                    "A" + slot, template.name, 1.0));
            teamB.add(build(template, level, stars, 1, slot,
                    "B" + slot, "Bayangan " + template.name, TRAINING_ENEMY_MULT));
        }

        if (teamA.isEmpty() || teamB.isEmpty()) {
            return null;
        }
        applySynergy(teamA);
        applySynergy(teamB);

        return new BattleInput(seed, teamA, teamB,
                Strategy.from(formation.strategy), Strategy.BALANCED,
                BattleInput.DEFAULT_MAX_TICKS);
    }

    private static BattleUnit build(CharacterTemplate template, int level, int stars,
                                    int team, int slot, String uid, String name, double mult) {
        CharacterStats stats = template.statsAt(level, stars);
        BattleUnit unit = new BattleUnit(
                uid, template.id, name, template.sprite, team, slot, level,
                template.element, template.role,
                scale(stats.hp, mult),
                scale(stats.atk, mult),
                scale(stats.def, mult),
                (int) Math.max(1L, Math.round(stats.spd)),
                stats.critRate,
                stats.critDmg,
                stats.acc,
                stats.eva,
                stats.res,
                stats.elemPower
        );
        SkillFactory.assign(unit);
        return unit;
    }

    private static int scale(double value, double mult) {
        return (int) Math.max(1L, Math.round(value * mult));
    }

    private static void applySynergy(List<BattleUnit> team) {
        Map<String, Integer> counts = new HashMap<>();
        for (BattleUnit unit : team) {
            String element = unit.element == null ? "" : unit.element;
            Integer current = counts.get(element);
            counts.put(element, current == null ? 1 : current + 1);
        }
        for (BattleUnit unit : team) {
            Integer count = counts.get(unit.element == null ? "" : unit.element);
            int total = count == null ? 0 : count;
            if (total >= 3) {
                unit.elemPower += SYNERGY_THREE_BP;
            } else if (total == 2) {
                unit.elemPower += SYNERGY_TWO_BP;
            }
        }
    }
}
