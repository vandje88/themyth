package zhaf.media.themyth.ui.common;

public enum SpriteAnimation {
    IDLE("idle.png", 4, 2, 110, true, false),
    RUN("run.png", 4, 2, 80, true, false),
    ATTACK("attack.png", 4, 2, 70, false, false),
    HURT("hurt.png", 4, 1, 70, false, false),
    DEFEND("defend.png", 4, 1, 90, false, false),
    DEAD("dead.png", 4, 2, 110, false, true),
    VICTORY("victory.png", 4, 3, 110, true, false),
    SKILL1("skill1.png", 4, 3, 70, false, false),
    SKILL2("skill2.png", 4, 3, 70, false, false),
    ULTIMA1("ultima1.png", 5, 4, 70, false, false),
    ULTIMA2("ultima2.png", 5, 4, 70, false, false);

    public final String file;
    public final int columns;
    public final int rows;
    public final int frameMs;
    public final boolean loop;
    public final boolean holdLast;

    SpriteAnimation(String file, int columns, int rows, int frameMs,
                    boolean loop, boolean holdLast) {
        this.file = file;
        this.columns = columns;
        this.rows = rows;
        this.frameMs = frameMs;
        this.loop = loop;
        this.holdLast = holdLast;
    }

    public int frames() {
        return columns * rows;
    }
}
