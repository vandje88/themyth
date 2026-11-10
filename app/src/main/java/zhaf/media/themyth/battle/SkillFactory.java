package zhaf.media.themyth.battle;

import zhaf.media.themyth.battle.engine.BattleUnit;
import zhaf.media.themyth.battle.engine.Skill;
import zhaf.media.themyth.battle.engine.Skill.Type;
import zhaf.media.themyth.battle.engine.Skill.Target;
import zhaf.media.themyth.battle.engine.StatusType;
import zhaf.media.themyth.data.progression.Progression;

public final class SkillFactory {

    private SkillFactory() {
    }

    public static void assign(BattleUnit unit) {
        assign(unit, 1, 1, 1, 1);
    }

    public static void assign(BattleUnit unit, int basicLevel, int skill1Level,
                              int skill2Level, int ultimateLevel) {
        if (unit == null) {
            return;
        }
        int[] bp = {
                scaleBp(basicLevel), scaleBp(skill1Level),
                scaleBp(skill2Level), scaleBp(ultimateLevel)
        };
        if ("gatotkaca".equals(unit.charId)) {
            assignGatotkaca(unit, bp);
            return;
        }
        if ("nyai_roro_kidul".equals(unit.charId)) {
            assignNyaiRoroKidul(unit, bp);
            return;
        }
        assignByRole(unit, bp);
    }

    private static int scaleBp(int level) {
        return (int) Math.round(Progression.skillMult(level) * 10000);
    }

    private static void assignGatotkaca(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Hantaman Baja", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Pukulan Kawat", Type.SKILL1)
                .power(17500)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.STUN, 1800, 1, 0)
                .build();
        Skill two = new Skill.Builder("Badai Otot", Type.SKILL2)
                .target(Target.ENEMY_DUO)
                .power(13000)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .selfEffect(StatusType.ATK_UP, 2, 1500)
                .build();
        Skill ultimate = new Skill.Builder("Kepalan Kawat", Type.ULTIMATE)
                .power(39000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.STUN, 3500, 1, 0)
                .build();
        unit.setSkills(basic, one, two, ultimate);
    }

    private static void assignNyaiRoroKidul(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Sapu Ombak", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Gulungan Ombak", Type.SKILL1)
                .target(Target.ENEMY_ROW)
                .power(16000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.WET, 4000, 2, 1500)
                .build();
        Skill two = new Skill.Builder("Pusaran Kidul", Type.SKILL2)
                .target(Target.ENEMY_ALL)
                .power(14000)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .effect(StatusType.DEF_DOWN, 6000, 2, 2000)
                .build();
        Skill ultimate = new Skill.Builder("Gelombang Kidul", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(36000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.WET, 6000, 2, 1500)
                .build();
        unit.setSkills(basic, one, two, ultimate);
    }

    private static void assignByRole(BattleUnit unit, int[] bp) {
        String role = unit.role == null ? "" : unit.role;
        switch (role) {
            case "assassin":
                unit.setSkills(
                        new Skill.Builder("Serangan Kilat", Type.BASIC)
                                .power(10000).scaleBp(bp[0]).energyGain(20).build(),
                        new Skill.Builder("Tusukan Bayangan", Type.SKILL1)
                                .power(18000).scaleBp(bp[1]).cooldown(3).energyGain(15).build(),
                        new Skill.Builder("Belati Kembar", Type.SKILL2)
                                .target(Target.ENEMY_DUO).power(12000).scaleBp(bp[2]).cooldown(4)
                                .energyGain(15)
                                .effect(StatusType.BLEED, 3000, 3, 0)
                                .build(),
                        new Skill.Builder("Eksekusi Senyap", Type.ULTIMATE)
                                .power(42000).scaleBp(bp[3]).energyGain(0).build()
                );
                return;
            case "tank":
                unit.setSkills(
                        new Skill.Builder("Hantam Perisai", Type.BASIC)
                                .power(10000).scaleBp(bp[0]).energyGain(20).build(),
                        new Skill.Builder("Tameng Baja", Type.SKILL1)
                                .power(14000).scaleBp(bp[1]).cooldown(3).energyGain(15).build(),
                        new Skill.Builder("Benteng Depan", Type.SKILL2)
                                .target(Target.ENEMY_ROW).power(9000).scaleBp(bp[2]).cooldown(4)
                                .energyGain(15).shield(15000).build(),
                        new Skill.Builder("Gempa Perisai", Type.ULTIMATE)
                                .target(Target.ENEMY_ALL).power(20000).scaleBp(bp[3])
                                .energyGain(0).build()
                );
                return;
            case "mage":
                unit.setSkills(
                        new Skill.Builder("Sihir Dasar", Type.BASIC)
                                .power(10000).scaleBp(bp[0]).energyGain(20).build(),
                        new Skill.Builder("Badai Elemen", Type.SKILL1)
                                .target(Target.ENEMY_ROW).power(16000).scaleBp(bp[1]).cooldown(3)
                                .energyGain(15).build(),
                        new Skill.Builder("Kutukan Elemen", Type.SKILL2)
                                .target(Target.ENEMY_ALL).power(14000).scaleBp(bp[2]).cooldown(4)
                                .energyGain(15)
                                .effect(StatusType.DEF_DOWN, 6000, 2, 2000)
                                .build(),
                        new Skill.Builder("Murka Alam", Type.ULTIMATE)
                                .target(Target.ENEMY_ALL).power(36000).scaleBp(bp[3]).energyGain(0)
                                .effect(StatusType.SHOCK, 6000, 2, 1000)
                                .build()
                );
                return;
            case "support":
                unit.setSkills(
                        new Skill.Builder("Sentuhan Ringan", Type.BASIC)
                                .power(10000).scaleBp(bp[0]).energyGain(20).build(),
                        new Skill.Builder("Cahaya Pemulih", Type.SKILL1)
                                .heal(16000).scaleBp(bp[1]).cooldown(3).energyGain(15).build(),
                        new Skill.Builder("Berkat Leluhur", Type.SKILL2)
                                .heal(18000).scaleBp(bp[2]).cooldown(4).energyGain(15)
                                .effect(StatusType.ATK_UP, 10000, 2, 1500)
                                .build(),
                        new Skill.Builder("Nyanyian Kehidupan", Type.ULTIMATE)
                                .heal(22000).scaleBp(bp[3]).energyGain(0).build()
                );
                return;
            default:
                unit.setSkills(
                        new Skill.Builder("Serangan Dasar", Type.BASIC)
                                .power(10000).scaleBp(bp[0]).energyGain(20).build(),
                        new Skill.Builder("Jurusan Pertama", Type.SKILL1)
                                .power(17000).scaleBp(bp[1]).cooldown(3).energyGain(15).build(),
                        new Skill.Builder("Jurusan Kedua", Type.SKILL2)
                                .target(Target.ENEMY_DUO).power(13000).scaleBp(bp[2]).cooldown(4)
                                .energyGain(15).build(),
                        new Skill.Builder("Puncak Kekuatan", Type.ULTIMATE)
                                .power(38000).scaleBp(bp[3]).energyGain(0).build()
                );
        }
    }
}
