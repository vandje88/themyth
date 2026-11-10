package zhaf.media.themyth.data.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import zhaf.media.themyth.core.Texts;

public final class TitleCatalog {

    public static final String DEFAULT_TITLE = "rookie_myth";

    private static final List<TitleDef> ALL = new ArrayList<>();

    static {
        ALL.add(new TitleDef("rookie_myth", "Rookie Myth", "Rookie Myth",
                "Gelar awal setiap pemanggil.", "The starting title of every summoner."));
        ALL.add(new TitleDef("shadow_hunter", "Pemburu Bayangan", "Shadow Hunter",
                "Menangkan 10 pertarungan Arena.", "Win 10 Arena matches."));
        ALL.add(new TitleDef("master_shinobi", "Master Shinobi", "Master Shinobi",
                "Capai rating Arena 1300.", "Reach 1300 Arena rating."));
        ALL.add(new TitleDef("elite_warrior", "Pejuang Elit", "Elite Warrior",
                "Capai level akun 10.", "Reach account level 10."));
        ALL.add(new TitleDef("legendary_myth", "Myth Legendaris", "Legendary Myth",
                "Menangkan 100 pertarungan Arena.", "Win 100 Arena matches."));
        ALL.add(new TitleDef("grandmaster", "Grandmaster", "Grandmaster",
                "Capai rating Arena 1900.", "Reach 1900 Arena rating."));
    }

    private TitleCatalog() {
    }

    public static List<TitleDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static TitleDef find(String id) {
        if (id == null) {
            return null;
        }
        for (TitleDef def : ALL) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static String nameOf(String id) {
        TitleDef def = find(id);
        return def == null ? Texts.pretty(id) : def.name();
    }

    public static boolean unlocked(String id, PlayerStats stats) {
        if (id == null) {
            return false;
        }
        if (stats == null) {
            return DEFAULT_TITLE.equals(id);
        }
        switch (id) {
            case DEFAULT_TITLE:
                return true;
            case "shadow_hunter":
                return stats.arenaWins >= 10;
            case "master_shinobi":
                return stats.rating >= 1300;
            case "elite_warrior":
                return stats.level >= 10;
            case "legendary_myth":
                return stats.arenaWins >= 100;
            case "grandmaster":
                return stats.rating >= 1900;
            default:
                return false;
        }
    }
}
