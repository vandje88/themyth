package zhaf.media.themyth.data.social;

import java.util.Locale;
import java.util.Map;

public final class GuildLogic {

    public static final int CREATE_COST_COIN = 200_000;
    public static final int MAX_MEMBERS = 30;
    public static final int DONATION_COST_COIN = 5_000;
    public static final int DONATIONS_PER_DAY = 3;
    public static final int CHAT_LIMIT = 50;
    public static final int MAX_MESSAGE = 200;

    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_OFFICER = "officer";
    public static final String ROLE_MEMBER = "member";

    public static final int JOIN_OK = 0;
    public static final int JOIN_CLOSED = 1;
    public static final int JOIN_FULL = 2;
    public static final int JOIN_LEVEL = 3;
    public static final int JOIN_SAME = 4;

    private GuildLogic() {
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean validName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 20;
    }

    public static boolean validTag(String tag) {
        if (tag == null) {
            return false;
        }
        String trimmed = tag.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 4;
    }

    public static int joinCheck(int level, int minLevel, int memberCount, boolean open,
                                String currentGuildId, String targetGuildId) {
        if (currentGuildId != null && currentGuildId.equals(targetGuildId)) {
            return JOIN_SAME;
        }
        if (!open) {
            return JOIN_CLOSED;
        }
        if (memberCount >= MAX_MEMBERS) {
            return JOIN_FULL;
        }
        if (level < minLevel) {
            return JOIN_LEVEL;
        }
        return JOIN_OK;
    }

    public static int donationsLeft(String day, int used, String today) {
        if (day == null || today == null || !day.equals(today)) {
            return DONATIONS_PER_DAY;
        }
        return Math.max(0, DONATIONS_PER_DAY - Math.max(0, used));
    }

    public static int roleRank(String role) {
        if (ROLE_LEADER.equals(role)) {
            return 2;
        }
        if (ROLE_OFFICER.equals(role)) {
            return 1;
        }
        return 0;
    }

    public static boolean canManage(String actorRole, String targetRole) {
        return roleRank(actorRole) > roleRank(targetRole);
    }

    public static String nextLeader(Map<String, String> roles, String leaderUid) {
        if (roles == null) {
            return null;
        }
        String officer = null;
        String member = null;
        for (Map.Entry<String, String> entry : roles.entrySet()) {
            String uid = entry.getKey();
            if (uid == null || uid.equals(leaderUid)) {
                continue;
            }
            String role = entry.getValue();
            if (ROLE_OFFICER.equals(role) && officer == null) {
                officer = uid;
            } else if (ROLE_MEMBER.equals(role) && member == null) {
                member = uid;
            }
        }
        return officer != null ? officer : member;
    }

    public static String guildId(String nameLower, long now) {
        String base = normalizeName(nameLower).replaceAll("[^a-z0-9]", "");
        if (base.length() > 10) {
            base = base.substring(0, 10);
        }
        if (base.isEmpty()) {
            base = "guild";
        }
        return "g_" + base + "_" + Long.toHexString(now);
    }

    public static int weeklyPoints(int donation) {
        return Math.max(0, donation) / 100;
    }
}
