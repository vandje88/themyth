package zhaf.media.themyth.data.meta;

public final class MissionLogic {

    private MissionLogic() {
    }

    public static int addProgress(int current, int amount, int target) {
        int safeTarget = Math.max(1, target);
        int next = Math.max(0, current) + Math.max(0, amount);
        return Math.min(safeTarget, next);
    }

    public static boolean complete(int progress, int target) {
        return progress >= Math.max(1, target);
    }

    public static boolean canClaim(int progress, int target, boolean claimed) {
        return !claimed && complete(progress, target);
    }
}
