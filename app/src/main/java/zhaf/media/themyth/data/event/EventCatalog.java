package zhaf.media.themyth.data.event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class EventCatalog {

    public static final String EVENT_FESTIVAL = "festival_lestari";
    public static final String EVENT_SHADOW = "pekan_bayangan";

    public static final String MISSION_WIN = "event_win";
    public static final String MISSION_ARENA = "event_arena";
    public static final String MISSION_SUMMON = "event_summon";
    public static final String MISSION_BOSS = "event_boss";
    public static final String MISSION_DONATE = "event_donate";

    private static final List<EventDef> EVENTS = new ArrayList<>();
    private static final List<EventMission> MISSIONS = new ArrayList<>();
    private static final List<EventReward> REWARDS = new ArrayList<>();

    static {
        EVENTS.add(new EventDef(EVENT_FESTIVAL,
                "Festival Lestari", "Heritage Festival",
                "Rayakan warisan mitos Nusantara. Kumpulkan Koin Lestari dari misi event "
                        + "dan tukarkan dengan hadiah sebelum gerbang festival menutup.",
                "Celebrate the archipelago's mythic heritage. Earn Festival Coins from "
                        + "event missions and trade them for rewards before the gate closes.",
                "Koin Lestari", "Festival Coin",
                utc(2026, 11, 9, 0, 0, 0), utc(2026, 11, 30, 23, 59, 59)));
        EVENTS.add(new EventDef(EVENT_SHADOW,
                "Pekan Bayangan", "Shadow Week",
                "Kegelapan menyusup ke dunia mitos. Kumpulkan Koin Bayangan dari misi "
                        + "event sebelum gerbang bayangan menutup.",
                "Darkness seeps into the myth world. Gather Shadow Coins from event "
                        + "missions before the shadow gate closes.",
                "Koin Bayangan", "Shadow Coin",
                utc(2026, 12, 1, 0, 0, 0), utc(2026, 12, 21, 23, 59, 59)));

        MISSIONS.add(new EventMission(MISSION_WIN, 5, 100,
                "Menang 5 Pertarungan", "Win 5 Battles",
                "Menangkan pertarungan apa pun selama event.",
                "Win any battle during the event."));
        MISSIONS.add(new EventMission(MISSION_ARENA, 3, 80,
                "Masuk Arena 3 Kali", "Enter the Arena 3 Times",
                "Bertarung di Arena selama event.",
                "Fight in the Arena during the event."));
        MISSIONS.add(new EventMission(MISSION_SUMMON, 3, 60,
                "Panggil 3 Myth", "Summon 3 Myths",
                "Lakukan summon selama event.",
                "Perform summons during the event."));
        MISSIONS.add(new EventMission(MISSION_BOSS, 2, 90,
                "Serang Guild Boss 2 Kali", "Attack the Guild Boss Twice",
                "Serang guild boss selama event.",
                "Attack the guild boss during the event."));
        MISSIONS.add(new EventMission(MISSION_DONATE, 3, 70,
                "Donasi Guild 3 Kali", "Donate to Your Guild 3 Times",
                "Donasi ke guild selama event.",
                "Donate to your guild during the event."));

        REWARDS.add(new EventReward("ev_diamond", 150, 0, 10, 0, 2));
        REWARDS.add(new EventReward("ev_ticket", 120, 0, 0, 1, 1));
        REWARDS.add(new EventReward("ev_coin", 200, 10000, 0, 0, 1));
    }

    private EventCatalog() {
    }

    private static long utc(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    public static List<EventDef> events() {
        return Collections.unmodifiableList(EVENTS);
    }

    public static List<EventMission> missions() {
        return Collections.unmodifiableList(MISSIONS);
    }

    public static List<EventReward> rewards() {
        return Collections.unmodifiableList(REWARDS);
    }

    public static EventDef findEvent(String id) {
        for (EventDef def : EVENTS) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static EventMission findMission(String id) {
        for (EventMission mission : MISSIONS) {
            if (mission.id.equals(id)) {
                return mission;
            }
        }
        return null;
    }

    public static EventReward findReward(String id) {
        for (EventReward reward : REWARDS) {
            if (reward.id.equals(id)) {
                return reward;
            }
        }
        return null;
    }
}
