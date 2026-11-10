# 04 — BACKEND, API & MULTIPLAYER

Backend yang dipakai: **Firebase Authentication + Realtime Database**. Tidak ada Cloud Functions pada Fase 1.

---

## 1. AUTENTIKASI

### 1.1 Alur
```
Splash
  └─ FirebaseAuth.getCurrentUser() != null ?
       ├─ ya → baca players/{uid} → Home
       └─ tidak → Login screen
             ├─ "Main sebagai Tamu" → signInAnonymously()
             └─ "Masuk dengan Google" → GoogleSignIn → signInWithCredential()
```

### 1.2 Aturan akun
| Kasus | Penanganan |
|---|---|
| Guest lalu login Google | **Link**: `user.linkWithCredential(googleCred)` → data tetap di UID lama |
| Google sudah terpakai akun lain | Tampilkan pilihan "Gunakan akun Google ini" (data lama guest hilang) atau batal |
| Guest ganti device | Tidak bisa, kecuali sudah ditautkan ke Google |
| Hapus akun | Hapus `players/{uid}` + data turunan, lalu `user.delete()` |
| Sesi kedaluwarsa | Re-auth diam-diam; jika gagal → Login |

### 1.3 Bootstrap player (sekali)
Saat login pertama:
1. `players/{uid}/profile` dengan `createdAt = ServerValue.TIMESTAMP`, username `Pemanggil####`.
2. `currencies` awal: coin 1.000, diamond 300, energy 120, ticketSummon 1.
3. `roster/sena_agni` level 1.
4. `formation/slots = ["sena_agni", null, null]`.
5. `flags/tutorialDone = false`.
6. `mail` sambutan.

Gunakan `runTransaction` pada `players/{uid}`; jika node sudah ada, jangan timpa.

### 1.4 Google Sign-In — checklist
- [ ] Firebase Console → Authentication → Sign-in method → **Google** aktif, **Anonymous** aktif.
- [ ] SHA-1 debug + release ditambahkan ke Android app `zhaf.media.themyth`.
- [ ] `google-services.json` diunduh ulang (saat ini `oauth_client` kosong).
- [ ] OAuth consent screen diisi.
- [ ] `strings.xml` tidak perlu web client ID jika memakai `GoogleSignIn` + `GoogleAuthProvider` (Firebase menangani).

---

## 2. MATCHMAKING

### 2.1 Ranked Arena (async) — implementasi fase 1 (M7)

Tanpa Cloud Functions, matchmaking tidak memakai node antrean. Yang berjalan:

```
1. Saat membuka Arena / mengganti formasi, client menulis snapshot diri:
   arena/snapshots/{uid} {username, cp, rating, strategy, units{0..2}, updatedAt}
   (hanya jika formasi berisi >= 3 unit).
2. Tekan "Cari Lawan" → query
   arena/snapshots.orderByChild("rating").startAt(r - w).endAt(r + w), limit 12.
3. Jendela w melebar 150 → 300 → 600 tiap 10 detik, dibatalkan otomatis pada 60 detik.
4. Kandidat dipilih ArenaMatchmaker.pick (rating terdekat, uid sebagai tie-break,
   tidak memilih diri sendiri). Jika kosong → tawaran bot bot_{uid}.
5. Client memotong 1 tiket (transaksi players/{uid}/arena, maks 5, regen 30 menit)
   lalu membuat battles/{bid} berisi seed acak, snapshot kedua tim, status PENDING.
6. BattleActivity memuat battle, mensimulasikan BattleEngine lokal, menampilkan animasi,
   lalu menulis result (sekali) + status SETTLED + rankings + inbox lawan dalam satu
   updateChildren.
```

Node `matchmaking/{mode}/{bracket}/{uid}` di §9 tetap dicadangkan untuk live matchmaking fase 2, dan `onDisconnect` belum diperlukan karena tidak ada antrean persisten.

Bracket tier (Bronze <1000 … Mythic ≥2200) hanya dipakai untuk menampilkan tier dan memilih warna UI; query memakai rating mentah agar tidak ada "dinding bracket".

### 2.1b Bot

Bot = snapshot sintetis `bot_{uid}` dengan rating & CP mendekati pemain, dibangun `ArenaMatchmaker.botFor`. Efeknya:
- K = 8 untuk rating (menang/loss kecil), tidak menulis `arenaInbox`, tidak masuk `rankings`, dan ditandai jelas di UI ("Bot").
- Berguna agar pemain baru tidak tertahan saat server sepi; bukan pengganti ranked sungguhan.

### 2.3 Casual & Friend Battle
- Casual: sama, tanpa rating.
- Friend Battle: pemilik room membuat kode 6 karakter di `battles/rooms/{code}`; teman join; battle async dengan snapshot.

---

## 3. SIKLUS HIDUP BATTLE (STATE MACHINE)

```
CREATED (seed, teams, snapshot)
   │ client simulasi lokal
   ▼
RESULT_SUBMITTED (result + hash ditulis sekali)
   │ fase 1: diterima langsung
   │ fase 2: Function memvalidasi
   ▼
SETTLED (reward & rating ditulis)
   │
   ▼
ARCHIVED (setelah 7 hari / cleanup)
```

Aturan rules (yang sudah aktif di `database.rules.json`):
- `battles/{id}` hanya boleh dibuat oleh `aUid`; `bUid` wajib string ≠ `auth.uid`, dan `a`/`b` wajib memuat `uid`, `units`, `cp`, `rating`.
- `result` hanya boleh ditulis jika belum ada dan `auth.uid == aUid`; `result.by` wajib sama dengan `aUid`; `ticks` 1..900, `winner` -1..1, `hpA`/`hpB` 0..100.
- `status` hanya `PENDING` → `SETTLED`, hanya oleh `aUid` (fase 1 arena). Fase 2 memindahkan penulisan `status`/`result` ke Function dan mencabut izin ini.
- `players/{uid}/arena/settled/{bid}` dan `defended/{bid}` hanya boleh `true`; keduanya penanda idempoten, bukan data yang dibaca UI.

---

## 4. VALIDASI HASIL (FASE 2, DISARANKAN)

Function `onCreate(battles/{id})`:
1. Muat snapshot kedua tim dari `arena/snapshots` (bukan dari payload client).
2. Jalankan ulang `BattleEngine` dengan `seed` dan `engineVersion` yang tersimpan.
3. Bandingkan `result.winner`, `turns`, dan `hash`.
4. Jika cocok → tulis `SETTLED`, update rating, kirim reward.
5. Jika tidak cocok → `REJECTED`, catat `admin/audit`, tandai akun (3x → suspend sementara).

Fase 1 (tanpa Function) hanya dapat memeriksa plausibility:
- `result.turns` antara 3 dan 400 (6 unit × ±18 aksi dalam 90 detik).
- `teamHpPct` 0–100 dan konsisten dengan `winner`.
- `durationMs` wajar.
- Satu `battleId` hanya untuk satu pasangan UID.
- Reward tidak melebihi batas harian.

### 4.1 `ArenaResultGuard` (implementasi fase 1)

Sebelum menulis apa pun, `ArenaRepository.settleBattle` memanggil `ArenaResultGuard.validate(...)` dengan status `PENDING` dan hasil simulasi lokal:

| Cek | Aturan | Alasan |
|---|---|---|
| Status | harus `PENDING` (belum disettle) | mencegah settle ganda |
| `winner` | `-1` (draw), `0` (A), `1` (B) saja | mencegah nilai liar |
| Konsistensi HP | menang A → `hpB == 0`; menang B → `hpA == 0`; draw → kedua > 0 | menutup "menang tanpa musuh mati" |
| `ticks` | 1..`DEFAULT_MAX_TICKS` (900) | durasi battle wajar |
| `hpA`/`hpB` | 0..100 | persen valid |

Hash replay (SHA-256 dari seed + event) tetap dihitung dan disimpan, tetapi fase 1 belum membandingkannya dengan simulasi ulang server — nilainya untuk audit dan Fase 2.

**Batas yang jujur:** client penyerang masih pemilik kebenaran hasil. Pemain yang memodifikasi APK bisa menulis `winner` palsu yang lolos guard (mis. `winner=0, hpB=0`). Mitigasi fase 1: reward kecil (koin + poin, bukan diamond), tiket terbatas, dan marker sekali-settle; mitigasi fase 2: simulasi ulang di Functions dan bandingkan `hash` (`REJECTED` + audit). Karena itu, papan peringkat fase 1 **belum boleh** dipakai untuk hadiah bernilai nyata.

---

## 5. RATING (ELO MODIFIED)

```
expectedA = 1 / (1 + 10^((ratingB - ratingA)/400))
scoreA = 1 if win else 0
delta = round(K × (scoreA - expectedA))
K = 32 (< 1600), 24 (1600–2199), 16 (>= 2200)
newRatingA = max(0, ratingA + delta)
```
- Bot: `K = 8`, hanya pemain yang berubah.
- Draw: `score = 0.5` untuk kedua pihak (perubahan kecil, tidak memicu bonus streak).
- Win streak ≥ 3: +5 bonus (maks +15); bonus hanya untuk kemenangan non-bot.
- Rank tier dari rating: Bronze <1000, Silver 1000–1299, Gold 1300–1599, Platinum 1600–1899, Diamond 1900–2199, Mythic ≥2200.
- Soft reset musiman: `1200 + (rating - 1200) × 0.6`.
- Musim fase 1: string `sYYYY_MM` dihitung UTC (`2026_11`), dipakai sebagai key `rankings/{season}/arena`.

**Rating pembela:** penyerang menghitung `ArenaRating.delta(b.rating, a.rating, defenderWin, 0, false)` dan menaruhnya sebagai `ratingDelta` di inbox. Pembela menerapkan angka itu apa adanya setelah memverifikasi battle — jadi kedua sisi konsisten tanpa perlu online bersamaan.

---

## 6. DISTRIBUSI REWARD

| Aksi | Reward | Validasi |
|---|---|---|
| PvE first clear | diamond + coin + item | `stageStars` belum ada bintang 1 |
| PvE repeat | coin/exp, drop table | Batas energy |
| Arena win | arena point + coin | 1 result per battle, tiket terpakai |
| Arena loss | arena point kecil | — |
| Rank musiman | mail berisi diamond + badge | Dihitung dari ranking final |
| Guild boss | guild coin + material | Damage transaksi |

Semua hadiah masuk lewat **`mail`** untuk hadiah besar (aman dari race), sedangkan mata uang kecil ditulis transaksional. Reward harian dibatasi: diamond F2P maks ±150/hari.

---

## 7. DISCONNECT & RECONNECT

| Situasi | Perilaku |
|---|---|
| Putus saat matchmaking | Node `matchmaking` dihapus via `onDisconnect`; masuk lagi mulai baru |
| Putus setelah battle dibuat | Battle tetap ada; client bisa menulis result saat online kembali (selama belum kedaluwarsa 10 menit) |
| Putus saat battle render | Simulasi sudah selesai; hanya animasi yang terhenti — bisa dilanjutkan |
| App dibunuh | Sama seperti di atas; state battle disimpan lokal (`SharedPreferences` JSON kecil) |
| PvE stage | Resume dari layar stage, bukan mid-battle |
| Live PvP (fase 2) | 15 detik grace, lalu AI menggantikan pemain |

Implementasi presence:
```java
presenceRef.onDisconnect().setValue(offlineMap);
```
Tidak ada penalti disconnect pada mode async. Pada live PvP: disconnect 2x dalam 1 jam → cooldown 10 menit.

---

## 8. ANTI-CHEAT

### 8.1 Lapisan
| Lapisan | Implementasi | Efektivitas |
|---|---|---|
| Integritas app | Google Play Integrity API (tanpa biaya), cek signature APK | Tinggi |
| Determinisme | seed + engine version + hash | Tinggi untuk replay |
| Rules | validasi tipe, batas, kepemilikan, sekali-tulis | Tinggi untuk data |
| Idempotency | requestId | Tinggi untuk duplikasi |
| Rate limit | `requests` timestamp + rules/timer | Sedang |
| Server validation | Functions (Fase 2) | Tinggi |
| Anomali | admin audit (coin gain > X/jam, hasil menang 99%) | Sedang |

### 8.2 Duplicate Request Protection
Semua operasi berdampak (klaim, beli, summon, upgrade) memakai:
```
requests/{uid}/{requestId} = { type, status, result, createdAt }
```
- `requestId` = UUID v4 yang dibuat sekali oleh client dan disimpan sampai sukses.
- Jika `status == DONE`, client menampilkan hasil lama, tidak memanggil ulang.
- Rules: `status` hanya bisa `PENDING` → `DONE`, tidak bisa kembali.

### 8.3 Rate Limit Sederhana (tanpa server)
- Simpan `players/{uid}/flags/lastActionAt` per aksi berat.
- Client menolak aksi jika < 500 ms dari aksi sebelumnya.
- Rules tidak bisa mencegah client modif, tetapi admin audit menandai pola (mis. 100 summon dalam 10 detik).

### 8.4 Larangan
- Jangan pernah menyimpan harga server di client lalu mempercayainya; selalu baca `static/shops`.
- Jangan kirim `uid` di payload (ambil dari `auth.uid`).
- Jangan log token/credential.
- Jangan izinkan client menulis `rankings` orang lain.

---

## 9. RANCANGAN API

### 9.1 Operasi Firebase SDK (Fase 1)
| Aksi | Method | Path | Payload |
|---|---|---|---|
| Bootstrap | transaction | `players/{uid}` | profil awal |
| Baca profil | read | `players/{uid}` | — |
| Simpan formasi | write | `players/{uid}/formation` | slots, strategy, cp |
| Klaim idle | transaction | `players/{uid}/idle` + `currencies` | lastClaimAt |
| Level up | transaction | `players/{uid}/roster/{charId}` | level, exp, cost |
| Pasang equipment | multi-write | `roster/{charId}/equipment`, `equipment/{id}/equippedBy` | — |
| Summon | transaction | `players/{uid}/summons/{pool}` + `roster` + `currencies` | pool, count |
| Klaim mail | transaction | `requests`, `mail`, `currencies` | requestId, mailId |
| Cari arena | write + query | `arena/snapshots/{uid}` (write sendiri), `orderByChild("rating")` (baca) | snapshot formasi |
| Tanding | transaction + push | `players/{uid}/arena/tickets`, `battles/{id}` | — |
| Submit hasil | write-once | `battles/{id}/result` + `status` | winner, ticks, hash |
| Klaim pembelaan | transaction | `players/{uid}/arena` (`arenaInbox`) | — |
| Leaderboard | query | `rankings/{season}/arena` (season `sYYYY_MM`) | orderByChild score |
| Guild join | transaction | `guilds/{gid}/members`, `players/{uid}/guild` | gid |
| Chat | push | `guilds/{gid}/chat` | text |
| Presence | set + onDisconnect | `presence/{uid}` | online, state |

### 9.2 Endpoint HTTP (Fase 2, Cloud Functions/Cloud Run)
Semua memakai header `Authorization: Bearer <Firebase ID token>` dan body JSON. `uid` diambil dari token.

```
POST /v1/summon
  { "requestId": "...", "poolId": "normal", "count": 10 }
  → 200 { "requestId": "...", "results": [ { "rarity": "epic", "charId": "kala_rudra", "isNew": true } ], "pity": { "epic": 4, "legendary": 21 }, "currencyLeft": 5200 }
  → 409 { "code": "ALREADY_PROCESSED", "requestId": "..." }
  → 402 { "code": "INSUFFICIENT_CURRENCY" }

POST /v1/battle/start
  { "mode": "arena_ranked" }
  → 200 { "battleId": "...", "seed": 1234, "opponent": { "uid": "...", "cp": 18000, "formation": [...] }, "ticketLeft": 4 }

POST /v1/battle/submit
  { "battleId": "...", "result": { "winner": 0, "turns": 41, "hash": "..." } }
  → 200 { "settled": true, "rating": 1305, "reward": [...] }
  → 422 { "code": "HASH_MISMATCH" }

POST /v1/claim
  { "requestId": "...", "type": "MAIL", "id": "m_..." }

GET /v1/leaderboard?mode=arena&season=s1&limit=100

POST /v1/guild/join { "guildId": "g_ab12" }
POST /v1/guild/boss/attack { "damage": 42000, "battleId": "..." }
POST /v1/admin/ban { "uid": "...", "reason": "...", "until": 0 }   // admin only
```

Kode error standar:
`UNAUTHENTICATED`, `PERMISSION_DENIED`, `INVALID_ARGUMENT`, `INSUFFICIENT_CURRENCY`, `ALREADY_PROCESSED`, `RATE_LIMITED`, `MAINTENANCE`, `VERSION_TOO_OLD`, `HASH_MISMATCH`.

### 9.3 Prinsip API
- Semua operasi mutasi **idempoten** dengan `requestId`.
- Semua reward berasal dari server (bukan angka dari client).
- Response selalu menyertakan state terbaru yang berubah (currency, rating).
- Timeout client 15 detik, retry maks 2x dengan backoff 1s/3s, lalu tampilkan error Indonesia.

---

## 10. ADMIN PANEL

Fase 1: **aplikasi admin terpisah** (Web/Android) yang login sebagai admin; data admin di `admin/admins/{uid} = true` dan rules membatasi.

| Modul | Aksi | Node |
|---|---|---|
| Dashboard | DAU, battle/hari, error | agregat lokal + RTDB |
| Player | cari UID, lihat, ban, kirim mail, reset | `players`, `admin/bans` |
| Character | lihat, edit stat, aktif/nonaktif | `static/characters` |
| Item | tambah/edit item | `static/items` |
| Economy | atur drop, harga, batas | `static/shops`, `static/gachaPools` |
| Event | buat event + reward | `static/events` |
| Shop | item, harga, stok | `static/shops` |
| Mail | kirim massal (segment) | `mail/{uid}` |
| Ranking | monitor, reset musiman | `rankings` |
| Ban/Suspend | alasan + durasi | `admin/bans` |
| Announcement | banner + teks | `meta/notices` |
| Config | maintenance, versi minimum | `meta` |
| Audit | log aksi admin | `admin/audit` |

Semua aksi admin dicatat: `{adminUid, action, target, at, before, after}`.

---

## 11. NOTIFIKASI

Fase 1 (tanpa server):
- Local notification dijadwalkan setelah pemain keluar: energy penuh, idle ready, daily reset.
- Gunakan `WorkManager` + `NotificationManager`. Minta izin `POST_NOTIFICATIONS` (Android 13+).

Fase 2:
- FCM topic `global` untuk pengumuman; topic `uid_<uid>` untuk personal.
- Kirim dari admin panel via HTTP v1 (butuh server key; simpan di server, jangan di APK).

---

## 12. KUOTA & BIAYA (JUJUR)

| Rencana | Koneksi simultan | Transfer | Biaya |
|---|---|---|---|
| Spark (sekarang) | 100 | 1 GB/bulan | Gratis |
| Blaze | 200.000 | $1/GB (ada kuota gratis) | Bayar per pakai |
| Functions | — | 2 juta invocation gratis/bulan | Perlu Blaze |

**Target "ribuan pemain":** minimal Blaze. Async PvP menghemat koneksi karena tidak perlu koneksi persisten saat battle.
**Strategi hemat:**
- Cache `static/*` di lokal (hanya baca saat `meta/version` berubah).
- Batasi listener aktif: hanya node pemain sendiri + arena + chat saat layar terbuka.
- Matikan listener saat `onStop()`.
- Simpan log battle maks 20.

---

## 13. SKENARIO GAGAL (WAJIB DITANGANI UI)

| Skenario | Pesan Indonesia | Aksi |
|---|---|---|
| Tidak ada internet | "Tidak ada koneksi. Periksa internetmu." | Tombol Coba Lagi |
| Auth expired | "Sesi berakhir. Silakan masuk lagi." | Ke Login |
| Versi app usang | "Versi baru tersedia. Perbarui aplikasi." | Buka Play Store |
| Maintenance | "Server sedang dalam perbaikan." | Tampilkan jam |
| Data player hilang | "Data tidak ditemukan." | Tombol buat ulang (support) |
| Reward sudah diklaim | "Hadiah sudah diklaim." | Refresh UI |
| Kuota penuh | "Penyimpanan penuh. Jual atau hapus item." | Ke Inventory |
| Battle kedaluwarsa | "Pertarungan kedaluwarsa." | Kembali ke Arena |
| Google login gagal | "Gagal masuk dengan Google. Coba lagi." | Retry, fallback guest |

Semua retry dibatasi (maks 2–3x), tombol dinonaktifkan saat proses, dan tidak ada loop otomatis tanpa henti.
