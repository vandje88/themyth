package zhaf.media.themyth.data.event;

import zhaf.media.themyth.core.Texts;

public final class EventMission {

    public final String id;
    public final int target;
    public final int eventCoin;
    public final String titleId;
    public final String titleEn;
    public final String descriptionId;
    public final String descriptionEn;

    EventMission(String id, int target, int eventCoin, String titleId, String titleEn,
                 String descriptionId, String descriptionEn) {
        this.id = id;
        this.target = Math.max(1, target);
        this.eventCoin = eventCoin;
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
}
