package zhaf.media.themyth.data.staticdata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import zhaf.media.themyth.core.Texts;

public final class GachaPool {

    public final String id;
    public final String name;
    public final String nameEn;
    public final String desc;
    public final String descEn;
    public final long costDiamond1;
    public final long costDiamond10;
    public final int costTicket1;
    public final int costTicket10;
    public final int pityEpic;
    public final int pityLegendary;
    public final boolean guaranteeRareIn10;
    public final String rateUpCharId;
    public final Map<String, Integer> rates;

    public GachaPool(String id, String name, String nameEn, String desc, String descEn,
                     long costDiamond1, long costDiamond10, int costTicket1, int costTicket10,
                     int pityEpic, int pityLegendary, boolean guaranteeRareIn10,
                     String rateUpCharId, Map<String, Integer> rates) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
        this.desc = desc;
        this.descEn = descEn;
        this.costDiamond1 = costDiamond1;
        this.costDiamond10 = costDiamond10;
        this.costTicket1 = costTicket1;
        this.costTicket10 = costTicket10;
        this.pityEpic = pityEpic;
        this.pityLegendary = pityLegendary;
        this.guaranteeRareIn10 = guaranteeRareIn10;
        this.rateUpCharId = rateUpCharId == null ? "" : rateUpCharId;
        this.rates = Collections.unmodifiableMap(new HashMap<>(rates));
    }

    public String displayName() {
        return Texts.pick(name, nameEn);
    }

    public String description() {
        return Texts.pick(desc, descEn);
    }

    public int rateOf(String rarity) {
        Integer rate = rarity == null ? null : rates.get(rarity);
        return rate == null ? 0 : rate;
    }

    public int totalRate() {
        int total = 0;
        for (Integer rate : rates.values()) {
            total += rate;
        }
        return total;
    }

    public long diamondCost(int count) {
        return count == 10 ? costDiamond10 : costDiamond1;
    }

    public int ticketCost(int count) {
        return count == 10 ? costTicket10 : costTicket1;
    }

    public static int duplicateShards(String rarity) {
        if (rarity == null) {
            return 10;
        }
        switch (rarity) {
            case "uncommon":
                return 15;
            case "rare":
                return 25;
            case "epic":
                return 40;
            case "legendary":
                return 60;
            case "mythic":
                return 100;
            default:
                return 10;
        }
    }
}
