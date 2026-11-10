package zhaf.media.themyth.data.meta;

public final class PlayerStats {

    public final int level;
    public final int rating;
    public final int arenaWins;
    public final int arenaLosses;
    public final int rosterSize;
    public final int mythicCount;
    public final int bossAttacks;
    public final boolean inGuild;

    public PlayerStats(int level, int rating, int arenaWins, int arenaLosses,
                       int rosterSize, int mythicCount, int bossAttacks, boolean inGuild) {
        this.level = Math.max(1, level);
        this.rating = Math.max(0, rating);
        this.arenaWins = Math.max(0, arenaWins);
        this.arenaLosses = Math.max(0, arenaLosses);
        this.rosterSize = Math.max(0, rosterSize);
        this.mythicCount = Math.max(0, mythicCount);
        this.bossAttacks = Math.max(0, bossAttacks);
        this.inGuild = inGuild;
    }

    public int totalBattles() {
        return arenaWins + arenaLosses;
    }

    public int winRatePercent() {
        int total = totalBattles();
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(arenaWins * 100.0 / total);
    }
}
