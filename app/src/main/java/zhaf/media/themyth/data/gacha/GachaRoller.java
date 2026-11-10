package zhaf.media.themyth.data.gacha;

import java.util.Random;

import zhaf.media.themyth.data.staticdata.GachaPool;
import zhaf.media.themyth.data.staticdata.GameConstants;

public final class GachaRoller {

    public static final class Pity {
        public int epic;
        public int legendary;

        public Pity() {
        }

        public Pity(int epic, int legendary) {
            this.epic = Math.max(0, epic);
            this.legendary = Math.max(0, legendary);
        }
    }

    private GachaRoller() {
    }

    public static String roll(GachaPool pool, Random rng, Pity pity) {
        pity.epic++;
        pity.legendary++;
        return applyPity(pool, pity, weighted(pool, rng, 0));
    }

    public static String rollRarePlus(GachaPool pool, Random rng, Pity pity) {
        pity.epic++;
        pity.legendary++;
        return applyPity(pool, pity, weighted(pool, rng, 2));
    }

    public static String applyPity(GachaPool pool, Pity pity, String rarity) {
        int rank = GameConstants.rarityRank(rarity);
        if (pity.legendary >= pool.pityLegendary && rank < rankOf("legendary")) {
            rarity = "legendary";
            rank = rankOf("legendary");
        } else if (pity.epic >= pool.pityEpic && rank < rankOf("epic")) {
            rarity = "epic";
            rank = rankOf("epic");
        }
        if (rank >= GameConstants.rarityRank("legendary")) {
            pity.legendary = 0;
        }
        if (rank >= GameConstants.rarityRank("epic")) {
            pity.epic = 0;
        }
        return rarity;
    }

    static String weighted(GachaPool pool, Random rng, int minRank) {
        int total = 0;
        for (String rarity : GameConstants.RARITY_LADDER) {
            if (GameConstants.rarityRank(rarity) >= minRank) {
                total += pool.rateOf(rarity);
            }
        }
        if (total <= 0) {
            return minRank >= 2 ? "rare" : "common";
        }
        int pick = rng.nextInt(total);
        int cursor = 0;
        for (String rarity : GameConstants.RARITY_LADDER) {
            if (GameConstants.rarityRank(rarity) < minRank) {
                continue;
            }
            cursor += pool.rateOf(rarity);
            if (pick < cursor) {
                return rarity;
            }
        }
        return minRank >= 2 ? "rare" : "common";
    }

    private static int rankOf(String rarity) {
        return GameConstants.rarityRank(rarity);
    }
}
