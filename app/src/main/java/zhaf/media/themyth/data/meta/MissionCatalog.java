package zhaf.media.themyth.data.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MissionCatalog {

    public static final String DAILY_WIN = "daily_win";
    public static final String DAILY_ARENA = "daily_arena";
    public static final String DAILY_SUMMON = "daily_summon";
    public static final String DAILY_DONATE = "daily_donate";
    public static final String WEEKLY_WIN = "weekly_win";
    public static final String WEEKLY_ARENA_WIN = "weekly_arena_win";
    public static final String WEEKLY_BOSS = "weekly_boss";

    private static final List<MissionDef> DAILY = new ArrayList<>();
    private static final List<MissionDef> WEEKLY = new ArrayList<>();
    private static final List<MissionDef> ALL = new ArrayList<>();

    static {
        add(new MissionDef(DAILY_WIN, MissionDef.SCOPE_DAILY, 3, 3000, 0, 0, 0,
                "Menang 3 Pertarungan", "Win 3 Battles",
                "Menangkan pertarungan apa pun hari ini.", "Win any battle today."));
        add(new MissionDef(DAILY_ARENA, MissionDef.SCOPE_DAILY, 1, 1500, 5, 0, 0,
                "Masuk Arena", "Enter the Arena",
                "Bertarung satu kali di Arena hari ini.", "Fight once in the Arena today."));
        add(new MissionDef(DAILY_SUMMON, MissionDef.SCOPE_DAILY, 1, 1000, 0, 0, 1,
                "Panggil Myth", "Summon a Myth",
                "Lakukan satu kali summon hari ini.", "Perform one summon today."));
        add(new MissionDef(DAILY_DONATE, MissionDef.SCOPE_DAILY, 1, 2000, 0, 0, 0,
                "Donasi Guild", "Guild Donation",
                "Donasi ke guild satu kali hari ini.", "Donate to your guild once today."));
        add(new MissionDef(WEEKLY_WIN, MissionDef.SCOPE_WEEKLY, 15, 15000, 30, 0, 0,
                "Menang 15 Pertarungan", "Win 15 Battles",
                "Kumpulkan 15 kemenangan minggu ini.", "Collect 15 wins this week."));
        add(new MissionDef(WEEKLY_ARENA_WIN, MissionDef.SCOPE_WEEKLY, 5, 10000, 20, 0, 0,
                "Menang 5 Arena", "Win 5 Arena Matches",
                "Menangkan lima pertarungan Arena minggu ini.", "Win five Arena matches this week."));
        add(new MissionDef(WEEKLY_BOSS, MissionDef.SCOPE_WEEKLY, 3, 8000, 10, 0, 0,
                "Serang Guild Boss", "Attack the Guild Boss",
                "Serang guild boss tiga kali minggu ini.", "Attack the guild boss three times this week."));
    }

    private MissionCatalog() {
    }

    private static void add(MissionDef def) {
        ALL.add(def);
        if (def.daily()) {
            DAILY.add(def);
        } else {
            WEEKLY.add(def);
        }
    }

    public static List<MissionDef> daily() {
        return Collections.unmodifiableList(DAILY);
    }

    public static List<MissionDef> weekly() {
        return Collections.unmodifiableList(WEEKLY);
    }

    public static List<MissionDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static MissionDef find(String id) {
        if (id == null) {
            return null;
        }
        for (MissionDef def : ALL) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }
}
