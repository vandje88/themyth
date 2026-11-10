package zhaf.media.themyth.data.arena;

public final class ArenaResultGuard {

    public static final int MIN_TICKS = 3;
    public static final int MAX_TICKS = 900;

    public enum Verdict {
        OK,
        TICKS_OUT_OF_RANGE,
        HP_OUT_OF_RANGE,
        WINNER_INVALID,
        HP_INCONSISTENT,
        STATUS_NOT_PENDING
    }

    private ArenaResultGuard() {
    }

    public static Verdict validate(String status, int winner, int ticks, int hpA, int hpB) {
        if (status != null && !"PENDING".equals(status) && !"SETTLED".equals(status)) {
            return Verdict.STATUS_NOT_PENDING;
        }
        if (ticks < MIN_TICKS || ticks > MAX_TICKS) {
            return Verdict.TICKS_OUT_OF_RANGE;
        }
        if (hpA < 0 || hpA > 100 || hpB < 0 || hpB > 100) {
            return Verdict.HP_OUT_OF_RANGE;
        }
        if (winner < -1 || winner > 1) {
            return Verdict.WINNER_INVALID;
        }
        if (winner == 0 && hpA < hpB) {
            return Verdict.HP_INCONSISTENT;
        }
        if (winner == 1 && hpB < hpA) {
            return Verdict.HP_INCONSISTENT;
        }
        return Verdict.OK;
    }
}
