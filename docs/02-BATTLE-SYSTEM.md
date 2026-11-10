# 02 — SISTEM BATTLE 3 VS 3

Dokumen ini adalah spesifikasi implementasi `BattleEngine`. Semua angka default ada di `06-BALANCE-SHEETS.md`.

---

## 1. PRINSIP

1. **Deterministik:** input sama + seed sama → hasil sama, di device mana pun.
2. **Frame-independent:** simulasi berbasis tick, bukan `deltaTime` render. Render hanya memutar hasil.
3. **Tanpa intervensi saat battle.** Strategi dipilih sebelum battle.
4. **Log ringkas tapi cukup** untuk replay dan audit.

---

## 2. MODEL DATA BATTLE

```java
public final class BattleUnit {
    public String uid;          // id unik dalam battle (mis. "A0")
    public String charId;       // "aruna"
    public int team;            // 0 = pemain, 1 = lawan
    public int slot;            // 0 depan, 1 tengah, 2 belakang
    public int level;

    // Stat final (sudah termasuk equipment/buff permanen)
    public int hp, maxHp;
    public int atk, def, spd;
    public int critRate;        // basis point, 7500 = 75%
    public int critDmg;         // 15000 = 150%
    public int acc, eva;        // basis point
    public int res;             // effect resistance
    public int elemPower;       // 0..3000 = 0..30%

    public Element element;
    public Role role;           // ASSASSIN, TANK, FIGHTER, MAGE, SUPPORT

    // State runtime
    public int gauge;           // 0..5000
    public int energy;          // 0..100
    public int shield;
    public boolean alive = true;
    public boolean taunting;
    public int[] cooldown = new int[3];   // skill1, skill2, ultimate(0 = pakai energy)
    public List<StatusEffect> effects = new ArrayList<>();
    public SkillSet skills;
}
```

```java
public final class BattleInput {
    public long seed;             // dari server / matchmaking
    public List<BattleUnit> teamA;
    public List<BattleUnit> teamB;
    public AiStrategy strategyA;  // BALANCED, FOCUS_BACKLINE, PROTECT_ALLY, BURST
    public AiStrategy strategyB;
    public int maxTicks = 900;    // 900 × 100ms = 90 detik simulasi
}
```

---

## 3. ACTION GAUGE (ATB)

- Satu tick = **100 ms** simulasi. Setiap tick, unit hidup: `gauge += spd`.
- Saat `gauge >= 5000`, unit siap bertindak.
- Jika beberapa unit siap, urutan: gauge tertinggi → team 0 → slot terkecil (tie-break deterministik).
- Setelah bertindak: `gauge -= 5000` (sisa gauge dipertahankan agar SPD tinggi tetap untung).
- Efek **Slow** mengurangi SPD; **Haste** menambah SPD (maks ±30%).
- Ultimate **tidak** memotong giliran tambahan; ultimate menggantikan aksi saat itu.
- Efek berdurasi turn dihitung dari **aksi unit yang terkena**, bukan tick. DoT diterapkan saat unit tersebut bertindak.

> SPD 100 → satu aksi tiap 5 detik (12 aksi/menit). Battle tipikal 45–90 detik, maksimal 90 detik simulasi.

---

## 4. URUTAN AKSI (AI)

```
1. Jika unit taunt/mati → lewati.
2. Jika energy >= 100 dan ultimate siap → ULTIMATE.
3. Jika skill2 cooldown == 0 → SKILL 2.
4. Jika skill1 cooldown == 0 → SKILL 1.
5. Selain itu → BASIC ATTACK.
6. Kurangi cooldown lain di akhir aksi.
```

Tidak ada pemilihan acak skill. Variasi hanya dari crit, evade, dan effect roll.

---

## 5. TARGETING

| Kondisi | Target |
|---|---|
| Taunt aktif pada lawan | Paksa menyerang unit taunt |
| ASSASSIN | Baris belakang hidup; jika kosong → tengah → depan |
| TANK | Baris depan; skill area → semua baris depan |
| FIGHTER | Baris depan; jika kosong → tengah |
| MAGE | Baris dengan jumlah unit terbanyak; skill area mengenai baris itu |
| SUPPORT (heal) | Ally hidup dengan HP% terendah (termasuk diri) |
| SUPPORT (buff) | Ally dengan ATK tertinggi |
| Semua (fallback) | Unit hidup dengan HP terendah |
| Strategy FOCUS_BACKLINE | Semua class menarget belakang lebih dulu |
| Strategy PROTECT_ALLY | Tank memprioritaskan taunt; support heal lebih agresif |
| Strategy BURST | Ultimate ditahan sampai energy 100; skill damage diprioritaskan |

Target yang mati di tengah multi-hit → hit berikutnya pindah ke target valid berikutnya (tidak hangus).

---

## 6. FORMULA DAMAGE

Semua perhitungan integer, pembulatan **setengah ke atas** (`(a + b/2) / b`), untuk determinisme.

### 6.1 Mitigasi DEF
```
mitigation = DEF / (DEF + 400 + 20 × attackerLevel)
damageAfterDef = raw × (10000 - mitigationBp) / 10000
```
- Level 1, DEF 100 → mitigasi 19,2%
- Level 40, DEF 800 → mitigasi 40,0%
- DEF tidak pernah membuat damage < 5% raw (soft cap).

### 6.2 Bonus Elemen
| Hubungan | Pengali |
|---|---|
| Kuat (+25%) | 12500 |
| Lemah (-15%) | 8500 |
| Netral | 10000 |

Ditambah `elemPower` penyerang: `× (10000 + elemPower) / 10000`.

### 6.3 Crit
```
critChance = clamp(critRate - targetCritResist, 0, 7500)   // targetCritResist default 0
critMultiplier = critDmg (default 15000 = 150%)
```
Crit hanya 1 roll per skill, bukan per hit.

### 6.4 Akurasi/Evasi
```
hitChance = clamp(9800 + acc - eva, 6500, 9800)
```
Jika meleset → damage 0, tidak ada energy untuk penyerang (target tetap dapat +5 energy).

### 6.5 Varians deterministik
```
variance = 9500 + (rngNext() % 1001)   // 95%..105%
```

### 6.6 Rumus Lengkap
```
raw        = ATK × skillPower / 10000
afterDef   = raw × (10000 - mitigationBp) / 10000
afterElem  = afterDef × elementMult / 10000
afterCrit  = crit ? afterElem × critDmg / 10000 : afterElem
final      = max(1, afterCrit × variance / 10000)
final     -= shield (shield diserap dulu, sisa damage ke HP)
```
`skillPower` 10000 = 100% ATK. Basic attack 10000, skill 1 ±16000–20000, skill 2 ±12000–18000 area, ultimate ±35000–45000.

### 6.7 Efek
```
effectChance = clamp(baseChance × (10000 - targetRes) / 10000, 0, 9500)
```
Burn/Bleed/Poison: damage per turn = `5% / 4% / 3%` ATK penyerang (snapshot saat dipasang), maks 3 stack.
Stun: melewati 1 aksi, tidak bisa di-stun 2 turn berturut.
Shield: menyerap damage, maks 30% maxHP, durasi 3 turn.
Heal: `ATK_healer × healPower / 10000 × (1 + healBonus)`, crit heal tidak ada.
Revive: hanya Support, 1x per battle, 35% HP, tidak bisa revive diri sendiri.

---

## 7. ENERGY

- Basic attack: +20
- Skill 1/2: +15
- Menerima damage: +10 (maks +30 per turn dari sumber ini)
- Awal battle: 30 (bisa diatur equipment)
- Ultimate: konsumsi 100

---

## 8. KONDISI MENANG

1. Semua unit satu tim mati → tim lain menang.
2. `maxTicks` (90 detik) tercapai → tim dengan **total HP% lebih tinggi** menang.
3. Seri HP% → total damage diberikan lebih tinggi menang.
4. Masih seri → tim B (lawan) menang (menghindari farming dengan formasi tank ekstrem).

Hasil: `WIN`, `LOSE`, `DRAW` (jarang, hanya jika kedua tim tidak ada unit sejak awal — dianggap tidak valid).

---

## 9. PSEUDOCODE ENGINE (JAVA 11)

```java
public final class BattleEngine {

    public static BattleResult simulate(BattleInput in) {
        Rng rng = new Rng(in.seed);
        List<BattleUnit> units = new ArrayList<>();
        units.addAll(in.teamA);
        units.addAll(in.teamB);

        BattleLog log = new BattleLog(in.seed);
        int tick = 0;

        while (tick < in.maxTicks && !isFinished(units)) {
            // 1) isi gauge
            for (BattleUnit u : units) {
                if (u.alive) u.gauge += u.spd;
            }
            // 2) aksi selama ada yang siap
            while (true) {
                BattleUnit actor = nextReady(units);
                if (actor == null) break;
                if (!isFinished(units)) {
                    act(actor, units, rng, log, tick, in);
                }
                actor.gauge -= 5000;
                tickEffectsEndOfAction(actor);
            }
            tick++;
        }
        return buildResult(units, log, tick);
    }

    private static BattleUnit nextReady(List<BattleUnit> units) {
        BattleUnit best = null;
        for (BattleUnit u : units) {
            if (!u.alive || u.gauge < 5000) continue;
            if (best == null
                    || u.gauge > best.gauge
                    || (u.gauge == best.gauge && u.team < best.team)
                    || (u.gauge == best.gauge && u.team == best.team && u.slot < best.slot)) {
                best = u;
            }
        }
        return best;
    }

    private static void act(BattleUnit a, List<BattleUnit> all, Rng rng,
                            BattleLog log, int tick, BattleInput in) {
        Skill skill = chooseSkill(a);
        if (skill == null) return;

        if (skill.type == SkillType.HEAL || skill.type == SkillType.SHIELD) {
            BattleUnit ally = pickAllyTarget(a, all, skill);
            applySupport(a, ally, skill, all, log, tick);
        } else {
            List<BattleUnit> targets = pickEnemyTargets(a, all, skill);
            for (BattleUnit t : targets) {
                resolveDamage(a, t, skill, rng, log, tick);
            }
            applyEffects(a, targets, skill, rng, log, tick);
        }

        a.energy = Math.min(100, a.energy + skill.energyGain);
        for (int i = 0; i < a.cooldown.length; i++) {
            if (a.cooldown[i] > 0) a.cooldown[i]--;
        }
        if (skill.slot >= 1) a.cooldown[skill.slot - 1] = skill.cooldown;
        if (skill.type == SkillType.ULTIMATE) a.energy = 0;
    }

    private static Skill chooseSkill(BattleUnit a) {
        if (a.energy >= 100 && a.skills.ultimate != null) return a.skills.ultimate;
        if (a.cooldown[1] == 0 && a.skills.skill2 != null) return a.skills.skill2;
        if (a.cooldown[0] == 0 && a.skills.skill1 != null) return a.skills.skill1;
        return a.skills.basic;
    }

    private static void resolveDamage(BattleUnit a, BattleUnit t, Skill s,
                                      Rng rng, BattleLog log, int tick) {
        if (!t.alive) return;

        int hitChance = clamp(9800 + a.acc - t.eva, 6500, 9800);
        if (rng.nextBp() >= hitChance) {
            log.miss(tick, a, t);
            t.energy = Math.min(100, t.energy + 5);
            return;
        }

        long raw = (long) a.atk * s.power / 10000L;
        int mitigationBp = (int) (t.def * 10000L / (t.def + 400L + 20L * a.level));
        mitigationBp = Math.min(mitigationBp, 9500);
        long dmg = raw * (10000L - mitigationBp) / 10000L;

        int elemBp = elementMultiplier(a.element, t.element); // 12500/10000/8500
        dmg = dmg * elemBp / 10000L;
        dmg = dmg * (10000L + a.elemPower) / 10000L;

        boolean crit = rng.nextBp() < clamp(a.critRate, 0, 7500);
        if (crit) dmg = dmg * a.critDmg / 10000L;

        int variance = 9500 + rng.nextInt(1001);
        dmg = dmg * variance / 10000L;
        if (!crit) dmg = Math.max(dmg, 1);

        int finalDmg = (int) Math.min(dmg, Integer.MAX_VALUE);
        applyDamage(t, finalDmg, log, tick, a, crit);
    }

    private static void applyDamage(BattleUnit t, int dmg, BattleLog log,
                                    int tick, BattleUnit src, boolean crit) {
        int remaining = dmg;
        if (t.shield > 0) {
            int absorbed = Math.min(t.shield, remaining);
            t.shield -= absorbed;
            remaining -= absorbed;
        }
        t.hp = Math.max(0, t.hp - remaining);
        t.energy = Math.min(100, t.energy + (crit ? 15 : 10));
        log.hit(tick, src, t, dmg, crit, t.hp);
        if (t.hp == 0) {
            t.alive = false;
            log.death(tick, t);
        }
    }
}
```

### PRNG deterministik
```java
public final class Rng {
    private long s;
    public Rng(long seed) { this.s = seed == 0 ? 0x9E3779B97F4A7C15L : seed; }

    public long next() {                 // SplitMix64
        s += 0x9E3779B97F4A7C15L;
        long z = s;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
    public int nextInt(int bound) { return (int) Math.floorMod(next(), bound); }
    public int nextBp() { return nextInt(10000); }
}
```
**Jangan** memakai `Math.random()`, `Random`, atau waktu device di dalam engine.

---

## 10. BATTLE LOG & REPLAY

Log disimpan sebagai array kejadian ringkas (bukan video):

```json
{
  "seed": 73014444032,
  "version": 1,
  "teams": {
    "a": ["aruna", "wira_bumi", "samudra_wening"],
    "b": ["kala_rudra", "raka_baja", "nyi_tirta"]
  },
  "events": [
    { "t": 12, "type": "HIT", "src": "A0", "dst": "B2", "dmg": 1840, "crit": true, "hp": 4210 },
    { "t": 12, "type": "EFFECT", "src": "A0", "dst": "B2", "id": "burn", "dur": 2 },
    { "t": 18, "type": "ULT", "src": "B0", "dst": "A0", "dmg": 3920, "hp": 980 },
    { "t": 41, "type": "DEATH", "dst": "B2" }
  ],
  "result": { "winner": 0, "turns": 41, "hash": "b1c9..." }
}
```

- `hash = sha256(seed + sortedTeamIds + concatenatedEventTuples)`.
- Replay = jalankan engine dengan `seed` dan log yang sama; UI memutar event per `t`.
- Log disimpan maks 20 battle terakhir per pemain (hemat kuota RTDB).

---

## 11. PVE STAGE SCALING

```
enemyLevel = stage.level
enemyStat = templateBase × (1 + 0.09 × (enemyLevel - 1)) × difficultyMult
difficultyMult = { NORMAL: 1.00, HARD: 1.35, NIGHTMARE: 1.75 }
bossMult = 1.6 HP, 1.2 ATK, +10% RES
```
Stage 1–12 per bab; total 96 stage + 3 difficulty.

---

## 12. ASYNC PVP

1. Server/queue memilih lawan dan menyimpan **snapshot formasi + stat** lawan (`arena_snapshots/{uid}`).
2. Seed dibuat acak oleh sistem (disimpan di `battles/{id}/seed`).
3. Client menjalankan simulasi, menulis `battles/{id}` dengan `resultHash`.
4. Fase 1: rules hanya mengizinkan penulisan result sekali (`!data.exists()`), dan hasil harus berasal dari `players/{uid}` yang sah.
5. Fase 2: Cloud Function menjalankan ulang simulasi dari snapshot + seed dan membandingkan `resultHash`; jika beda → result ditolak, akun ditandai.

**Kenapa aman-ish di Fase 1:** lawan tidak hadir, damage tidak bisa di-farm berkali-kali (result sekali tulis), dan rank dihitung dari hasil yang konsisten dengan snapshot. Namun client yang dimodifikasi tetap bisa memalsukan hasil; karena itu Fase 2 wajib sebelum rank berhadiah nyata.

---

## 13. KOMPLEKSITAS & PERFORMA

- Unit per battle: 6. Aksi maksimal ± 70 per menit simulasi (6 unit × 12 aksi).
- Engine 1 battle di device mid-range: **< 15 ms** (hanya matematika integer).
- Render: sprite atlas, object pooling, maks 80 particle, target 60 fps; fallback 30 fps di device low-end.
- Simulasi boleh dijalankan di background thread; UI hanya membaca `BattleResult`.

---

## 14. CONTOH HASIL

```json
{
  "winner": 0,
  "turns": 41,
  "durationMs": 24600,
  "teamHpPct": { "a": 62, "b": 0 },
  "mvp": "A0",
  "damageDealt": { "A0": 18400, "A1": 9200, "A2": 3100 },
  "healingDone": { "A2": 8700 },
  "hash": "b1c9f0e7a34d..."
}
```

MVP = damage + healing×0.7 + damage tanked×0.4 tertinggi.
