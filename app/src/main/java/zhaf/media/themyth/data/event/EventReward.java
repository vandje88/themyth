package zhaf.media.themyth.data.event;

public final class EventReward {

    public final String id;
    public final int cost;
    public final int coin;
    public final int diamond;
    public final int tickets;
    public final int limit;

    EventReward(String id, int cost, int coin, int diamond, int tickets, int limit) {
        this.id = id;
        this.cost = Math.max(1, cost);
        this.coin = coin;
        this.diamond = diamond;
        this.tickets = tickets;
        this.limit = Math.max(1, limit);
    }
}
