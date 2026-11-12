package zhaf.media.themyth.data.event;

import zhaf.media.themyth.core.Texts;

public final class EventDef {

    public final String id;
    public final String titleId;
    public final String titleEn;
    public final String descriptionId;
    public final String descriptionEn;
    public final String coinNameId;
    public final String coinNameEn;
    public final long startsAt;
    public final long endsAt;

    EventDef(String id, String titleId, String titleEn, String descriptionId,
             String descriptionEn, String coinNameId, String coinNameEn,
             long startsAt, long endsAt) {
        this.id = id;
        this.titleId = titleId;
        this.titleEn = titleEn;
        this.descriptionId = descriptionId;
        this.descriptionEn = descriptionEn;
        this.coinNameId = coinNameId;
        this.coinNameEn = coinNameEn;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public String title() {
        return Texts.pick(titleId, titleEn);
    }

    public String description() {
        return Texts.pick(descriptionId, descriptionEn);
    }

    public String coinName() {
        return Texts.pick(coinNameId, coinNameEn);
    }
}
