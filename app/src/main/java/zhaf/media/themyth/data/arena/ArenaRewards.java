package zhaf.media.themyth.data.arena;

public final class ArenaRewards {

    public static final int WIN_COIN = 500;
    public static final int LOSS_COIN = 100;
    public static final int WIN_POINTS = 20;
    public static final int LOSS_POINTS = 5;
    public static final int DELTA_COIN = 2;

    private ArenaRewards() {
    }

    public static int coin(boolean win) {
        return win ? WIN_COIN : LOSS_COIN;
    }

    public static int points(boolean win, int ratingDelta) {
        int base = win ? WIN_POINTS : LOSS_POINTS;
        return Math.max(0, base + ratingDelta * DELTA_COIN);
    }
}
