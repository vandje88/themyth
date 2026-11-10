package zhaf.media.themyth.data.arena;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArenaResultGuardTest {

    @Test
    public void winningResultIsAccepted() {
        assertEquals(ArenaResultGuard.Verdict.OK,
                ArenaResultGuard.validate("PENDING", 0, 200, 30, 0));
    }

    @Test
    public void impossibleTickCountIsRejected() {
        assertEquals(ArenaResultGuard.Verdict.TICKS_OUT_OF_RANGE,
                ArenaResultGuard.validate("PENDING", 0, 0, 30, 0));
        assertEquals(ArenaResultGuard.Verdict.TICKS_OUT_OF_RANGE,
                ArenaResultGuard.validate("PENDING", 0, 5000, 30, 0));
    }

    @Test
    public void hpPercentagesMustStayInRange() {
        assertEquals(ArenaResultGuard.Verdict.HP_OUT_OF_RANGE,
                ArenaResultGuard.validate("PENDING", 0, 120, -1, 0));
        assertEquals(ArenaResultGuard.Verdict.HP_OUT_OF_RANGE,
                ArenaResultGuard.validate("PENDING", 0, 120, 30, 140));
    }

    @Test
    public void winnerMustMatchRemainingHp() {
        assertEquals(ArenaResultGuard.Verdict.HP_INCONSISTENT,
                ArenaResultGuard.validate("PENDING", 0, 120, 10, 80));
        assertEquals(ArenaResultGuard.Verdict.HP_INCONSISTENT,
                ArenaResultGuard.validate("PENDING", 1, 120, 80, 10));
    }

    @Test
    public void unknownWinnerIsRejected() {
        assertEquals(ArenaResultGuard.Verdict.WINNER_INVALID,
                ArenaResultGuard.validate("PENDING", 7, 120, 10, 0));
    }

    @Test
    public void closedBattleCannotReceiveAnotherResult() {
        assertEquals(ArenaResultGuard.Verdict.STATUS_NOT_PENDING,
                ArenaResultGuard.validate("ARCHIVED", 0, 120, 10, 0));
    }
}
