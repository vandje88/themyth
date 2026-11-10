package zhaf.media.themyth.data.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BadgeCatalog {

    public static final String NONE = "none";

    private static final List<BadgeDef> ALL = new ArrayList<>();

    static {
        ALL.add(new BadgeDef("first_win", "Kemenangan Pertama", "First Victory",
                "Menangkan pertarungan Arena pertamamu.",
                "Win your first Arena match."));
        ALL.add(new BadgeDef("myth_master", "Penakluk Myth", "Myth Master",
                "Miliki satu myth ber-rarity Mythic.",
                "Own one Mythic rarity myth."));
        ALL.add(new BadgeDef("legendary_fighter", "Pejuang Legendaris", "Legendary Fighter",
                "Menangkan 50 pertarungan Arena.",
                "Win 50 Arena matches."));
        ALL.add(new BadgeDef("pvp_champion", "Juara PvP", "PvP Champion",
                "Capai rating Arena 1900.",
                "Reach 1900 Arena rating."));
        ALL.add(new BadgeDef("dungeon_explorer", "Penjelajah Dungeon", "Dungeon Explorer",
                "Serang guild boss 10 kali.",
                "Attack the guild boss 10 times."));
        ALL.add(new BadgeDef("collector", "Kolektor", "Collector",
                "Kumpulkan 10 myth di roster.",
                "Collect 10 myths in your roster."));
        ALL.add(new BadgeDef("guild_warrior", "Pejuang Guild", "Guild Warrior",
                "Bergabung dengan sebuah guild.",
                "Join a guild."));
        ALL.add(new BadgeDef("top_ranking", "Puncak Ranking", "Top Ranking",
                "Capai rating Arena 1600.",
                "Reach 1600 Arena rating."));
    }

    private BadgeCatalog() {
    }

    public static List<BadgeDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static BadgeDef find(String id) {
        if (id == null) {
            return null;
        }
        for (BadgeDef def : ALL) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static String nameOf(String id) {
        if (id == null || NONE.equals(id)) {
            return zhaf.media.themyth.core.Texts.pick("Tanpa Badge", "No Badge");
        }
        BadgeDef def = find(id);
        return def == null ? zhaf.media.themyth.core.Texts.pretty(id) : def.name();
    }

    public static boolean unlocked(String id, PlayerStats stats) {
        if (id == null || NONE.equals(id)) {
            return true;
        }
        if (stats == null) {
            return false;
        }
        switch (id) {
            case "first_win":
                return stats.arenaWins >= 1;
            case "myth_master":
                return stats.mythicCount >= 1;
            case "legendary_fighter":
                return stats.arenaWins >= 50;
            case "pvp_champion":
                return stats.rating >= 1900;
            case "dungeon_explorer":
                return stats.bossAttacks >= 10;
            case "collector":
                return stats.rosterSize >= 10;
            case "guild_warrior":
                return stats.inGuild;
            case "top_ranking":
                return stats.rating >= 1600;
            default:
                return false;
        }
    }
}
