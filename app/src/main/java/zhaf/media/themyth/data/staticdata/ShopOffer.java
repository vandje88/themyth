package zhaf.media.themyth.data.staticdata;

public final class ShopOffer {

    public final String id;
    public final String itemId;
    public final int qty;
    public final String currency;
    public final long price;
    public final int dailyLimit;

    public ShopOffer(String id, String itemId, int qty, String currency,
                     long price, int dailyLimit) {
        this.id = id;
        this.itemId = itemId;
        this.qty = qty;
        this.currency = currency;
        this.price = price;
        this.dailyLimit = dailyLimit;
    }

    public boolean costsDiamond() {
        return "diamond".equals(currency);
    }

    public boolean isCurrencyItem() {
        return "ticketSummon".equals(itemId) || "ticketPvp".equals(itemId)
                || "ticketDungeon".equals(itemId) || "energy".equals(itemId);
    }
}
