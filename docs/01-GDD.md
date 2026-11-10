# 01 — GAME DESIGN DOCUMENT (GDD)
## THE MYTH — 2D Anime Mitologi RPG, 3 VS 3 Idle Auto Battle

Versi 1.0 · 2026-11-10 · Dokumen ini adalah acuan desain. Angka numerik final ada di `06-BALANCE-SHEETS.md`; algoritma battle di `02-BATTLE-SYSTEM.md`; skema data di `03-DATABASE.md`.

---

## 1. KONSEP UTAMA

### 1.1 Elevator Pitch
Di benua **Arunika**, lima kawasan elemen dijaga oleh para **Mitos** — perwujudan nama-nama kuno. Retakan Langit melepaskan **Kabut Lupa** yang menghapus nama mereka dari dunia. Pemain berperan sebagai **Pemanggil Nama** yang memulihkan nama para Mitos, menyusun tim tiga petarung, dan memimpin mereka dalam pertarungan otomatis melawan Kabut Lupa maupun Pemanggil lain.

### 1.2 Data Inti
| Aspek | Keputusan |
|---|---|
| Genre | 2D anime mitologi RPG |
| Platform | Android (landscape, locked) |
| Kamera | Side view 2.5D (parallax 2 layer) |
| Battle | 3 vs 3, idle auto battle berbasis Action Gauge (ATB) |
| Multiplayer | Online: async PvP (snapshot), live PvE co-op fase 2 |
| Akun | Guest (anonymous), Google Sign-In, tautkan akun |
| Bahasa | Indonesia (default), English |
| Durasi battle | 45–120 detik (3x speed: 20–45 detik) |
| Session | 5–15 menit, 3–5 sesi/hari |

### 1.3 Pilar Desain
1. **Tontonan, bukan tombol.** Pemain menyusun strategi sebelum battle; saat battle mereka menonton, mempercepat, dan membaca log.
2. **Setiap Mitos punya cerita.** Roster kecil tapi dalam (18 karakter awal), bukan ratusan unit tanpa identitas.
3. **Progres tanpa tekanan.** Idle reward, energy yang tidak memaksa login tiap 2 jam.
4. **Kompetisi transparan.** Rating, log battle, dan peluang gacha terlihat jelas.

### 1.4 Dunia & Cerita
**Lima kawasan:**
- **Karang Agni** — gunung bara, kuil di kawah. Elemen Api.
- **Teluk Wening** — laut tenang yang menyimpan kota tenggelam. Elemen Air.
- **Puncak Bayu** — menara batu di atas awan. Elemen Angin.
- **Rimba Bumi** — hutan purba berakar raksasa. Elemen Tanah.
- **Dataran Kilat** — padang badai abadi. Elemen Petir.

**Struktur cerita (8 bab, 96 stage):**
1. *Nama yang Hilang* — tutorial, memulihkan Sena Agni.
2. *Bara yang Menyala* — Karang Agni, bos **Iblis Arang**.
3. *Nyanyian Teluk* — Teluk Wening, bos **Ikan Purba Bermata Sembilan**.
4. *Angin yang Lupa Arah* — Puncak Bayu.
5. *Akar yang Berbicara* — Rimba Bumi.
6. *Badai Tanpa Nama* — Dataran Kilat.
7. *Retakan Langit* — konfrontasi dengan **Sang Kabut**, antitesis Pemanggil Nama.
8. *Kembalinya Nama* — endgame, membuka **Menara Tanpa Ujung**.

**Tema:** identitas, ingatan, dan nama. Tidak ada karakter yang meniru IP lain.

---

## 2. GAMEPLAY LOOP

### 2.1 Loop Mikro (30 detik – 2 menit)
Buka app → klaim Idle Chest → cek Energy → pilih stage/arena → battle auto → lihat hasil & loot → upgrade/replace equipment.

### 2.2 Loop Menengah (1 sesi, 5–15 menit)
Daily Mission → 3–5 battle campaign → cek Arena (3 tiket) → gacha/summon jika ada tiket → atur formasi/skill → klaim badge/progress.

### 2.3 Loop Makro (harian → mingguan → musiman)
Harian: login, daily, idle, arena. Mingguan: weekly mission, guild boss, tournament. Musiman (4 minggu): reset rank, season pass, event karakter terbatas, guild war.

### 2.4 Alur Fitur Saling Terhubung
```
Battle → loot (coin, exp, material, equipment)
      → upgrade karakter/equipment (coin)
      → Combat Power naik
      → stage & arena lebih tinggi
      → rank & reward lebih besar
      → summon karakter baru (diamond/tiket)
      → formasi baru & sinergi elemen
```
Semua reward berujung pada tiga hal: **naik stage**, **naik rank**, atau **koleksi**. Tidak ada reward yang tidak terpakai.

---

## 3. ROSTER AWAL (ORIGINAL)

18 Mitos, 5 elemen, 5 class. Nama, julukan, dan konsep orisinal.

| # | Nama | Julukan | Elemen | Class | Rarity | Ringkasan |
|---|---|---|---|---|---|---|
| 1 | Sena Agni | Bara Pertama | Api | Fighter | Common | Petarung muda dari Karang Agni |
| 2 | Raka Baja | Perisai Karat | Tanah | Tank | Common | Penjaga gerbang desa tambang |
| 3 | Gilang Bayu | Langkah Ringan | Angin | Assassin | Common | Pencuri yang berubah jadi pahlawan |
| 4 | Dewi Asri | Tunas Rimba | Tanah | Support | Uncommon | Penyembuh rimba |
| 5 | Nyi Tirta | Suara Teluk | Air | Mage | Uncommon | Penyanyi yang memanggil ombak |
| 6 | Wira Kilat | Kilat Muda | Petir | Assassin | Uncommon | Pelari badai |
| 7 | Angin Laras | Suling Awan | Angin | Mage | Rare | Peniup suling pengendali angin |
| 8 | Wira Bumi | Akar Penjaga | Tanah | Tank | Rare | Golem penjaga hutan |
| 9 | Banyu Aji | Ombak Perkasa | Air | Fighter | Rare | Pendekar laut |
| 10 | Samudra Wening | Nyanyian Penyembuh | Air | Support | Epic | Penyembuh kota tenggelam |
| 11 | Kala Rudra | Petir Merah | Petir | Fighter | Epic | Jenderal badai yang jatuh |
| 12 | Bhumi Aksara | Penulis Tanah | Tanah | Mage | Epic | Mage aksara kuno |
| 13 | Aruna | Bara Senja | Api | Assassin | Legendary | Pembunuh bayangan dari kawah |
| 14 | Naga Samudra | Penjaga Palung | Air | Tank | Legendary | Naga laut terakhir |
| 15 | Ratu Kilat | Mahkota Badai | Petir | Mage | Legendary | Ratu Dataran Kilat |
| 16 | Rani Puspa | Tombak Puspa | Angin | Fighter | Legendary | Ksatria bunga angin |
| 17 | Hyang Aditya | Matahari Terakhir | Api | Support | Mythic | Dewa matahari yang melemah |
| 18 | Sang Penjaga Gerbang | Penjaga Retakan | Petir | Tank | Mythic | Penjaga gerbang antar dunia |

**Distribusi:** Common 3, Uncommon 3, Rare 3, Epic 3, Legendary 4, Mythic 2.
**Distribusi class:** Assassin 3, Tank 4, Fighter 4, Mage 4, Support 3.
**Distribusi elemen:** Api 3, Air 4, Angin 3, Tanah 4, Petir 4.

### 3.1 Contoh Profil Karakter (template data)
- **Aruna — Bara Senja** (Api/Assassin/Legendary)
  - Peran: pembunuh backline, burst.
  - Passive *Jejak Bara*: setiap kill +15% SPD selama 2 turn.
  - Skill 1 *Tebasan Senja*: 180% ATK ke target belakang.
  - Skill 2 *Selubung Asap*: 120% ATK area + 30% peluang Burn.
  - Ultimate *Fajar Terakhir*: 420% ATK ke target dengan HP terendah, ignore 25% DEF.
  - Kelemahan: DEF rendah, mudah di-taunt.

*(Detail stat dan skill seluruh roster ada di `06-BALANCE-SHEETS.md`.)*

---

## 4. LIMA CLASS

| Class | Peran | Kelebihan | Kelemahan | Passive Umum | Prioritas Target |
|---|---|---|---|---|---|
| **Assassin** | Burst backline | CRIT DMG +30%, SPD tertinggi | DEF -15%, HP -10% | +10% CRIT Rate saat menyerang target HP < 50% | Baris belakang, lalu Mage/Support |
| **Tank** | Menahan & melindungi | HP +25%, DEF +20% | ATK -10% | Taunt 1 turn saat HP pertama kali < 40%; -20% damage area | Menyerang depan, memaksa lawan fokus |
| **Fighter** | Damage stabil | Seimbang, combo | Tidak ada burst besar | Setiap 3 basic attack → 1 serangan bonus 60% | Terdekat/baris depan |
| **Mage** | Damage area + debuff | Damage area, elemental | DEF rendah, lambat | Ultimate +20% effect chance | Baris tengah/belakang, klaster |
| **Support** | Heal, shield, buff | Utility, revive | Damage rendah | +15% healing received | Ally HP terendah; revive 1x/battle |

**Sinergi tim yang disarankan:**
- **Bruiser:** Tank + Fighter + Support — stabil, bagus untuk campaign.
- **Burst:** Assassin + Mage + Fighter — menang cepat, rapuh.
- **Sustain:** Tank + Support + Mage — lama tapi aman, bagus untuk boss.
- **Full element:** 3 elemen sama → +8% damage elemen (lihat §11).

---

## 5. SISTEM STAT

### 5.1 Stat Dasar
| Stat | Fungsi | Catatan |
|---|---|---|
| HP | Nyawa | 0 = tumbang |
| ATK | Damage dasar | Fisik atau elemen sesuai skill |
| DEF | Mitigasi | Formula di `02` |
| SPD | Kecepatan Action Gauge | 90–150 |
| CRIT Rate | Peluang crit | basis point, cap 75% |
| CRIT DMG | Pengali crit | default 150% |
| ACC | Akurasi | lawan EVA |
| EVA | Penghindaran | cap 35% |
| RES | Resist efek | lawan effect chance |
| Element Power | Bonus damage elemen | 0–30% |
| CP | Combat Power | ringkasan kekuatan |

### 5.2 Sumber Pertumbuhan
```
Stat Akhir = (Base + Growth × (Level-1)) × RarityMult × StarMult
           + EquipmentFlat
           + AwakeningBonus
dikalikan (1 + SetBonus% + GuildBuff% + ElementSynergy%)
```
- Level karakter: 1–80 (breakthrough di 20/40/60/80).
- Star: 1–6 (duplikat → shard → star).
- Awakening: 0–4 (node pasif).
- Equipment: 6 slot.
- CP dihitung dari stat akhir (formula di `02`).

### 5.3 Level Akun
Level akun 1–100 memberi: max energy, slot inventory, idle cap, dan unlock fitur (Arena lv 8, Guild lv 12, Awakening lv 20).

---

## 6. BATTLE 3 VS 3 (RINGKASAN)

Detail teknis di `02-BATTLE-SYSTEM.md`.

- Formasi 3 slot: **Depan, Tengah, Belakang** (posisi memengaruhi targeting & damage area).
- Action Gauge naik tiap tick; karakter dengan gauge penuh bertindak.
- Basic attack mengisi Energy +20; menerima damage +10. Ultimate saat Energy 100.
- Skill 1 cooldown 3 turn, Skill 2 cooldown 4 turn, Ultimate butuh Energy.
- Battle speed 1x/2x/3x mengubah kecepatan animasi, **bukan** hasil (simulasi tetap sama).
- Auto battle selalu aktif. Pemain bisa memilih strategi AI: `Balanced`, `Focus Backline`, `Protect Ally`, `Burst`.
- Battle log mencatat setiap aksi; replay memutar ulang dari log + seed.
- Kemenangan: tim yang masih punya unit hidup. Jika 90 detik (simulasi) habis → total HP% tertinggi menang; jika seri → damage tertinggi menang.

---

## 7. SISTEM IDLE

| Sumber | Isi | Cap | Peningkatan |
|---|---|---|---|
| Idle Chest | Coin + EXP material | 12 jam (max 24 jam) | Account level & Guild level |
| Offline AFK | Coin/jam + material acak | sama dengan Idle | Stage tertinggi |
| Energy regen | 1 Energy / 6 menit | 120 | Account level |
| Arena ticket | 1 / 30 menit | 5 | Rank |
| Guild donation | 3 / hari | — | — |

**Formula idle (ringkas):** `coin = stageIndex × 12 × jam × (1 + guildBonus)`, material per 2 jam, maksimal 12 jam. Detail di `06`.

Anti-idle-abuse: cap berbasis `serverNow` dari rules (`now`), bukan jam device; reward dihitung ulang dari selisih timestamp.

---

## 8. MULTIPLAYER (RINGKASAN)

| Mode | Tipe | Teknologi | Fase |
|---|---|---|---|
| Arena Ranked | Async PvP (snapshot formasi lawan) | RTDB + matchmaking queue | M5 |
| Arena Casual | Async PvP | RTDB | M5 |
| Friend Battle | Async, kode room | RTDB | M5 |
| Guild Boss | Async co-op damage race | RTDB transaksi | M5 |
| World Boss | Async global damage race | RTDB | M6 |
| Live PvP | Realtime | Butuh server authoritative | Fase 2 (Blaze/Cloud Run) |

**Async PvP** dipilih untuk MVP karena: hemat koneksi, tahan disconnect, tidak butuh server realtime, dan hasil bisa divalidasi deterministik.

**Aturan penting:** client tidak pernah menulis hasil rank/reward langsung. Client menulis `battles/{id}/result` yang kemudian diverifikasi (Fase 1: rules + plausibility; Fase 2: Cloud Function) sebelum `rankings` diperbarui.

---

## 9. PROGRESSION

| Progres | Batas | Material | Efek |
|---|---|---|---|
| Character Level | 1–80 | EXP material, coin | Stat naik |
| Breakthrough | 20/40/60/80 | Element Stone + coin | Buka level cap, +5% stat |
| Star | 1–6 | Character shard | +8% stat/star, skill scaling |
| Awakening | 0–4 | Awakening material + coin | Node pasif |
| Skill Level | 1–5 | Skill scroll + coin | +6% multiplier/level |
| Equipment Enhance | +0–+15 | Enhance stone + coin | +4% main stat/level |
| Account Level | 1–100 | Player EXP | Unlock & cap |
| Rank | Bronze→Mythic | Arena points | Reward musiman |

**Tidak ada sistem yang bisa di-beli langsung dengan uang tanpa batas.** Diamond hanya memberi material lebih cepat.

---

## 10. EKONOMI

### 10.1 Mata Uang
| Mata uang | Sumber | Penggunaan | Batas |
|---|---|---|---|
| **Coin** | Battle, idle, sell item, mission | Level up, enhance, shop | Tidak dibatasi |
| **Diamond** | Mission, achievement, arena, event, IAP | Summon, refresh, skin | Soft cap harian dari F2P ±150 |
| **Energy** | Regen, mission, mail, diamond | Campaign, dungeon, boss | Cap 120 |
| **Ticket** | Daily, event, guild | Summon/PvP/Dungeon | Cap per jenis 10 |

### 10.2 Prinsip
- **Sink > source** untuk coin di endgame (enhance +15 mahal) agar inflasi terkendali.
- Diamond F2P cukup untuk ±1 multi-summon tiap 10–14 hari.
- Tidak ada item yang hanya bisa dibeli dengan uang nyata dan memberi stat battle.

Tabel source/sink lengkap per hari di `06`.

---

## 11. SKILL & ELEMEN

### 11.1 Slot Skill
Basic Attack · Active 1 · Active 2 · Ultimate · Passive · Awakening (unlock lv 40).

### 11.2 Efek
Buff (ATK/DEF/SPD), Debuff (ATK down, DEF down, Slow), DoT (Burn, Bleed, Poison), CC (Stun 1 turn, Silence), Heal, Shield, Revive (Support, 1x).

Semua efek punya: durasi turn, peluang dasar, resistensi target, dan cap stacking (maks 3 stack).

### 11.3 Elemen
| Elemen | Status | Kuat lawan | Lemah lawan |
|---|---|---|---|
| Api | Burn (DoT 5% ATK/turn) | Angin | Air |
| Air | Wet (-15% RES) | Api | Petir |
| Angin | Bleed (DoT 4% ATK/turn) | Tanah | Api |
| Tanah | Petrify (peluang stun) | Petir | Angin |
| Petir | Shock (+10% damage diterima) | Air | Tanah |

Bonus elemen: kuat **+25%**, lemah **-15%**, netral 0%.
Sinergi 3 unit satu elemen: +8% Element Power; 2 unit +4%.

---

## 12. BADGE, TITLE, AVATAR

### 12.1 Badge (contoh)
| Badge | Syarat | Reward |
|---|---|---|
| First Victory | Menang 1 battle | 100 coin |
| Myth Collector | Punya 10 Mitos | 50 diamond |
| Legendary Fighter | Menang 100 battle | 200 diamond |
| PvP Champion | Peringkat 1 arena mingguan | Badge eksklusif |
| Dungeon Explorer | Selesaikan 50 dungeon | 5 ticket |
| Guild Warrior | 500 poin guild | 150 diamond |
| Top Ranking | Top 100 global | Frame emas |

Badge punya progress, klaim manual, dan bisa dipajang maksimal 3 di profil.

### 12.2 Title
Rookie → Shadow Hunter → Master Shinobi → Elite Warrior → Legendary Myth → Grandmaster.
Sumber: achievement, rank akhir musim, event, guild war.

### 12.3 Avatar
Default, Character, Rare, Epic, Legendary, Event, Premium. Fitur: frame, border, rarity, koleksi. Avatar tidak memberi stat.

---

## 13. PLAYER PROFILE

UID · Username · Avatar + Frame · Level + EXP · Rank · CP tertinggi · Total battle/Win/Lose/Win rate · Highest rank · Badge (3) · Title · Mitos favorit · Guild · Last online · Negara opsional.

Profil terlihat oleh lawan di arena (snapshot publik: username, avatar, title, CP, formasi, win rate).

---

## 14. GAME MODE

### 14.1 PvE
| Mode | Energi | Durasi | Reward | Catatan |
|---|---|---|---|---|
| Story Campaign | 6 | 1–2 menit | Coin, EXP, equipment, first-clear diamond | 8 bab × 12 stage, 3 difficulty |
| Endless Tower | 8 | 2 menit | Material, diamond per 5 lantai | Reset mingguan |
| Boss Raid | 10 | 3 menit | Equipment rare, shard | 3 boss/minggu |
| Daily Dungeon | 8 | 2 menit | EXP material sesuai hari | 2x/hari |
| Element Dungeon | 8 | 2 menit | Element stone | Buka per hari elemen |
| Training Ground | 0 | bebas | Tanpa reward | Uji formasi |
| Survival | 10 | wave | Coin scaling | 10 wave |

### 14.2 PvP
| Mode | Tiket | Reward | Reset |
|---|---|---|---|
| Ranked Arena 3v3 | 5 | Arena point, diamond mingguan | Harian tiket, musiman rank |
| Casual Arena | 5 | Coin, mission | Harian |
| Global Ranking | — | Papan | Musiman |
| Weekly Tournament | tiket event | Chest besar | Mingguan |
| Guild War | partisipasi | Guild coin, badge | Mingguan |

### 14.3 Multiplayer
Friend Battle (async), Guild Battle, Co-op Boss, Team Challenge, World Boss.

**Matchmaking ranked:** ambil snapshot lawan dengan `rating ± 150`, lebar ±50 per 10 detik, fallback bot hanya jika >30 detik (ditandai jelas "Bot").
**Season:** 4 minggu; soft reset `newRating = 1200 + (oldRating-1200) × 0.6`; reward berdasarkan tier tertinggi.

---

## 15. GUILD

Create (500 diamond atau 200.000 coin) · Join (cek level min) · Level 1–30 (EXP dari donation & mission) · Member 30 · Role Leader/Officer/Member · Chat (RTDB, 100 pesan terakhir) · Donation (3/hari) · Mission (5/hari) · Shop (guild coin) · Ranking · Boss (mingguan, damage total) · War (sabtu–minggu, 2 guild async).

Izin: Leader (semua), Officer (kick member, approve join, mulai boss), Member (donation, chat, mission).

---

## 16. QUEST & MISSION

| Jenis | Contoh | Reward | Reset |
|---|---|---|---|
| Main Quest | Selesaikan Bab 1 | Diamond, karakter | — |
| Daily | Menang 3 battle | Coin, EXP, ticket | 04:00 |
| Weekly | Menang 20 arena | Diamond, equipment | Senin |
| Achievement | Total 1.000 battle | Badge, diamond | permanen |
| Beginner | Login 7 hari | Rani Puspa (Legendary) | 7 hari |
| Character | Naikkan Aruna ke lv 40 | Shard Aruna | per karakter |
| Guild | Donasi 10 kali | Guild coin | mingguan |
| Event | Kumpulkan 500 event currency | Skin/avatar | per event |

Setiap mission: `target`, `progress`, `claimed`, `expiresAt`. Klaim harus idempoten (sekali saja).

---

## 17. LOGIN & REWARD HARIAN

- **Daily Login:** 7 hari (coin → ticket → diamond → equipment → shard → ticket → Legendary selector).
- **Monthly Login:** 28 hari, hadiah besar di hari 7/14/21/28.
- **Daily Reward:** 3 misi harian → chest.
- **Offline/AFK/Idle Chest:** cap 12 jam (naik ke 24 jam di lv 60), klaim manual.
- **Streak:** putus 1 hari → reset ke hari 1 (tidak menghukum berat; hanya kehilangan streak).

Semua klaim tercatat `lastClaimAt` dan divalidasi rules terhadap `now`.

---

## 18. SHOP

| Toko | Mata uang | Isi | Refresh |
|---|---|---|---|
| General | Coin | Potion, material, EXP | Harian |
| Diamond | Diamond | Summon ticket, material | Statis + harian |
| Coin | Coin | Equipment dasar | Harian |
| Arena | Arena point | Equipment PvP, shard | Mingguan |
| Guild | Guild coin | Buff, material, shard | Mingguan |
| Event | Event currency | Skin, avatar, shard | Selama event |
| Equipment | Coin/Diamond | Equipment random | Harian |
| Premium | Uang nyata | Pass, skin, starter | Statis |

Batas pembelian disimpan di `players/{uid}/shopLimits/{shopId}/{itemId}` dengan `resetAt`.

---

## 19. SUMMON / GACHA

### 19.1 Pool
Normal (diamond/tiket), Premium (limited, rate-up), Element (hanya 1 elemen), Event (limited unit), Shard Summon (shard → karakter).

### 19.2 Rate (transparan, ditampilkan di UI)
| Rarity | Rate |
|---|---|
| Common | 45,0% |
| Uncommon | 30,0% |
| Rare | 15,0% |
| Epic | 7,5% |
| Legendary | 2,2% |
| Mythic | 0,3% |

- **Pity:** jaminan Epic+ di pull ke-20, Legendary+ di pull ke-80 (counter terpisah per pool).
- **x10:** minimal 1 Rare+.
- **Duplikat:** karakter duplikat → shard (Common 10, Uncommon 15, Rare 25, Epic 40, Legendary 60, Mythic 100).
- **Riwayat:** 100 pull terakhir disimpan (timestamp, pool, hasil, pity counter).
- Rate-up event: 50% peluang karakter fokus jika Legendary/Mythic keluar.

### 19.3 Anti pay-to-win
Tidak ada unit eksklusif berbayar; semua masuk pool reguler setelah event selesai (kecuali skin).
Pemain F2P mendapat ±5.700 diamond/bulan dari daily + arena + achievement (≈ 2x multi-summon).

---

## 20. AWAKENING & PROMOTION

| Sistem | Material | Efek | Batas |
|---|---|---|---|
| Star Upgrade | Shard karakter | +8% stat/star, ultimate scaling | 6 |
| Breakthrough | Element Stone + Coin | Buka level cap, +5% stat | 4x |
| Awakening | Awakening Crystal + Coin | Node pasif (4 node) | 4 |
| Skill Enhance | Skill Scroll + Coin | +6% multiplier/level | 5 |
| Evolution | Full material set | Naik rarity bintang visual (bukan rarity gacha) | 1 |

Biaya naik eksponensial; total biaya maksimal satu karakter Legendary ± ekuivalen 3 minggu main aktif (F2P).

---

## 21. FRIEND

Add via UID/username, request/accept, list max 50, online status via RTDB presence, profil teman, kirim gift (1/hari, coin/energy), friend battle (kode room async), hapus teman.

---

## 22. MAIL

System, Reward, Event, Ranking, Compensation. Field: `id`, `type`, `title`, `body`, `attachments[]`, `createdAt`, `expiresAt`, `read`, `claimed`. Maks 100 mail, auto-hapus setelah 30 hari. Klaim idempoten.

---

## 23. EVENT

Limited Character, Login, Boss, PvP Tournament, Collection, Seasonal, Anniversary.
Setiap event: `startsAt`, `endsAt`, `rewards`, `leaderboard`, `eventCurrency`, `missions`.
Event currency hangus 7 hari setelah event (ada peringatan).

---

## 24. NOTIFIKASI

Local notification (tanpa FCM berbayar): Energy penuh, Idle reward siap, Daily mission, Event mulai, Guild activity, Ranking reward, Mail baru. Dijadwalkan `WorkManager`/`AlarmManager`; FCM opsional untuk pengumuman (sudah termasuk dependency).

---

## 25. UI/UX

### 25.1 Design System
| Token | Nilai |
|---|---|
| BG Deep | `#070B1A` |
| BG Panel | `#101A33` |
| Panel Elevated | `#16224A` |
| Purple | `#8B5CF6` |
| Purple Light | `#A855F7` |
| Cyan | `#22D3EE` |
| Gold | `#F5C451` |
| Danger | `#EF4444` |
| Success | `#34D399` |
| Text Primary | `#E6ECFF` |
| Text Muted | `#8B9AC4` |
| Border Glow | `#22D3EE` @ 35% |

- Font heading: **Cinzel** (bundled TTF), body: **Plus Jakarta Sans**.
- Grid 8dp, corner radius 12dp (panel), 20dp (modal), tombol tinggi 48dp.
- Efek glow: layer gradient + shadow cyan/purple, tanpa blur berat (hemat GPU).
- Animasi transisi 180–240ms, easing `FastOutSlowIn`.
- Landscape 16:9; safe area notch; UI utama di kiri/kanan, battle di tengah.

### 25.2 Navigasi
```
Splash → Login → Home Lobby
Home Lobby (bottom nav 5): Battle | Characters | Summon | Guild | More
More → Inventory, Shop, Quest, Arena, Ranking, Friends, Mail, Profile, Settings
Battle → Campaign | Tower | Boss | Arena | Event
Characters → Roster | Detail | Equipment | Skill | Awakening | Formation
```

### 25.3 Spesifikasi 20 Halaman
| # | Halaman | Komponen Utama | Aksi Kunci |
|---|---|---|---|
| 1 | Splash | Logo animasi, loading, versi | Auto lanjut |
| 2 | Login | Guest, Google, syarat, bahasa | Auth |
| 3 | Home Lobby | Profil ringkas, energy, diamond, coin, idle chest, banner event, tombol battle | Klaim idle, masuk mode |
| 4 | Character | Grid kartu, filter elemen/class/rarity, sort CP | Pilih, detail |
| 5 | Formation | 3 slot + cadangan, drag & drop, sinergi elemen, total CP | Simpan formasi |
| 6 | Battle | Arena 3v3, HP bar, energy, speed 1x/2x/3x, pause, log | Speed, skip |
| 7 | Inventory | Tab kategori, grid, filter, search, lock, jual | Pakai, jual |
| 8 | Equipment | 6 slot, bandingkan stat, enhance, set bonus | Pasang, enhance |
| 9 | Skill | Daftar skill, multiplier, cooldown, upgrade | Upgrade |
| 10 | Summon | Banner, rate transparan, x1/x10, pity counter, riwayat | Pull |
| 11 | Shop | Tab toko, item card, harga, batas | Beli |
| 12 | Quest | Main/daily/weekly tab, progress bar, klaim | Klaim |
| 13 | Mission | Sama dengan quest, fokus harian | Klaim |
| 14 | Arena | Rank, tiket, lawan 3 pilihan, riwayat | Battle |
| 15 | Ranking | Top 100, filter global/friend/guild, profil | Lihat |
| 16 | Guild | Info, member, chat, donation, mission, boss, war | Kontribusi |
| 17 | Friends | List, request, search UID, online status | Tambah, gift |
| 18 | Mail | List, badge unread, detail, klaim semua | Klaim |
| 19 | Profile | Statistik, badge, title, avatar, formasi | Ganti tampilan |
| 20 | Settings | Audio, bahasa, akun, tautkan Google, hapus data, kebijakan | Simpan |

### 25.4 State Wajib
- Loading skeleton untuk grid.
- Empty state (belum punya item/teman).
- Error state dengan tombol **Coba Lagi** (tanpa loop tak berujung).
- Konfirmasi untuk pembelian, jual item rare+, keluar guild, hapus akun.
- Anti double-tap: tombol disable saat proses.

---

## 26. AUDIO & ANIMASI

### 26.1 Animasi (sprite sheet, 12 fps, atlas)
Idle (4 frame), Walk (6), Attack (6), Skill (8–12), Ultimate (16–24 + overlay fullscreen), Hurt (2), Defend (3), Victory (6), Defeat (4), Death (6).

### 26.2 VFX
Damage angka melayang (normal putih, crit kuning, heal hijau, shield biru), screen shake ringan untuk ultimate, flash elemental sesuai warna elemen, particle maksimal 80 per battle (hindari lag di device low-end).

### 26.3 Audio
BGM: lobby, battle, boss, guild (loop 90 detik). SFX: hit, crit, skill, ultimate, heal, shield, win, lose, UI tap. Volume terpisah BGM/SFX, mute saat app background.

---

## 27. MONETISASI

| Produk | Isi | Harga indikatif | Syarat adil |
|---|---|---|---|
| Rewarded Ads | +50 energy / +coin | gratis | Opsional, max 5/hari |
| Premium Pass | Kosmetik + material | Rp 49.000/bulan | Tanpa stat battle |
| Monthly Pass | Diamond harian | Rp 29.000/30 hari | Sama dengan F2P harian |
| Cosmetic Shop | Skin, frame | Rp 15.000–75.000 | Kosmetik saja |
| Starter Pack | 1 Epic + material | Rp 19.000 sekali | Epic bisa didapat F2P |
| Optional Ads | Banner | — | Tidak mengganggu battle |

**Larangan:** jual stat eksklusif, unit hanya-bayar, paywall progres, loot box berbayar tanpa rate.

---

## 28. ALUR LINTAS FITUR (CONTOH)

**New player (D1):** Login guest → tutorial Sena Agni → 5 battle campaign → dapat Raka Baja → daily reward → summon x1 gratis (Gilang Bayu) → formasi 3 → arena pertama (bot) → upgrade equipment → idle chest.

**Returning player (D7):** Klaim idle 12 jam → daily 3 misi → arena 5 tiket → guild donation → guild boss → event pull → upgrade Aruna → naik stage.

**Endgame:** Tower 50+ → arena top 100 → guild war → awakening 4 → equipment +15 → seasonal title.

---

## 29. RISIKO DESAIN

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Roster kecil membosankan | Retensi turun | Event karakter baru tiap 6 minggu, skin |
| Async PvP repetitif | Arena sepi | Rotasi modifier mingguan, ranking terlihat |
| Cheat karena client-authoritative | Ekonomi rusak | Deterministik, rules ketat, validator Fase 2 |
| Spark plan 100 koneksi | Tidak bisa scaling | Rencana migrasi Blaze terdokumentasi |
| Sprite 2D mahal | Produksi lambat | Atlas + animasi 12 fps + reuse rig |
| Device low-end | FPS drop | Batas particle, texture 2048, object pooling |

---

## 30. KEPUTUSAN DESAIN FINAL (MVP)

Must-have MVP: Auth guest + Google, Home, roster + formasi, battle PvE 3v3, battle engine deterministik, progression level/equip, inventory, gacha, daily, mail, arena async, ranking, settings.

Should-have: guild, event, tower, boss, friend.

Could-have: live PvP, co-op realtime, tournament otomatis.

Won't (v1): cross-server, trading, real-time chat global, Storage/CDN aset.
