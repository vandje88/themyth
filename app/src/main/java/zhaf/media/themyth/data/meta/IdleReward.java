package zhaf.media.themyth.data.meta;

public final class IdleReward {

    public final long minutes;
    public final int coin;
    public final int energy;

    IdleReward(long minutes, int coin, int energy) {
        this.minutes = minutes;
        this.coin = coin;
        this.energy = energy;
    }

    public boolean isEmpty() {
        return coin <= 0 && energy <= 0;
    }
}
