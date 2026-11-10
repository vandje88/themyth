# 06 — BALANCE SHEETS

Angka di dokumen ini adalah **default v1**. Semua diletakkan di `static/constants` dan template karakter agar bisa diubah tanpa update APK.

---

## 1. RARITY

| Rarity | Mult HP/ATK/DEF | Max Level | Shard per Duplikat | Sell Value | Gacha Rate |
|---|---|---|---|---|---|
| Common | 1,00 | 60 | 10 | 100 coin | 45,0% |
| Uncommon | 1,06 | 65 | 15 | 250 coin | 30,0% |
| Rare | 1,12 | 70 | 25 | 600 coin | 15,0% |
| Epic | 1,20 | 75 | 40 | 1.500 coin | 7,5% |
| Legendary | 1,30 | 80 | 60 | 4.000 coin | 2,2% |
| Mythic | 1,42 | 80 | 100 | 10.000 coin | 0,3% |

Rate dijumlahkan tepat 10.000 bp (100%). Ditampilkan di UI summon.

---

## 2. BASE STAT PER CLASS (LEVEL 1, SEBELUM RARITY)

| Class | HP | ATK | DEF | SPD | CRIT Rate | CRIT DMG | ACC | EVA | RES |
|---|---|---|---|---|---|---|---|---|---|
| Assassin | 3.200 | 380 | 120 | 135 | 1500 | 16000 | 400 | 1000 | 400 |
| Tank | 5.600 | 250 | 260 | 95 | 500 | 14000 | 200 | 400 | 800 |
| Fighter | 4.400 | 330 | 200 | 115 | 1000 | 15000 | 300 | 600 | 600 |
| Mage | 3.400 | 400 | 140 | 105 | 800 | 15500 | 350 | 500 | 500 |
| Support | 4.200 | 260 | 180 | 110 | 500 | 14000 | 250 | 600 | 700 |

Persen dalam basis point (1500 = 15%). SPD tidak memakai basis point.

---

## 3. PERTUMBUHAN LEVEL

```
HP_lv  = Base × Rarity × Star × (1 + 0,048 × (Level - 1))
ATK_lv = Base × Rarity × Star × (1 + 0,042 × (Level - 1))
DEF_lv = Base × Rarity × Star × (1 + 0,045 × (Level - 1))
SPD_lv = Base + 0,10 × (Level - 1)          // maks +8
CRIT/DMG/ACC/EVA/RES = tetap (naik dari bintang, equipment, awakening)
Star  = 1 + 0,08 × (Star - 1)                // 1★ = 1,00 … 6★ = 1,40
```

### Contoh: Aruna (Legendary Assassin) level 42, 3★
```
HP  = 3200 × 1,30 × 1,16 × (1 + 0,048×41) = 14.322
ATK =  380 × 1,30 × 1,16 × (1 + 0,042×41) = 1.560
DEF =  120 × 1,30 × 1,16 × (1 + 0,045×41) =   515
SPD = 135 + 4,1 = 139
```

### Contoh: Kala Rudra (Epic Fighter) level 44, 3★
```
HP  = 4400 × 1,20 × 1,16 × (1 + 0,048×43) = 18.766
ATK =  330 × 1,20 × 1,16 × (1 + 0,042×43) =  1.289
DEF =  200 × 1,20 × 1,16 × (1 + 0,045×43) =   817
SPD = 115 + 4,3 = 119
```

---

## 4. COMBAT POWER

```
CP = HP×0,12 + ATK×3,2 + DEF×1,8 + SPD×12
   + CRITRate×0,6 + CRITDMG×0,12
   + ACC×0,3 + EVA×0,3 + RES×0,4 + ElemPower×6
```
CP adalah **ringkasan**, bukan penentu hasil. Tim dengan CP 10% lebih tinggi tidak otomatis menang (komposisi & elemen penting).

Contoh Aruna di atas (tanpa equipment): **CP ≈ 12.700**. Dengan equipment Epic +7 → **CP ≈ 16.000–18.000**.

---

## 5. EXP

### 5.1 Karakter
```
expToNext(L) = round(120 × L^1,35 / 10) × 10
```
| Level | EXP ke level berikut | Kumulatif |
|---|---|---|
| 1 | 120 | 0 |
| 10 | 2.690 | 12.400 |
| 20 | 6.840 | 51.000 |
| 40 | 17.540 | 205.000 |
| 60 | 30.550 | 545.000 |
| 79 | 45.170 | ±1.450.000 |

Total 1→80 ≈ **1,5 juta EXP** per karakter.

Sumber EXP karakter:
| Sumber | Nilai |
|---|---|
| Battle PvE | `40 + stageIndex × 6` dibagi rata ke unit hidup |
| EXP Scroll S | 1.000 |
| EXP Scroll M | 5.000 |
| EXP Scroll L | 25.000 |
| Idle | 1 scroll S per 2 jam per 10 stageIndex |

### 5.2 Akun
```
playerExpToNext(L) = round(50 × L^1,25)
```
Total 1→100 ≈ **700 ribu EXP**.
Sumber: battle (`15 + stageIndex`), daily mission (500), weekly (3.000), achievement.

---

## 6. BIAYA UPGRADE

### 6.1 Level karakter
`coin = 20 × level` per level up (level 40 → 800 coin).

### 6.2 Breakthrough
| Tier | Syarat Level | Coin | Element Stone | Efek |
|---|---|---|---|---|
| 1 | 20 | 20.000 | 5 | +5% stat, cap 40 |
| 2 | 40 | 60.000 | 10 | +5% stat, cap 60 |
| 3 | 60 | 150.000 | 20 | +5% stat, cap 70 |
| 4 | 80 | 350.000 | 40 | +5% stat, cap 80 |

### 6.3 Star
| Naik ke | Shard | Coin | Efek |
|---|---|---|---|
| 2★ | 20 | 10.000 | +8% stat |
| 3★ | 40 | 25.000 | +8% stat |
| 4★ | 70 | 60.000 | +8% stat |
| 5★ | 110 | 120.000 | +8% stat |
| 6★ | 160 | 250.000 | +8% stat |

Total 1→6★: **400 shard**.

### 6.4 Awakening
| Node | Coin | Awakening Crystal | Efek contoh |
|---|---|---|---|
| 1 | 50.000 | 10 | +5% ATK |
| 2 | 100.000 | 20 | +8% CRIT DMG |
| 3 | 200.000 | 40 | Skill 2 +10% power |
| 4 | 400.000 | 80 | Ultimate +15% power |

### 6.5 Skill level
`coin = 2.000 × level`, `skillScroll = level`. Maks level 5. +6% multiplier per level.

### 6.6 Equipment enhance
`coin = 80 × newLevel^1,5`, `enhanceStone = newLevel`.
| Level | Coin | Stone | Bonus main stat |
|---|---|---|---|
| +1 | 80 | 1 | +4% |
| +5 | 895 | 5 | +20% |
| +10 | 2.530 | 10 | +40% |
| +15 | 4.648 | 15 | +60% |

---

## 7. MATRIX ELEMEN

Baris = penyerang, kolom = target.

| | Api | Air | Angin | Tanah | Petir |
|---|---|---|---|---|---|
| **Api** | 1,00 | **0,85** | **1,25** | 1,00 | 1,00 |
| **Air** | **1,25** | 1,00 | 1,00 | 1,00 | **0,85** |
| **Angin** | **0,85** | 1,00 | 1,00 | **1,25** | 1,00 |
| **Tanah** | 1,00 | 1,00 | **0,85** | 1,00 | **1,25** |
| **Petir** | 1,00 | **1,25** | 1,00 | **0,85** | 1,00 |

Sinergi tim: 3 unit satu elemen → `+8% elemPower`; 2 unit → `+4%`.

Status:
| Elemen | Efek | Nilai |
|---|---|---|
| Api | Burn | 5% ATK penyerang / turn, 2 turn |
| Air | Wet | -15% RES, 2 turn |
| Angin | Bleed | 4% ATK / turn, 3 turn |
| Tanah | Petrify | 25% stun 1 turn |
| Petir | Shock | +10% damage diterima, 2 turn |

---

## 8. SKILL DEFAULT PER CLASS

| Class | Skill 1 | Skill 2 | Ultimate |
|---|---|---|---|
| Assassin | 180% single backline, CD 3 | 120% area 3 target + Bleed 30%, CD 4 | 420% lowest HP, ignore 25% DEF |
| Tank | 140% single + taunt 2 turn, CD 3 | 90% area depan + shield 15% maxHP, CD 4 | 200% area + -20% ATK lawan 2 turn |
| Fighter | 170% single, CD 3 | 130% 2 target + self ATK +15%, CD 4 | 380% single + 2 hit bonus |
| Mage | 160% area baris, CD 3 | 140% area + debuff DEF -20%, CD 4 | 360% area semua + elemen status 60% |
| Support | Heal 160% ATK lowest ally, CD 3 | Shield 180% ATK 2 ally + ATK buff 10%, CD 4 | Heal 220% semua + revive 1 ally (35% HP) |

Variasi per karakter mengubah angka ini ±20% dan menambah efek unik. Semua definisi di `static/skills`.

---

## 9. ENERGY BATTLE

| Aksi | Energy |
|---|---|
| Basic attack | +20 |
| Skill 1 / 2 | +15 |
| Menerima damage | +10 (maks +30/turn) |
| Awal battle | 30 |
| Ultimate | -100 |

Artinya ultimate pertama sekitar aksi ke-4 (tergantung damage diterima).

---

## 10. GACHA

| Pool | Cost x1 | Cost x10 | Pity Epic | Pity Legendary |
|---|---|---|---|---|
| Normal | 300 diamond / 1 ticket | 2.700 diamond / 10 ticket | 20 | 80 |
| Premium (event) | 350 diamond | 3.150 diamond | 20 | 80 |
| Element | 300 diamond | 2.700 diamond | 20 | 100 |
| Shard Summon | 10 shard → 1 pull | 100 shard → 10 | — | — |

Aturan:
- x10 dijamin minimal 1 Rare+.
- Pity Epic reset saat Epic+ keluar; Pity Legendary reset saat Legendary+ keluar.
- Rate-up: 50% dari Legendary/Mythic yang keluar adalah karakter fokus.
- Riwayat 100 pull terakhir.
- **Mythic tidak masuk pity**; rate tetap 0,3% dan tersedia di semua pool setelah event selesai.

---

## 11. IDLE REWARD

```
coin       = stageIndex × 40 × jam × (1 + 0,02 × guildLevel)
expScrollS = max(1, floor(stageIndex / 10)) per 2 jam
material   = 1 random (chance 35%) per 2 jam
cap        = 12 jam (24 jam jika account level ≥ 60)
```
Contoh stage 34, 8 jam, guild level 5:
`coin = 34 × 40 × 8 × 1,10 = 11.968`; scroll S = 3 × 4 = 12.

Klaim memakai selisih `lastClaimAt` dan `ServerValue.TIMESTAMP`, bukan jam device.

---

## 12. EKONOMI HARIAN (PEMAIN AKTIF, F2P)

### 12.1 Sumber
| Sumber | Coin | Diamond | Energy | Ticket |
|---|---|---|---|---|
| Idle 8 jam | 11.000 | — | — | — |
| 15 battle campaign | 9.000 | 30 (first clear) | -90 | — |
| Daily login | 500 | 50 | 30 | — |
| Daily mission | 3.000 | 40 | 20 | 1 summon |
| Arena 5x | 2.500 | 40 | — | — |
| Weekly (dibagi 7) | 1.500 | 30 | — | 0,3 |
| **Total/hari** | **± 27.500** | **± 190** | **+50 net** | **± 1,3** |

### 12.2 Pengeluaran wajar
| Pos | Coin |
|---|---|
| Level up 3 karakter (rata-rata lv 40) | 2.400 |
| 2x enhance equipment (+8) | 3.600 |
| 1x skill upgrade | 6.000 |
| Potion/consumable | 1.000 |
| **Total** | **± 13.000** |

Surplus coin dipakai untuk breakthrough/star (biaya besar jangka panjang). Ini mencegah inflasi.

### 12.3 Diamond F2P per bulan
± 190 × 30 = **5.700 diamond** ≈ 2x multi-summon (5.400). Cukup untuk 1 karakter Epic+ baru per bulan lewat pity.
Pemain yang menyimpan 80 pull (24.000 diamond) butuh ± 4 bulan — pity Legendary adalah target jangka panjang, bukan harian.

---

## 13. ENERGY

| Aktivitas | Energi |
|---|---|
| Campaign | 6 |
| Tower | 8 |
| Boss Raid | 10 |
| Daily/Element Dungeon | 8 |
| Survival | 10 |
| Training | 0 |
| Refill pertama | 50 diamond → 60 energy (maks 3x/hari) |

Regen: 1 per 6 menit = 240/hari. Cap 120. Dana harian: 240 + 30 (login) + 20 (misi) = 290 → ± 48 campaign/hari (jika main terus), realistis 15–25.

---

## 14. STAGE & DROP

### 14.1 Scaling musuh
```
enemyStat = base × (1 + 0,09 × (level - 1)) × difficulty
NORMAL 1,00 · HARD 1,35 · NIGHTMARE 1,75
Boss: HP ×1,6 · ATK ×1,2 · RES +10%
```

### 14.2 Reward per stage
| Tipe | Coin | Player EXP | Char EXP | Drop |
|---|---|---|---|---|
| Normal stage | 400 + 40×stage | 15 + stage | 40 + 6×stage | 1–2 item |
| Elite stage (tiap 4) | ×1,5 | ×1,5 | ×1,5 | +1 equipment |
| Boss stage (tiap 12) | ×3 | ×3 | ×3 | equipment Rare+ / shard |
| First clear | +500 | +100 | — | +30 diamond |

### 14.3 Drop table contoh (`dt_stage_1`)
| Item | Chance | Qty |
|---|---|---|
| EXP Scroll S | 60% | 1–2 |
| Enhance Stone | 45% | 1–2 |
| Iron Blade (Common) | 5% | 1 |
| Coin bonus | 20% | 100–300 |

Tidak ada "drop kosong"; jika semua roll gagal, minimal 1 EXP Scroll S.

---

## 15. ARENA

### 15.1 Tier
| Tier | Rating | Reward akhir musim |
|---|---|---|
| Bronze | < 1000 | 200 diamond |
| Silver | 1000–1299 | 400 diamond + 2 ticket |
| Gold | 1300–1599 | 700 diamond + 5 ticket |
| Platinum | 1600–1899 | 1.000 diamond + frame |
| Diamond | 1900–2199 | 1.500 diamond + avatar |
| Mythic | ≥ 2200 | 2.500 diamond + badge + title |

### 15.2 Rating
K = 32 (<1600), 24 (1600–2199), 16 (≥2200). Win streak ≥3: +5 (maks +15).
Soft reset: `1200 + (rating - 1200) × 0,6`.

### 15.3 Tiket
1 per 30 menit, cap 5. Beli 1 tiket = 50 diamond (maks 3/hari).

---

## 16. GUILD

| Sistem | Angka |
|---|---|
| Biaya buat | 500 diamond atau 200.000 coin |
| Level 1–30 | EXP = donation 50/hari + mission + boss |
| Anggota | 30 |
| Donation | 3/hari, 500 coin → 20 guild coin |
| Boss HP | 2.000.000 + 300.000 × guild level |
| Boss damage reward | 1 guild coin per 10.000 damage |
| War | 2 guild, total damage 48 jam |

---

## 17. SHOP (CONTOH HARGA)

| Item | Harga | Batas |
|---|---|---|
| EXP Scroll M | 1.200 coin | 20/hari |
| Enhance Stone | 300 coin | 30/hari |
| Potion S | 150 coin | 10/hari |
| Summon Ticket | 500 diamond | 3/minggu |
| Arena Equipment Chest | 800 arena point | 1/minggu |
| Guild Buff ATK +5% (24 jam) | 500 guild coin | 1/hari |
| Avatar Frame Epic | 1.200 diamond | — |

---

## 18. MONETISASI & NILAI

| Paket | Isi | Harga | Nilai vs F2P |
|---|---|---|---|
| Monthly Pass | 100 diamond/hari × 30 | Rp 29.000 | 3.000 diamond |
| Premium Pass | Kosmetik + 20 scroll L | Rp 49.000 | kosmetik |
| Starter | 1 Epic pilihan + 10 ticket | Rp 19.000 | 1 minggu main |
| Skin Legendary | Kosmetik | Rp 75.000 | 0 stat |

Tidak ada item yang hanya bisa dibeli dengan uang dan memengaruhi battle.

---

## 19. SIMULASI KESEIMBANGAN (SANITY CHECK)

| Skenario | Harapan | Cara uji |
|---|---|---|
| Tim CP +20% vs CP sama, komposisi acak | Menang 65–75% | Simulasi 10.000 battle |
| Elemen kuat vs netral (CP sama) | Menang 55–60% | Simulasi 10.000 |
| 3 Support vs 3 Assassin | Assassin menang < 30 detik | Simulasi |
| Tim tank vs tank | Timeout 90 detik → HP% menang | Simulasi |
| Pity 80 | 95% pemain dapat Legendary+ ≤ 100 pull | 1 juta pull |
| F2P 30 hari | 1 karakter Epic+ baru, 2 karakter lv 60 | Tracking harian |
| Coin harian | Surplus 10–15 ribu/hari, tidak inflasi | 30 hari tracking |

Jika hasil menyimpang >10% dari target, ubah **satu** tuas pada satu waktu:
1. `rarityMult`
2. `levelGrowth`
3. `skillPower`
4. `elementMult`
5. `pity`
6. `idleRate`

---

## 20. TUAS TUNING CEPAT

| Ingin | Ubah |
|---|---|
| Battle lebih cepat | `spd` dasar +10%, atau `maxTicks` 2400 |
| Lebih banyak crit | `critRate` base +200 bp |
| Tank terlalu kuat | mitigasi DEF: `400` → `600` |
| Assassin terlalu lemah | skill 1 power 180% → 195% |
| Progres terlalu lambat | `expToNext` faktor 120 → 100 |
| Ekonomi terlalu longgar | `idleRate` 40 → 32 |
| Gacha terlalu dermawan | rate Legendary 220 → 180 bp |
| Arena terlalu mudah | K 32 → 24 untuk semua tier |

Semua perubahan harus dicatat di `meta/version` dan diuji ulang dengan simulasi.
