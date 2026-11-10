package zhaf.media.themyth.data.meta;

public final class LoginReward {

    public final int day;
    public final int coin;
    public final int diamond;
    public final int energy;
    public final int tickets;

    public LoginReward(int day, int coin, int diamond, int energy, int tickets) {
        this.day = day;
        this.coin = coin;
        this.diamond = diamond;
        this.energy = energy;
        this.tickets = tickets;
    }

    public boolean isEmpty() {
        return coin <= 0 && diamond <= 0 && energy <= 0 && tickets <= 0;
    }
}
