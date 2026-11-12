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
        switch (unit.charId == null ? "" : unit.charId) {
            case "gatotkaca":
                assignGatotkaca(unit, bp);
                return;
            case "nyai_roro_kidul":
                assignNyaiRoroKidul(unit, bp);
                return;
            case "prabu_siliwangi":
                assignPrabuSiliwangi(unit, bp);
                return;
            case "si_pitung":
                assignSiPitung(unit, bp);
                return;
            case "lutung_kasarung":
                assignLutungKasarung(unit, bp);
                return;
            case "calon_arang":
                assignCalonArang(unit, bp);
                return;
            case "ken_arok":
                assignKenArok(unit, bp);
                return;
            case "roro_mendut":
                assignRoroMendut(unit, bp);
                return;
            case "damarwulan":
                assignDamarwulan(unit, bp);
                return;
            case "jaka_tarub":
                assignJakaTarub(unit, bp);
                return;
            default:
                assignByRole(unit, bp);
        }
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
        Skill one = new Skill.Builder("Otot Kawat Tulang Besi", Type.SKILL1)
                .target(Target.ALLY_SELF)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .selfEffect(StatusType.DEF_UP, 2, 5000)
                .selfEffect2(StatusType.REFLECT, 2, 2000)
                .build();
        Skill two = new Skill.Builder("Pukulan Brajadenta", Type.SKILL2)
                .power(17000)
                .scaleBp(bp[2])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.STUN, 10000, 1, 0)
                .build();
        Skill ultimate = new Skill.Builder("Terbang ke Angkasa", Type.ULTIMATE)
                .target(Target.ENEMY_ROW)
                .power(36000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.STUN, 4000, 1, 0)
                .build();
        Skill ultimate2 = new Skill.Builder("Brajamusti - Amukan Petir", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(38000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.DEF_DOWN, 10000, 2, 2500)
                .effect2(StatusType.SHOCK, 10000, 2, 1500)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignNyaiRoroKidul(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Sapu Ombak", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Gelombang Mistis", Type.SKILL1)
                .target(Target.ENEMY_ROW)
                .power(15000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.SLOW, 9000, 2, 2000)
                .build();
        Skill two = new Skill.Builder("Mahkota Samudra", Type.SKILL2)
                .shield(22000)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .build();
        Skill ultimate = new Skill.Builder("Tsunami Pantai Selatan", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(34000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SLOW, 9000, 2, 2500)
                .effect2(StatusType.WET, 9000, 2, 1500)
                .build();
        Skill ultimate2 = new Skill.Builder("Gerbang Istana Laut Selatan", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(39000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SLOW, 10000, 2, 4000)
                .teamHealMaxHp(1500)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignPrabuSiliwangi(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Cakar Harimau", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Aum Harimau Putih", Type.SKILL1)
                .power(8000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.ATK_DOWN, 9000, 2, 3000)
                .build();
        Skill two = new Skill.Builder("Cakar Maung", Type.SKILL2)
                .power(17000)
                .scaleBp(bp[2])
                .cooldown(3)
                .energyGain(15)
                .selfHealFromDamage(3000)
                .build();
        Skill ultimate = new Skill.Builder("Wujud Maung Siliwangi", Type.ULTIMATE)
                .target(Target.ALLY_SELF)
                .scaleBp(bp[3])
                .energyGain(0)
                .selfEffect(StatusType.SPD_UP, 3, 4000)
                .selfEffect2(StatusType.CRIT_UP, 3, 3000)
                .build();
        Skill ultimate2 = new Skill.Builder("Murka Maung Pajajaran", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(40000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.ATK_DOWN, 10000, 2, 3000)
                .teamEffect(StatusType.ATK_UP, 2, 2000)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignSiPitung(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Pukulan Betawi", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Jurus Silat Betawi", Type.SKILL1)
                .power(6000)
                .hits(3)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .build();
        Skill two = new Skill.Builder("Kebal Peluru", Type.SKILL2)
                .target(Target.ALLY_SELF)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .selfEffect(StatusType.EVADE_UP, 2, 4000)
                .selfEffect2(StatusType.DEF_UP, 2, 3000)
                .build();
        Skill ultimate = new Skill.Builder("Jagoan Rawa Belong", Type.ULTIMATE)
                .target(Target.ENEMY_LOWEST)
                .power(38000)
                .execute(4000)
                .scaleBp(bp[3])
                .energyGain(0)
                .build();
        Skill ultimate2 = new Skill.Builder("Jurus Seribu Bayangan", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(14000)
                .hits(2)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.STUN, 2000, 1, 0)
                .selfEffect(StatusType.EVADE_UP, 2, 3000)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignLutungKasarung(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Sentuhan Rimba", Type.BASIC)
                .power(9000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Lempar Buah Gaib", Type.SKILL1)
                .power(8000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.BLIND, 9000, 2, 3000)
                .build();
        Skill two = new Skill.Builder("Kecerdasan Kera Suci", Type.SKILL2)
                .target(Target.ALLY_ALL)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .teamEffect(StatusType.EVADE_UP, 2, 2500)
                .build();
        Skill ultimate = new Skill.Builder("Transformasi Guru Minda", Type.ULTIMATE)
                .heal(25000)
                .target(Target.ALLY_ALL)
                .scaleBp(bp[3])
                .energyGain(0)
                .teamEffect(StatusType.ATK_UP, 3, 2500)
                .build();
        Skill ultimate2 = new Skill.Builder("Anugerah Hutan Kahyangan", Type.ULTIMATE2)
                .target(Target.ALLY_ALL)
                .scaleBp(bp[3])
                .energyGain(0)
                .teamShieldMaxHp(2000)
                .teamHealMaxHp(2000)
                .teamEffect(StatusType.RES_UP, 3, 2500)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignCalonArang(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Sihir Hitam", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Kutukan Penyakit", Type.SKILL1)
                .power(9000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.CURSE, 9000, 3, 300)
                .build();
        Skill two = new Skill.Builder("Sihir Leak", Type.SKILL2)
                .target(Target.ENEMY_ALL)
                .power(5000)
                .hits(3)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .effect(StatusType.BURN, 6000, 2, 0)
                .build();
        Skill ultimate = new Skill.Builder("Ritual Kematian", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(42000)
                .selfHpCost(1500)
                .scaleBp(bp[3])
                .energyGain(0)
                .build();
        Skill ultimate2 = new Skill.Builder("Bangkitnya Ratu Leak", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(40000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.CURSE, 10000, 3, 300)
                .selfHealFromDamage(3000)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignKenArok(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Tusuk Keris", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Sabetan Keris Mpu Gandring", Type.SKILL1)
                .power(17000)
                .armorPierce(3000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .build();
        Skill two = new Skill.Builder("Ambisi Takhta", Type.SKILL2)
                .target(Target.ALLY_SELF)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .selfEffect(StatusType.CRIT_UP, 2, 2500)
                .build();
        Skill ultimate = new Skill.Builder("Dendam Sang Pendiri", Type.ULTIMATE)
                .power(38000)
                .armorPierce(2000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.BLEED, 10000, 2, 0)
                .build();
        Skill ultimate2 = new Skill.Builder("Takdir Keris Mpu Gandring", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(36000)
                .armorPierce(4000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.CURSE, 10000, 3, 200)
                .selfEffect(StatusType.CRIT_UP, 2, 1500)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignRoroMendut(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Sapuan Rok", Type.BASIC)
                .power(9500)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Pesona Rok Cita", Type.SKILL1)
                .power(8000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.STUN, 10000, 1, 0)
                .build();
        Skill two = new Skill.Builder("Pajak Rok Rok", Type.SKILL2)
                .power(6000)
                .drainEnergy(1500)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .build();
        Skill ultimate = new Skill.Builder("Perlawanan Tragis", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(32000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SLOW, 10000, 2, 2500)
                .build();
        Skill ultimate2 = new Skill.Builder("Pesona Seribu Bunga", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(30000)
                .drainEnergy(2000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SLOW, 10000, 2, 3500)
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignDamarwulan(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Tebasan Pedang", Type.BASIC)
                .power(10000)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Tusukan Panah Emas", Type.SKILL1)
                .target(Target.ENEMY_HIGHEST)
                .power(19000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .build();
        Skill two = new Skill.Builder("Wibawa Kesatria", Type.SKILL2)
                .target(Target.ALLY_ALL)
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .teamEffect(StatusType.ATK_UP, 2, 2000)
                .build();
        Skill ultimate = new Skill.Builder("Kemenangan atas Minak Jinggo", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(38000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.STUN, 4000, 1, 0)
                .build();
        Skill ultimate2 = new Skill.Builder("Titah Ksatria Majapahit", Type.ULTIMATE2)
                .target(Target.ALLY_ALL)
                .scaleBp(bp[3])
                .energyGain(0)
                .teamEffect(StatusType.ATK_UP, 3, 3500)
                .teamEffect2(StatusType.CRIT_UP, 3, 1500)
                .cleanseTeam()
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
    }

    private static void assignJakaTarub(BattleUnit unit, int[] bp) {
        Skill basic = new Skill.Builder("Lembing Cahaya", Type.BASIC)
                .power(9500)
                .scaleBp(bp[0])
                .energyGain(20)
                .build();
        Skill one = new Skill.Builder("Selendang Bidadari", Type.SKILL1)
                .power(8000)
                .scaleBp(bp[1])
                .cooldown(3)
                .energyGain(15)
                .effect(StatusType.STUN, 10000, 1, 0)
                .build();
        Skill two = new Skill.Builder("Pencuri Cahaya", Type.SKILL2)
                .power(6000)
                .stripBuff()
                .scaleBp(bp[2])
                .cooldown(4)
                .energyGain(15)
                .build();
        Skill ultimate = new Skill.Builder("Penerbangan Kayangan", Type.ULTIMATE)
                .target(Target.ENEMY_ALL)
                .power(33000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SHOCK, 9000, 2, 1500)
                .build();
        Skill ultimate2 = new Skill.Builder("Tujuh Selendang Kahyangan", Type.ULTIMATE2)
                .target(Target.ENEMY_ALL)
                .power(36000)
                .scaleBp(bp[3])
                .energyGain(0)
                .effect(StatusType.SILENCE, 6000, 2, 0)
                .stripBuff()
                .build();
        unit.setSkills(basic, one, two, ultimate, ultimate2);
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
