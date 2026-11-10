package zhaf.media.themyth.data.meta;

import zhaf.media.themyth.core.Texts;

public final class MissionDef {

    public static final String SCOPE_DAILY = "daily";
    public static final String SCOPE_WEEKLY = "weekly";

    public final String id;
    public final String scope;
    public final int target;
    public final int coin;
    public final int diamond;
    public final int energy;
    public final int tickets;
    public final String titleId;
    public final String titleEn;
    public final String descriptionId;
    public final String descriptionEn;

    MissionDef(String id, String scope, int target, int coin, int diamond, int energy,
               int tickets, String titleId, String titleEn,
               String descriptionId, String descriptionEn) {
        this.id = id;
        this.scope = scope;
        this.target = Math.max(1, target);
        this.coin = coin;
        this.diamond = diamond;
        this.energy = energy;
        this.tickets = tickets;
        this.titleId = titleId;
        this.titleEn = titleEn;
        this.descriptionId = descriptionId;
        this.descriptionEn = descriptionEn;
    }

    public String title() {
        return Texts.pick(titleId, titleEn);
    }

    public String description() {
        return Texts.pick(descriptionId, descriptionEn);
    }

    public boolean daily() {
        return SCOPE_DAILY.equals(scope);
    }
}
