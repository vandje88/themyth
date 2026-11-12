package zhaf.media.themyth.data.event;

import zhaf.media.themyth.data.meta.MissionLogic;

public final class EventLogic {

    public static final long CLAIM_GRACE_MS = 7L * 24L * 60L * 60L * 1000L;

    private EventLogic() {
    }

    public static boolean active(EventDef event, long now) {
        return event != null && now >= event.startsAt && now <= event.endsAt;
    }

    public static boolean claimOpen(EventDef event, long now) {
        return event != null && now >= event.startsAt && now <= claimDeadline(event);
    }

    public static long claimDeadline(EventDef event) {
        return event == null ? 0L : event.endsAt + CLAIM_GRACE_MS;
    }

    public static EventDef activeEvent(long now) {
        for (EventDef event : EventCatalog.events()) {
            if (active(event, now)) {
                return event;
            }
        }
        return null;
    }

    public static EventDef nextEvent(long now) {
        EventDef next = null;
        for (EventDef event : EventCatalog.events()) {
            if (event.startsAt > now && (next == null || event.startsAt < next.startsAt)) {
                next = event;
            }
        }
        return next;
    }

    public static int addProgress(int current, int amount, int target) {
        return MissionLogic.addProgress(current, amount, target);
    }

    public static boolean canClaim(int progress, int target, boolean claimed, boolean claimOpen) {
        return claimOpen && MissionLogic.canClaim(progress, target, claimed);
    }

    public static boolean canRedeem(int cost, int balance, int redeemed, int limit,
                                    boolean claimOpen) {
        return claimOpen && balance >= Math.max(1, cost)
                && Math.max(0, redeemed) < Math.max(1, limit);
    }
}
