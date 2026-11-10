package zhaf.media.themyth.battle.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleInput {

    public static final int DEFAULT_MAX_TICKS = 900;

    public final long seed;
    public final List<BattleUnit> teamA;
    public final List<BattleUnit> teamB;
    public final Strategy strategyA;
    public final Strategy strategyB;
    public final int maxTicks;

    public BattleInput(long seed, List<BattleUnit> teamA, List<BattleUnit> teamB,
                       Strategy strategyA, Strategy strategyB, int maxTicks) {
        this.seed = seed;
        this.teamA = Collections.unmodifiableList(
                new ArrayList<>(teamA == null ? Collections.emptyList() : teamA));
        this.teamB = Collections.unmodifiableList(
                new ArrayList<>(teamB == null ? Collections.emptyList() : teamB));
        this.strategyA = strategyA == null ? Strategy.BALANCED : strategyA;
        this.strategyB = strategyB == null ? Strategy.BALANCED : strategyB;
        this.maxTicks = maxTicks <= 0 ? DEFAULT_MAX_TICKS : maxTicks;
    }

    public Strategy strategyFor(int team) {
        return team == 0 ? strategyA : strategyB;
    }

    public List<BattleUnit> allUnits() {
        List<BattleUnit> units = new ArrayList<>(teamA.size() + teamB.size());
        units.addAll(teamA);
        units.addAll(teamB);
        return units;
    }
}
