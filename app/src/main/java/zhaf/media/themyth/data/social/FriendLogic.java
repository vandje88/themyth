package zhaf.media.themyth.data.social;

public final class FriendLogic {

    public static final int MAX_FRIENDS = 50;
    public static final int GIFT_COIN = 50;

    public static final int OK = 0;
    public static final int SELF = 1;
    public static final int MISSING = 2;
    public static final int ALREADY_FRIEND = 3;
    public static final int PENDING = 4;
    public static final int FULL = 5;
    public static final int INCOMING_PENDING = 6;

    private FriendLogic() {
    }

    public static boolean validUid(String uid) {
        if (uid == null) {
            return false;
        }
        String trimmed = uid.trim();
        return trimmed.length() >= 6 && trimmed.length() <= 128 && !trimmed.contains("/");
    }

    public static int requestCheck(String myUid, String targetUid, boolean targetExists,
                                   boolean alreadyFriend, boolean outgoingPending,
                                   boolean incomingPending, int friendCount) {
        if (!validUid(myUid) || !validUid(targetUid) || myUid.equals(targetUid)) {
            return SELF;
        }
        if (!targetExists) {
            return MISSING;
        }
        if (alreadyFriend) {
            return ALREADY_FRIEND;
        }
        if (incomingPending) {
            return INCOMING_PENDING;
        }
        if (outgoingPending) {
            return PENDING;
        }
        if (friendCount >= MAX_FRIENDS) {
            return FULL;
        }
        return OK;
    }

    public static boolean giftReady(String lastGiftDay, long now) {
        return SocialDay.giftReady(lastGiftDay, now);
    }

    public static String giftKey(String fromUid, String day) {
        return (fromUid == null ? "?" : fromUid) + "_" + (day == null ? "?" : day);
    }
}
