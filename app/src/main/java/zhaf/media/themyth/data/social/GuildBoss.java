package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.core.Texts;

public final class GuildBoss {

    public final String bossId;
    public final String weekKey;
    public final String name;
    public final String nameEn;
    public final String charId;
    public final String sprite;
    public final String element;
    public final String role;
    public final int level;
    public final int hpMax;
    public final int hpLeft;
    public final long endsAt;
    public final Map<String, Integer> damage;
    public final Map<String, Map<String, Boolean>> attacks;

    public GuildBoss(String bossId, String weekKey, String name, String nameEn,
                     String charId, String sprite, String element, String role, int level,
                     int hpMax, int hpLeft, long endsAt,
                     Map<String, Integer> damage, Map<String, Map<String, Boolean>> attacks) {
        this.bossId = bossId == null ? GuildBossLogic.BOSS_ID : bossId;
        this.weekKey = weekKey == null ? "" : weekKey;
        this.name = name == null ? "Bayangan Gatotkaca" : name;
        this.nameEn = nameEn == null ? "Shadow of Gatotkaca" : nameEn;
        this.charId = charId == null || charId.isEmpty() ? "gatotkaca" : charId;
        this.sprite = sprite == null || sprite.isEmpty() ? "gatotkaca" : sprite;
        this.element = element == null || element.isEmpty() ? "fire" : element;
        this.role = role == null || role.isEmpty() ? "tank" : role;
        this.level = Math.max(1, level);
        this.hpMax = Math.max(1, hpMax);
        this.hpLeft = Math.max(0, Math.min(hpLeft, hpMax));
        this.endsAt = endsAt;
        this.damage = Collections.unmodifiableMap(damage == null
                ? Collections.emptyMap() : new HashMap<>(damage));
        this.attacks = Collections.unmodifiableMap(attacks == null
                ? Collections.emptyMap() : new HashMap<>(attacks));
    }

    public String displayName() {
        return Texts.pick(name, nameEn);
    }

    public boolean defeated() {
        return hpLeft <= 0;
    }

    public int hpPercent() {
        return (int) Math.round(hpLeft * 100.0 / Math.max(1, hpMax));
    }

    public int damageOf(String uid) {
        Integer value = uid == null ? null : damage.get(uid);
        return value == null ? 0 : Math.max(0, value);
    }

    public boolean attackedToday(String uid, long now) {
        return GuildBossLogic.attackedToday(attacks, uid, SocialDay.dayKey(now));
    }

    public static GuildBoss from(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        Map<String, Integer> damage = new HashMap<>();
        for (DataSnapshot child : snapshot.child("damage").getChildren()) {
            if (child.getKey() != null && child.getValue() instanceof Number) {
                damage.put(child.getKey(), ((Number) child.getValue()).intValue());
            }
        }
        Map<String, Map<String, Boolean>> attacks = new HashMap<>();
        for (DataSnapshot user : snapshot.child("attacks").getChildren()) {
            if (user.getKey() == null) {
                continue;
            }
            Map<String, Boolean> days = new HashMap<>();
            for (DataSnapshot day : user.getChildren()) {
                if (day.getKey() != null && Boolean.TRUE.equals(day.getValue(Boolean.class))) {
                    days.put(day.getKey(), true);
                }
            }
            attacks.put(user.getKey(), days);
        }
        int hpMax = intOf(snapshot.child("hpMax").getValue(), 1);
        return new GuildBoss(
                snapshot.child("bossId").getValue(String.class),
                snapshot.child("weekKey").getValue(String.class),
                snapshot.child("name").getValue(String.class),
                snapshot.child("nameEn").getValue(String.class),
                snapshot.child("charId").getValue(String.class),
                snapshot.child("sprite").getValue(String.class),
                snapshot.child("element").getValue(String.class),
                snapshot.child("role").getValue(String.class),
                intOf(snapshot.child("level").getValue(), 60),
                hpMax,
                intOf(snapshot.child("hpLeft").getValue(), hpMax),
                longOf(snapshot.child("endsAt").getValue(), 0),
                damage, attacks);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("bossId", bossId);
        map.put("weekKey", weekKey);
        map.put("name", name);
        map.put("nameEn", nameEn);
        map.put("charId", charId);
        map.put("sprite", sprite);
        map.put("element", element);
        map.put("role", role);
        map.put("level", level);
        map.put("hpMax", hpMax);
        map.put("hpLeft", hpLeft);
        map.put("endsAt", endsAt);
        map.put("damage", damage);
        map.put("attacks", attacks);
        return map;
    }

    public static GuildBoss fresh(int memberCount, long now) {
        int hp = GuildBossLogic.bossHp(memberCount);
        return new GuildBoss(GuildBossLogic.BOSS_ID, SocialDay.weekKey(now),
                "Bayangan Gatotkaca", "Shadow of Gatotkaca",
                "gatotkaca", "gatotkaca", "fire", "tank", 60,
                hp, hp, SocialDay.weekEnd(now),
                new HashMap<>(), new HashMap<>());
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
