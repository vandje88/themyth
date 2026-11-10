package zhaf.media.themyth.battle.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleResult {

    public final long seed;
    public final int winner;
    public final int ticks;
    public final int teamHpPctA;
    public final int teamHpPctB;
    public final String mvpUid;
    public final String mvpName;
    public final String hash;
    public final List<BattleEvent> events;
    public final List<BattleUnit> units;

    public BattleResult(long seed, int winner, int ticks, int teamHpPctA, int teamHpPctB,
                        String mvpUid, String mvpName, String hash,
                        List<BattleEvent> events, List<BattleUnit> units) {
        this.seed = seed;
        this.winner = winner;
        this.ticks = ticks;
        this.teamHpPctA = teamHpPctA;
        this.teamHpPctB = teamHpPctB;
        this.mvpUid = mvpUid;
        this.mvpName = mvpName;
        this.hash = hash;
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.units = Collections.unmodifiableList(new ArrayList<>(units));
    }

    public boolean playerWin() {
        return winner == 0;
    }

    public boolean draw() {
        return winner < 0;
    }

    public int durationMs() {
        return ticks * 100;
    }

    public int teamHpPct(int team) {
        return team == 0 ? teamHpPctA : teamHpPctB;
    }

    public List<BattleUnit> team(int team) {
        List<BattleUnit> result = new ArrayList<>();
        for (BattleUnit unit : units) {
            if (unit.team == team) {
                result.add(unit);
            }
        }
        return result;
    }

    public BattleUnit find(String uid) {
        for (BattleUnit unit : units) {
            if (unit.uid.equals(uid)) {
                return unit;
            }
        }
        return null;
    }

    public long damageDealtBy(int team) {
        long total = 0;
        for (BattleUnit unit : units) {
            if (unit.team == team) {
                total += unit.damageDealt;
            }
        }
        return total;
    }

    public long healingDoneBy(int team) {
        long total = 0;
        for (BattleUnit unit : units) {
            if (unit.team == team) {
                total += unit.healingDone;
            }
        }
        return total;
    }

    public String endReason() {
        for (int i = events.size() - 1; i >= 0; i--) {
            BattleEvent event = events.get(i);
            if (event.type == BattleEvent.Type.END && event.detail != null) {
                return event.detail;
            }
        }
        return null;
    }
}
