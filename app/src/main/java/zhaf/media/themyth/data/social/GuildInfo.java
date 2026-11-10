package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

import zhaf.media.themyth.core.Texts;

public final class GuildInfo {

    public final String gid;
    public final String name;
    public final String nameLower;
    public final String tag;
    public final int level;
    public final String leaderUid;
    public final int memberCount;
    public final int minLevel;
    public final boolean open;
    public final String notice;
    public final String noticeEn;
    public final long bank;
    public final long createdAt;

    public GuildInfo(String gid, String name, String nameLower, String tag, int level,
                     String leaderUid, int memberCount, int minLevel, boolean open,
                     String notice, String noticeEn, long bank, long createdAt) {
        this.gid = gid == null ? "" : gid;
        this.name = name == null ? "" : name;
        this.nameLower = nameLower == null ? GuildLogic.normalizeName(name) : nameLower;
        this.tag = tag == null ? "" : tag;
        this.level = Math.max(1, level);
        this.leaderUid = leaderUid == null ? "" : leaderUid;
        this.memberCount = Math.max(0, memberCount);
        this.minLevel = Math.max(1, minLevel);
        this.open = open;
        this.notice = notice == null ? "" : notice;
        this.noticeEn = noticeEn == null ? "" : noticeEn;
        this.bank = Math.max(0, bank);
        this.createdAt = createdAt;
    }

    public String displayNotice() {
        return Texts.pick(notice, noticeEn);
    }

    public static GuildInfo from(DataSnapshot snapshot, String gid) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return new GuildInfo(
                gid,
                snapshot.child("name").getValue(String.class),
                snapshot.child("nameLower").getValue(String.class),
                snapshot.child("tag").getValue(String.class),
                intOf(snapshot.child("level").getValue(), 1),
                snapshot.child("leaderUid").getValue(String.class),
                intOf(snapshot.child("memberCount").getValue(), 0),
                intOf(snapshot.child("minLevel").getValue(), 1),
                Boolean.TRUE.equals(snapshot.child("open").getValue(Boolean.class)),
                snapshot.child("notice").getValue(String.class),
                snapshot.child("noticeEn").getValue(String.class),
                longOf(snapshot.child("bank").getValue(), 0),
                longOf(snapshot.child("createdAt").getValue(), 0));
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
