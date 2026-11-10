package zhaf.media.themyth.data.meta;

import zhaf.media.themyth.core.Texts;

public final class TitleDef {

    public final String id;
    public final String nameId;
    public final String nameEn;
    public final String descriptionId;
    public final String descriptionEn;

    TitleDef(String id, String nameId, String nameEn,
             String descriptionId, String descriptionEn) {
        this.id = id;
        this.nameId = nameId;
        this.nameEn = nameEn;
        this.descriptionId = descriptionId;
        this.descriptionEn = descriptionEn;
    }

    public String name() {
        return Texts.pick(nameId, nameEn);
    }

    public String description() {
        return Texts.pick(descriptionId, descriptionEn);
    }
}
