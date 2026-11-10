package zhaf.media.themyth.battle.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class BattleEngine {

    public static final int MAX_EFFECT_STACKS = 3;
    public static final int CRIT_CAP_BP = 7500;
    public static final int SHIELD_MAX_HP_BP = 3000;

    private BattleEngine() {
    }

    public static BattleResult simulate(BattleInput input) {
        List<BattleUnit> units = input.allUnits();
        for (BattleUnit unit : units) {
            unit.reset();
        }

        Rng rng = new Rng(input.seed);
        List<BattleEvent> events = new ArrayList<>();
        int tick = 0;
        int actionIndex = 0;
        events.add(BattleEvent.start(tick));

        while (tick < input.maxTicks && !isFinished(units)) {
            for (BattleUnit unit : units) {
                if (unit.alive) {
                    unit.gauge += unit.spd;
                }
            }
            while (true) {
                BattleUnit actor = nextReady(units);
                if (actor == null) {
                    break;
                }
                if (!isFinished(units)) {
                    act(actor, units, input, rng, events, tick, actionIndex);
                    actionIndex++;
                }
                actor.gauge -= BattleUnit.GAUGE_ACTION;
            }
            tick++;
        }

        return buildResult(input, units, events, tick);
    }

    private static boolean isFinished(List<BattleUnit> units) {
        boolean teamA = false;
        boolean teamB = false;
        for (BattleUnit unit : units) {
            if (!unit.alive) {
                continue;
            }
            if (unit.team == 0) {
                teamA = true;
            } else {
                teamB = true;
            }
            if (teamA && teamB) {
                return false;
            }
        }
        return true;
    }

    private static BattleUnit nextReady(List<BattleUnit> units) {
        BattleUnit best = null;
        for (BattleUnit unit : units) {
            if (!unit.alive || unit.gauge < BattleUnit.GAUGE_ACTION) {
                continue;
            }
            if (best == null
                    || unit.gauge > best.gauge
                    || (unit.gauge == best.gauge && unit.team < best.team)
                    || (unit.gauge == best.gauge && unit.team == best.team
                    && unit.slot < best.slot)) {
                best = unit;
            }
        }
        return best;
    }

    private static void act(BattleUnit actor, List<BattleUnit> all, BattleInput input,
                            Rng rng, List<BattleEvent> events, int tick, int actionIndex) {
        actor.energyFromDamageThisTurn = 0;

        if (applyDot(actor, events, tick)) {
            return;
        }
        if (consumeStun(actor, events, tick)) {
            decrementEffects(actor, actionIndex);
            return;
        }
        actor.stunImmune = false;

        Strategy strategy = input.strategyFor(actor.team);
        Skill skill = chooseSkill(actor, strategy, all);
        if (skill == null) {
            decrementEffects(actor, actionIndex);
            return;
        }

        boolean support = skill.isSupport();
        List<BattleUnit> targets = null;
        BattleUnit primary;
        if (support) {
            primary = pickAllyLowest(actor, all);
        } else {
            targets = pickTargets(actor, all, skill, strategy);
            primary = targets.isEmpty() ? null : targets.get(0);
        }

        int energyAfter = skill.type == Skill.Type.ULTIMATE
                ? 0
                : Math.min(BattleUnit.MAX_ENERGY, actor.energy + skill.energyGain);
        events.add(BattleEvent.action(tick, actor, skill, primary, energyAfter));

        if (support) {
            if (primary != null) {
                applySupport(actor, primary, skill, events, tick, actionIndex);
            }
        } else if (targets != null && !targets.isEmpty()) {
            boolean crit = rng.nextBp() < clamp(actor.critRate, 0, CRIT_CAP_BP);
            for (int i = 0; i < targets.size(); i++) {
                BattleUnit target = targets.get(i);
                if (!target.alive) {
                    target = firstLivingEnemy(actor, all, targets);
                    if (target == null) {
                        continue;
                    }
                    targets.set(i, target);
                }
                resolveDamage(actor, target, skill, rng, crit, events, tick);
                if (target.alive) {
                    applyEffects(actor, target, skill, rng, events, tick, actionIndex);
                }
            }
        }

        if (skill.selfEffect != null
                && applyStatus(actor, skill.selfEffect, skill.selfValue, skill.selfTurns,
                actor, actionIndex)) {
            events.add(BattleEvent.status(tick, actor, actor, skill.selfEffect,
                    skill.selfTurns, true));
        }

        actor.energy = Math.min(BattleUnit.MAX_ENERGY, actor.energy + skill.energyGain);
        if (actor.cooldown1 > 0) {
            actor.cooldown1--;
        }
        if (actor.cooldown2 > 0) {
            actor.cooldown2--;
        }
        if (skill.type == Skill.Type.SKILL1) {
            actor.cooldown1 = skill.cooldown;
        } else if (skill.type == Skill.Type.SKILL2) {
            actor.cooldown2 = skill.cooldown;
        }
        if (skill.type == Skill.Type.ULTIMATE) {
            actor.energy = 0;
        }

        decrementEffects(actor, actionIndex);
    }

    private static Skill chooseSkill(BattleUnit actor, Strategy strategy,
                                     List<BattleUnit> all) {
        if (actor.energy >= BattleUnit.MAX_ENERGY && actor.ultimate != null) {
            return actor.ultimate;
        }
        if (strategy == Strategy.PROTECT_ALLY
                && "support".equals(actor.role)
                && actor.skill1 != null
                && actor.cooldown1 == 0
                && actor.skill1.heal
                && hasWoundedAlly(actor, all)) {
            return actor.skill1;
        }
        if (actor.cooldown2 == 0 && actor.skill2 != null) {
            return actor.skill2;
        }
        if (actor.cooldown1 == 0 && actor.skill1 != null) {
            return actor.skill1;
        }
        return actor.basic;
    }

    private static boolean hasWoundedAlly(BattleUnit actor, List<BattleUnit> all) {
        for (BattleUnit unit : all) {
            if (unit.alive && unit.team == actor.team && unit.hpPctBp() < 6000) {
                return true;
            }
        }
        return false;
    }

    private static List<BattleUnit> pickTargets(BattleUnit actor, List<BattleUnit> all,
                                                Skill skill, Strategy strategy) {
        List<BattleUnit> enemies = new ArrayList<>();
        for (BattleUnit unit : all) {
            if (unit.alive && unit.team != actor.team) {
                enemies.add(unit);
            }
        }
        List<BattleUnit> result = new ArrayList<>();
        if (enemies.isEmpty()) {
            return result;
        }
        if (skill.target == Skill.Target.ENEMY_ALL) {
            result.addAll(enemies);
            return result;
        }
        if (skill.target == Skill.Target.ENEMY_ROW) {
            int row = bestRow(enemies);
            for (BattleUnit unit : enemies) {
                if (unit.slot == row) {
                    result.add(unit);
                }
            }
            return result;
        }

        BattleUnit primary = tauntTarget(enemies);
        if (primary == null) {
            primary = pickPrimaryTarget(actor, enemies, strategy);
        }
        if (primary == null) {
            return result;
        }
        result.add(primary);

        if (skill.target == Skill.Target.ENEMY_DUO) {
            List<BattleUnit> rest = new ArrayList<>(enemies);
            rest.remove(primary);
            BattleUnit second = pickPrimaryTarget(actor, rest, strategy);
            if (second != null) {
                result.add(second);
            }
        }
        return result;
    }

    private static BattleUnit tauntTarget(List<BattleUnit> enemies) {
        BattleUnit taunter = null;
        for (BattleUnit unit : enemies) {
            if (unit.tauntTurns > 0
                    && (taunter == null || unit.slot < taunter.slot)) {
                taunter = unit;
            }
        }
        return taunter;
    }

    private static BattleUnit pickPrimaryTarget(BattleUnit actor, List<BattleUnit> enemies,
                                                Strategy strategy) {
        if (enemies.isEmpty()) {
            return null;
        }
        if (strategy == Strategy.FOCUS_BACKLINE) {
            return pickLine(enemies, 2, 1, 0);
        }
        String role = actor.role == null ? "" : actor.role;
        switch (role) {
            case "assassin":
                return pickLine(enemies, 2, 1, 0);
            case "tank":
            case "fighter":
                return pickLine(enemies, 0, 1, 2);
            case "mage": {
                int row = bestRow(enemies);
                return pickLowestHp(inRow(enemies, row));
            }
            default:
                return pickLowestHp(enemies);
        }
    }

    private static BattleUnit pickLine(List<BattleUnit> enemies, int... order) {
        for (int slot : order) {
            List<BattleUnit> line = inRow(enemies, slot);
            if (!line.isEmpty()) {
                return pickLowestHp(line);
            }
        }
        return pickLowestHp(enemies);
    }

    private static List<BattleUnit> inRow(List<BattleUnit> enemies, int slot) {
        List<BattleUnit> line = new ArrayList<>();
        for (BattleUnit unit : enemies) {
            if (unit.slot == slot) {
                line.add(unit);
            }
        }
        return line;
    }

    private static int bestRow(List<BattleUnit> enemies) {
        int bestRow = 0;
        int bestCount = -1;
        for (int slot = 0; slot < 3; slot++) {
            int count = inRow(enemies, slot).size();
            if (count > bestCount) {
                bestCount = count;
                bestRow = slot;
            }
        }
        return bestRow;
    }

    private static BattleUnit pickLowestHp(List<BattleUnit> candidates) {
        BattleUnit best = null;
        for (BattleUnit unit : candidates) {
            if (unit == null || !unit.alive) {
                continue;
            }
            if (best == null) {
                best = unit;
                continue;
            }
            int hp = unit.hpPctBp();
            int bestHp = best.hpPctBp();
            if (hp < bestHp
                    || (hp == bestHp && unit.slot < best.slot)
                    || (hp == bestHp && unit.slot == best.slot
                    && unit.uid.compareTo(best.uid) < 0)) {
                best = unit;
            }
        }
        return best;
    }

    private static BattleUnit pickAllyLowest(BattleUnit actor, List<BattleUnit> all) {
        List<BattleUnit> allies = new ArrayList<>();
        for (BattleUnit unit : all) {
            if (unit.alive && unit.team == actor.team) {
                allies.add(unit);
            }
        }
        return pickLowestHp(allies);
    }

    private static BattleUnit firstLivingEnemy(BattleUnit actor, List<BattleUnit> all,
                                               List<BattleUnit> excluded) {
        for (BattleUnit unit : all) {
            if (unit.alive && unit.team != actor.team && !excluded.contains(unit)) {
                return unit;
            }
        }
        return null;
    }

    private static void resolveDamage(BattleUnit actor, BattleUnit target, Skill skill,
                                      Rng rng, boolean crit, List<BattleEvent> events, int tick) {
        int hitChance = clamp(9800 + actor.acc - target.eva, 6500, 9800);
        if (rng.nextBp() >= hitChance) {
            events.add(BattleEvent.miss(tick, actor, target, skill));
            target.energy = Math.min(BattleUnit.MAX_ENERGY, target.energy + 5);
            return;
        }

        int targetDef = target.effectiveDef();
        long raw = (long) actor.effectiveAtk() * skill.power / 10000L;
        raw = raw * skill.scaleBp / 10000L;
        long mitigationBp = (long) targetDef * 10000L
                / (targetDef + 400L + 20L * actor.level);
        mitigationBp = Math.min(mitigationBp, 9500L);

        long damage = raw * (10000L - mitigationBp) / 10000L;
        long floor = raw * 500L / 10000L;
        if (damage < floor) {
            damage = floor;
        }
        damage = damage * ElementMatrix.multiplierBp(actor.element, target.element) / 10000L;
        damage = damage * (10000L + actor.elemPower) / 10000L;
        damage = damage * target.damageTakenBp() / 10000L;
        if (crit) {
            damage = damage * actor.critDmg / 10000L;
        }
        int variance = 9500 + rng.nextInt(1001);
        damage = damage * variance / 10000L;

        int finalDamage = (int) Math.max(1L, Math.min(damage, (long) Integer.MAX_VALUE));
        applyDamage(actor, target, skill, finalDamage, crit, events, tick);
    }

    private static void applyDamage(BattleUnit actor, BattleUnit target, Skill skill,
                                    int damage, boolean crit, List<BattleEvent> events, int tick) {
        int remaining = absorbShield(target, damage);
        target.hp = Math.max(0, target.hp - remaining);
        target.damageTaken += damage;
        actor.damageDealt += damage;
        addEnergyFromDamage(target, crit);
        events.add(BattleEvent.hit(tick, actor, target, skill, damage, crit));
        if (target.hp == 0) {
            kill(target, events, tick);
        }
    }

    private static void addEnergyFromDamage(BattleUnit target, boolean crit) {
        int room = Math.max(0, 30 - target.energyFromDamageThisTurn);
        int gain = Math.min(crit ? 15 : 10, room);
        target.energy = Math.min(BattleUnit.MAX_ENERGY, target.energy + gain);
        target.energyFromDamageThisTurn += gain;
    }

    private static int absorbShield(BattleUnit target, int damage) {
        int remaining = damage;
        if (target.shield > 0) {
            int absorbed = Math.min(target.shield, remaining);
            target.shield -= absorbed;
            remaining -= absorbed;
            if (target.shield <= 0) {
                target.shield = 0;
                target.shieldTurns = 0;
            }
        }
        return remaining;
    }

    private static boolean applyDot(BattleUnit target, List<BattleEvent> events, int tick) {
        if (target.effects.isEmpty()) {
            return false;
        }
        List<StatusEffect> dots = new ArrayList<>();
        for (StatusEffect effect : target.effects) {
            if (effect.isDot()) {
                dots.add(effect);
            }
        }
        for (StatusEffect effect : dots) {
            int damage = Math.max(1, effect.value);
            int remaining = absorbShield(target, damage);
            target.hp = Math.max(0, target.hp - remaining);
            target.damageTaken += damage;
            if (effect.source != null) {
                effect.source.damageDealt += damage;
            }
            events.add(BattleEvent.dot(tick, effect.source, target, effect, damage));
            if (target.hp == 0) {
                kill(target, events, tick);
                return true;
            }
        }
        return false;
    }

    private static boolean consumeStun(BattleUnit target, List<BattleEvent> events, int tick) {
        Iterator<StatusEffect> iterator = target.effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            if (effect.type == StatusType.STUN) {
                iterator.remove();
                target.stunImmune = true;
                events.add(BattleEvent.status(tick, null, target, StatusType.STUN, 1, true));
                return true;
            }
        }
        return false;
    }

    private static void applyEffects(BattleUnit actor, BattleUnit target, Skill skill,
                                     Rng rng, List<BattleEvent> events, int tick, int actionIndex) {
        if (skill.effect == null) {
            return;
        }
        int chance = clamp((int) ((long) skill.effectChance
                * (10000L - target.effectiveRes()) / 10000L), 0, 9500);
        boolean success = rng.nextBp() < chance;
        if (!success) {
            return;
        }
        if (!applyStatus(target, skill.effect, skill.effectValue, skill.effectTurns,
                actor, actionIndex)) {
            return;
        }
        events.add(BattleEvent.status(tick, actor, target, skill.effect,
                skill.effectTurns, true));
    }

    private static boolean applyStatus(BattleUnit target, StatusType type, int value,
                                       int turns, BattleUnit source, int actionIndex) {
        if (type == StatusType.STUN) {
            if (target.stunImmune || target.hasStatus(StatusType.STUN)) {
                return false;
            }
            target.effects.add(new StatusEffect(StatusType.STUN, 0, 1, actionIndex));
            return true;
        }
        if (type == StatusType.BURN || type == StatusType.BLEED) {
            if (target.statusCount(type) >= MAX_EFFECT_STACKS) {
                return false;
            }
            int perTurn = (int) ((long) source.effectiveAtk()
                    * (type == StatusType.BURN ? 500L : 400L) / 10000L);
            StatusEffect effect = new StatusEffect(type, Math.max(1, perTurn),
                    Math.max(1, turns), actionIndex);
            effect.source = source;
            target.effects.add(effect);
            return true;
        }

        Iterator<StatusEffect> iterator = target.effects.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().type == type) {
                iterator.remove();
            }
        }
        StatusEffect effect = new StatusEffect(type, Math.max(1, value),
                Math.max(1, turns), actionIndex);
        effect.source = source;
        target.effects.add(effect);
        return true;
    }

    private static void applySupport(BattleUnit actor, BattleUnit ally, Skill skill,
                                     List<BattleEvent> events, int tick, int actionIndex) {
        if (skill.heal && skill.healPower > 0) {
            int amount = (int) ((long) actor.effectiveAtk() * skill.healPower / 10000L
                    * skill.scaleBp / 10000L);
            int before = ally.hp;
            ally.hp = Math.min(ally.maxHp, ally.hp + amount);
            int actual = ally.hp - before;
            actor.healingDone += actual;
            events.add(BattleEvent.heal(tick, actor, ally, skill, actual));
        }
        if (skill.shieldPower > 0) {
            int amount = (int) ((long) actor.effectiveAtk() * skill.shieldPower / 10000L
                    * skill.scaleBp / 10000L);
            int cap = ally.maxHp * SHIELD_MAX_HP_BP / 10000;
            int before = ally.shield;
            ally.shield = Math.min(cap, ally.shield + amount);
            ally.shieldTurns = 3;
            ally.shieldAppliedAction = actionIndex;
            events.add(BattleEvent.shield(tick, actor, ally, skill,
                    Math.max(0, ally.shield - before)));
        }
        if (skill.effect != null
                && applyStatus(ally, skill.effect, skill.effectValue, skill.effectTurns,
                actor, actionIndex)) {
            events.add(BattleEvent.status(tick, actor, ally, skill.effect,
                    skill.effectTurns, true));
        }
    }

    private static void decrementEffects(BattleUnit unit, int actionIndex) {
        Iterator<StatusEffect> iterator = unit.effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            if (effect.appliedAction < actionIndex) {
                effect.turns--;
                if (effect.turns <= 0) {
                    iterator.remove();
                }
            }
        }
        if (unit.tauntTurns > 0) {
            unit.tauntTurns--;
        }
        if (unit.shieldTurns > 0 && unit.shieldAppliedAction < actionIndex) {
            unit.shieldTurns--;
            if (unit.shieldTurns <= 0) {
                unit.shield = 0;
            }
        }
    }

    private static void kill(BattleUnit unit, List<BattleEvent> events, int tick) {
        unit.alive = false;
        unit.hp = 0;
        unit.shield = 0;
        unit.shieldTurns = 0;
        unit.effects.clear();
        events.add(BattleEvent.death(tick, unit));
    }

    private static BattleResult buildResult(BattleInput input, List<BattleUnit> units,
                                            List<BattleEvent> events, int ticks) {
        int aliveA = 0;
        int aliveB = 0;
        long hpA = 0;
        long maxA = 0;
        long hpB = 0;
        long maxB = 0;
        long damageA = 0;
        long damageB = 0;
        for (BattleUnit unit : units) {
            if (unit.alive) {
                if (unit.team == 0) {
                    aliveA++;
                } else {
                    aliveB++;
                }
            }
            if (unit.team == 0) {
                hpA += unit.hp;
                maxA += unit.maxHp;
                damageA += unit.damageDealt;
            } else {
                hpB += unit.hp;
                maxB += unit.maxHp;
                damageB += unit.damageDealt;
            }
        }

        int pctA = maxA <= 0 ? 0 : (int) ((hpA * 100L + maxA - 1) / maxA);
        int pctB = maxB <= 0 ? 0 : (int) ((hpB * 100L + maxB - 1) / maxB);
        int winner;
        String reason;
        if (aliveA == 0 && aliveB == 0) {
            winner = -1;
            reason = "draw";
        } else if (aliveB == 0) {
            winner = 0;
            reason = "wipe";
        } else if (aliveA == 0) {
            winner = 1;
            reason = "wipe";
        } else if (pctA != pctB) {
            winner = pctA > pctB ? 0 : 1;
            reason = "timeout";
        } else if (damageA != damageB) {
            winner = damageA > damageB ? 0 : 1;
            reason = "timeout";
        } else {
            winner = 1;
            reason = "timeout";
        }

        events.add(BattleEvent.end(ticks, winner, reason));

        BattleUnit mvp = null;
        long bestScore = -1;
        for (BattleUnit unit : units) {
            if (unit.team != 0) {
                continue;
            }
            long score = unit.damageDealt * 10L
                    + unit.healingDone * 7L
                    + unit.damageTaken * 4L;
            if (score > bestScore) {
                bestScore = score;
                mvp = unit;
            }
        }

        String hash = hash(input, units, events);
        return new BattleResult(input.seed, winner, ticks, pctA, pctB,
                mvp == null ? null : mvp.uid,
                mvp == null ? null : mvp.name,
                hash, events, units);
    }

    private static String hash(BattleInput input, List<BattleUnit> units,
                               List<BattleEvent> events) {
        List<String> teamIds = new ArrayList<>();
        for (BattleUnit unit : units) {
            teamIds.add(unit.team + ":" + unit.charId + ":" + unit.slot + ":"
                    + unit.level + ":" + unit.maxHp);
        }
        Collections.sort(teamIds);

        StringBuilder payload = new StringBuilder();
        payload.append(input.seed).append('|');
        for (String id : teamIds) {
            payload.append(id).append(';');
        }
        payload.append('|');
        for (BattleEvent event : events) {
            payload.append(event.tick)
                    .append(event.type.name())
                    .append(event.srcUid)
                    .append(event.dstUid)
                    .append(event.value)
                    .append(event.hpAfter)
                    .append(event.crit ? 1 : 0)
                    .append(';');
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
