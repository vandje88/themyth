package zhaf.media.themyth.data.social;

import com.google.firebase.database.DataSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.core.Texts;

public final class MailMessage {

    public final String id;
    public final String type;
    public final String title;
    public final String titleEn;
    public final String body;
    public final String bodyEn;
    public final long coin;
    public final long diamond;
    public final Map<String, Integer> items;
    public final long createdAt;
    public final long expiresAt;
    public final boolean read;
    public final boolean claimed;

    public MailMessage(String id, String type, String title, String titleEn,
                       String body, String bodyEn, long coin, long diamond,
                       Map<String, Integer> items, long createdAt, long expiresAt,
                       boolean read, boolean claimed) {
        this.id = id;
        this.type = type == null || type.isEmpty() ? "SYSTEM" : type;
        this.title = title == null ? "" : title;
        this.titleEn = titleEn == null ? "" : titleEn;
        this.body = body == null ? "" : body;
        this.bodyEn = bodyEn == null ? "" : bodyEn;
        this.coin = Math.max(0, coin);
        this.diamond = Math.max(0, diamond);
        this.items = Collections.unmodifiableMap(items == null
                ? Collections.emptyMap() : new HashMap<>(items));
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.read = read;
        this.claimed = claimed;
    }

    public String displayTitle() {
        return Texts.pick(title, titleEn);
    }

    public String displayBody() {
        return Texts.pick(body, bodyEn);
    }

    public boolean hasAttachment() {
        return MailLogic.hasAttachment(coin, diamond, items);
    }

    public boolean canClaim(long now) {
        return MailLogic.canClaim(claimed, expiresAt, hasAttachment(), now);
    }

    public static MailMessage from(DataSnapshot snapshot, String fallbackId) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        Map<String, Integer> items = new HashMap<>();
        DataSnapshot itemNode = snapshot.child("attachments").child("items");
        for (DataSnapshot child : itemNode.getChildren()) {
            String itemId = child.getKey();
            Object value = child.getValue();
            if (itemId != null && value instanceof Number) {
                items.put(itemId, ((Number) value).intValue());
            }
        }
        DataSnapshot attachments = snapshot.child("attachments");
        return new MailMessage(
                text(snapshot.child("id").getValue(String.class), fallbackId),
                text(snapshot.child("type").getValue(String.class), "SYSTEM"),
                text(snapshot.child("title").getValue(String.class), ""),
                text(snapshot.child("titleEn").getValue(String.class), ""),
                text(snapshot.child("body").getValue(String.class), ""),
                text(snapshot.child("bodyEn").getValue(String.class), ""),
                longOf(attachments.child("coin").getValue(), 0),
                longOf(attachments.child("diamond").getValue(), 0),
                items,
                longOf(snapshot.child("createdAt").getValue(), 0),
                longOf(snapshot.child("expiresAt").getValue(), 0),
                Boolean.TRUE.equals(snapshot.child("read").getValue(Boolean.class)),
                Boolean.TRUE.equals(snapshot.child("claimed").getValue(Boolean.class)));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> attachments = new HashMap<>();
        attachments.put("coin", coin);
        attachments.put("diamond", diamond);
        if (!items.isEmpty()) {
            attachments.put("items", new HashMap<>(items));
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("title", title);
        map.put("titleEn", titleEn);
        map.put("body", body);
        map.put("bodyEn", bodyEn);
        map.put("attachments", attachments);
        map.put("createdAt", createdAt);
        map.put("expiresAt", expiresAt);
        map.put("read", read);
        map.put("claimed", claimed);
        return map;
    }

    public MailMessage claimed(boolean claimed) {
        return new MailMessage(id, type, title, titleEn, body, bodyEn, coin, diamond,
                items, createdAt, expiresAt, read, claimed);
    }

    private static String text(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
