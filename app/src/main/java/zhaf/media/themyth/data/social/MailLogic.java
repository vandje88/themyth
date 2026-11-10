package zhaf.media.themyth.data.social;

import java.util.Map;

public final class MailLogic {

    public static final int MAX_MAIL = 100;
    public static final long TTL_MS = 30L * SocialDay.DAY_MS;

    private MailLogic() {
    }

    public static long expiresAt(long createdAt) {
        return createdAt + TTL_MS;
    }

    public static boolean isExpired(long expiresAt, long now) {
        return expiresAt > 0 && now >= expiresAt;
    }

    public static boolean hasAttachment(long coin, long diamond,
                                        Map<String, Integer> items) {
        if (coin > 0 || diamond > 0) {
            return true;
        }
        if (items == null) {
            return false;
        }
        for (Integer qty : items.values()) {
            if (qty != null && qty > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean canClaim(boolean claimed, long expiresAt, boolean hasAttachment,
                                   long now) {
        return !claimed && !isExpired(expiresAt, now) && hasAttachment;
    }

    public static int clampQty(long value) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.min(value, 99999L);
    }

    public static int clampCurrency(long value) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.min(value, 999999999L);
    }
}
