# 05 — TECHNICAL & ANDROID IMPLEMENTATION

Target: Android Studio, Java/Kotlin, Firebase Auth + Realtime Database, tanpa Storage.

---

## 1. STACK & KEPUTUSAN TEKNIS

| Area | Pilihan | Alasan |
|---|---|---|
| Bahasa | **Java 11** untuk core/battle, **Kotlin** untuk UI baru | Project sudah Java; Java membuat port engine ke server lebih mudah |
| UI | **View System + ViewBinding** | Ringan, stabil, sudah aktif di project |
| Compose | Opsional hanya untuk layar non-battle | Butuh plugin Compose Compiler; jangan campur untuk MVP |
| Render battle | **SurfaceView + Canvas** | Sprite 2D sederhana, kontrol fps, hemat |
| Animasi UI | Lottie (sudah ada) | Efek glow/loading tanpa banyak aset |
| Async | `ExecutorService` + callback Firebase | Tanpa dependency tambahan; Flow opsional |
| Cache | `SharedPreferences` + file JSON | Offline read, hemat kuota |
| Background | WorkManager | Notifikasi energy/idle, cleanup log |
| Auth | FirebaseAuth + `play-services-auth` | Wajib tambah dependency Google Sign-In |
| DB | Firebase Realtime Database | Sesuai batasan |

### 1.1 Dependency yang perlu ditambah/dirapikan
```kotlin
// WAJIB untuk Google Sign-In (belum ada di build.gradle.kts saat ini)
implementation("com.google.android.gms:play-services-auth:20.7.0")

// Notifikasi terjadwal
implementation("androidx.work:work-runtime:2.9.0")

// Opsional: Play Integrity (anti-cheat lapisan atas)
implementation("com.google.android.play:integrity:1.3.0")
```

### 1.2 Inkonsistensi yang harus dibereskan lebih dulu
| Masalah | Rekomendasi |
|---|---|
| Root Kotlin `1.9.10` vs catalog `2.0.21` | Pilih **1.9.10** untuk MVP; hapus plugin Compose dari catalog atau naikkan semuanya ke 2.0.21 |
| Dependency Compose ada tanpa plugin | Hapus 6 baris Compose untuk MVP, atau aktifkan plugin Compose saat mulai memakai |
| `io.coil-kt:coil-compose` | Tidak dipakai di View System; hapus atau ganti `coil` biasa |
| `com.google.guava` | Tidak diperlukan; hapus jika tidak dipakai |
| Tema Light | Ganti ke dark theme `Theme.Material3.Dark.NoActionBar` / custom |
| `android:screenOrientation` belum diset | `sensorLandscape`, `configChanges="orientation|screenSize|keyboardHidden"` |

---

## 2. STRUKTUR FOLDER

```
app/src/main/
├── java/zhaf/media/themyth/
│   ├── TheMythApp.java                 // Application, init Firebase, prefs
│   ├── MainActivity.java               // host navigasi (single-activity) ATAU router
│   │
│   ├── core/
│   │   ├── Result.java                 // Success/Failure
│   │   ├── UiState.java                // loading/success/error
│   │   ├── Prefs.java                  // SharedPreferences
│   │   ├── Ids.java                    // UUID requestId
│   │   ├── TimeProvider.java           // server offset + now
│   │   ├── Numbers.java                // clamp, bp, format
│   │   └── AppExecutors.java           // io/main executor
│   │
│   ├── auth/
│   │   ├── AuthRepository.java         // anonymous, google, link, delete
│   │   └── SessionManager.java         // current uid, profile cache
│   │
│   ├── data/
│   │   ├── FirebaseRefs.java           // semua path RTDB (single source)
│   │   ├── arena/                      // ArenaRating, ArenaTickets, ArenaMatchmaker,
│   │   │                               // ArenaBattle, ArenaSnapshot, ArenaSnapshotFactory,
│   │   │                               // ArenaResultGuard, ArenaRewards
│   │   ├── social/                     // SocialDay, MailLogic, FriendLogic, GuildLogic,
│   │   │                               // GuildBossLogic + model (MailMessage, FriendEntry,
│   │   │                               // FriendRequest, GiftEntry, DirectoryEntry,
│   │   │                               // GuildInfo, GuildMember, GuildChatMessage,
│   │   │                               // GuildBoss, GuildSummary)
│   │   ├── model/                      // Player, RosterChar, EquipmentInstance,
│   │   │                               // ItemStack, Formation, BattleResult, Reward
│   │   ├── staticdata/                 // CharacterTemplate, SkillDef, ItemDef,
│   │   │                               // EquipmentDef, StageDef, GachaPool, Constants
│   │   └── repository/
│   │       ├── PlayerRepository.java
│   │       ├── RosterRepository.java
│   │       ├── InventoryRepository.java
│   │       ├── SummonRepository.java
│   │       ├── BattleRepository.java
│   │       ├── ArenaRepository.java
│   │       ├── GuildRepository.java    // guild, chat, donasi, boss
│   │       ├── SocialRepository.java   // directory, teman, hadiah
│   │       ├── MailRepository.java     // seed, klaim, cleanup
│   │       └── StaticRepository.java   // cache static/ ke lokal
│   │
│   ├── battle/
│   │   ├── BattleEngine.java
│   │   ├── BattleInput.java
│   │   ├── BattleUnit.java
│   │   ├── BattleResult.java
│   │   ├── BattleEvent.java
│   │   ├── BattleLog.java
│   │   ├── Rng.java
│   │   ├── DamageCalculator.java
│   │   ├── Targeting.java
│   │   ├── ElementMatrix.java
│   │   └── CpCalculator.java
│   │
│   ├── render/
│   │   ├── BattleSurfaceView.java      // loop 60fps, state machine visual
│   │   ├── SpriteAtlas.java
│   │   ├── AnimationPlayer.java
│   │   ├── UnitSprite.java
│   │   ├── FloatingTextPool.java
│   │   ├── ParticlePool.java
│   │   └── Camera.java                 // parallax, shake
│   │
│   ├── audio/
│   │   └── AudioManager.java
│   │
│   ├── service/
│   │   ├── PresenceService.java
│   │   ├── NotificationScheduler.java
│   │   └── CleanupWorker.java
│   │
│   └── ui/
│       ├── common/  BaseActivity, GlowButton, StatBar, CurrencyBar,
│       │            CharacterCardView, RewardDialog, ConfirmDialog, ErrorView
│       ├── splash/    SplashActivity
│       ├── login/     LoginActivity
│       ├── home/      HomeActivity + HomeFragment
│       ├── character/ CharacterListFragment, CharacterDetailActivity,
│       │              EquipmentFragment, SkillFragment, AwakeningFragment
│       ├── formation/ FormationFragment
│       ├── battle/    BattleActivity, BattleResultActivity, ReplayActivity
│       ├── inventory/ InventoryFragment, ItemDetailDialog
│       ├── summon/    SummonFragment, SummonResultDialog, SummonHistoryFragment
│       ├── shop/      ShopFragment
│       ├── quest/     QuestFragment
│       ├── arena/     ArenaActivity (profil, matchmaking, leaderboard)
│       ├── ranking/   RankingFragment
│       ├── guild/     GuildActivity (info, anggota, chat, donasi, boss + launch battle)
│       ├── friends/   FriendsActivity (cari UID, request, hadiah)
│       ├── mail/      MailActivity + MailAdapter
│       ├── profile/   ProfileFragment, TitleBadgeFragment
│       └── settings/  SettingsFragment
│
├── assets/
│   ├── sprites/atlas/battle_units.png, .json
│   ├── sprites/portraits/*.webp
│   ├── anim/*.json
│   ├── audio/bgm/*.ogg
│   ├── audio/sfx/*.ogg
│   └── seed/ (JSON fallback untuk static data saat offline)
│
└── res/
    ├── values/ (id default), values-en/
    ├── drawable/ (nine-patch panel, glow, ikon)
    ├── layout/ (satu activity xml + fragment xml)
    └── font/ (cinzel.ttf, plus_jakarta_sans.ttf)
```

**Aturan:** semua path RTDB hanya didefinisikan di `FirebaseRefs`; semua akses DB lewat repository; UI tidak pernah memanggil `FirebaseDatabase` langsung.

---

## 3. ARSITEKTUR LAPISAN

```
UI (Activity/Fragment/View)
   ↑ UiState
ViewModel / Presenter (opsional, ringan)
   ↑ callback
Repository (validasi lokal + Firebase)
   ↑ model
Model / StaticData
```
- **UI:** hanya render & input. Tidak ada logika ekonomi.
- **Repository:** satu-satunya yang menyentuh Firebase; menangani `requestId`, retry, cache.
- **BattleEngine:** pure Java, tanpa Android/Firebase → bisa diuji dengan JUnit.
- **StaticRepository:** memuat `static/*` sekali, simpan ke `SharedPreferences`/file, refresh jika `meta/version` berubah.

---

## 4. ALUR APLIKASI

```
SplashActivity
  - cek maintenance & minAppVersion (meta)
  - cek auth
  - preload static data (progress bar)
  → LoginActivity / HomeActivity

HomeActivity (single host, bottom navigation)
  - Fragment: Battle, Characters, Summon, Guild, More
  - CurrencyBar selalu terlihat
  - IdleChest badge

BattleActivity
  - intro 1s → simulasi (background) → render event → result
  - tombol 1x/2x/3x, pause, log, skip
```

**Landscape:** `<activity android:screenOrientation="sensorLandscape" android:configChanges="orientation|screenSize|keyboardHidden"/>`
**Keep screen on:** hanya saat battle (`FLAG_KEEP_SCREEN_ON`).

---

## 5. RENDER BATTLE

- `SurfaceView` + thread render sendiri; engine berjalan di thread terpisah.
- Event dari engine disimpan dengan `timeMs`; renderer memutar sesuai speed.
- Sprite: satu atlas 2048×2048 per kategori; frame metadata JSON.
- Object pooling untuk floating text & particle (maks 80 aktif).
- Delta time di-clamp (maks 33 ms) agar tidak lompat setelah pause.
- Low-end: deteksi `ActivityManager.isLowRamDevice()` → 30 fps, particle 40, matikan parallax.
- Posisi unit: tim A di kiri (x = 22%, 30%, 38%), tim B di kanan (78%, 70%, 62%); baris depan lebih dekat tengah.
- State visual: IDLE, WALK, ATTACK, CAST, HURT, DEAD, WIN. Transisi berbasis event, bukan tebakan.

---

## 6. ASSET PIPELINE (TANPA STORAGE)

1. Seniman membuat PNG per frame → digabung ke atlas (TexturePacker/LibreSprite).
2. Metadata JSON: `{ "frames": { "aruna_idle_0": {x,y,w,h} }, "animations": { "idle": [..], "attack": [..] } }`.
3. Simpan di `assets/sprites/atlas/`; dibaca `SpriteAtlas` saat splash.
4. Audio OGG 96 kbps mono untuk SFX, 128 kbps stereo untuk BGM.
5. Ukuran target APK: < 80 MB (AAB memecah densitas).
6. Update aset = rilis versi baru; tidak ada unduhan runtime.

---

## 7. OFFLINE & CACHE

| Data | Cache | TTL |
|---|---|---|
| `static/*` | file JSON di internal storage | sampai `meta/version` berubah |
| Profil pemain | SharedPreferences snapshot | dibaca saat offline, tulis ditunda |
| Battle result | file kecil | sampai terkirim |
| Gambar | bundled (tidak perlu cache) | — |

Saat offline:
- Tampilkan data terakhir + banner "Mode offline — progres tidak tersimpan".
- Battle PvE boleh dimainkan, tetapi reward **tidak** diberikan sampai tersinkron.
- Antrean tulis maks 20 aksi; setelah itu tampilkan error.

---

## 8. PENGUJIAN

### 8.1 Unit test wajib
```java
@Test public void battleIsDeterministic() {
    BattleInput in = Fixtures.standardBattle(seed = 42L);
    BattleResult a = BattleEngine.simulate(in);
    for (int i = 0; i < 100; i++) {
        BattleResult b = BattleEngine.simulate(Fixtures.standardBattle(42L));
        assertEquals(a.winner, b.winner);
        assertEquals(a.turns, b.turns);
        assertEquals(a.hash, b.hash);
    }
}
```
- Damage formula: kasus DEF 0, DEF tinggi, crit, evade, elemen kuat/lemah.
- CP calculator: monoton naik seiring stat.
- Gacha: distribusi 1 juta pull dalam toleransi ±0,3%.
- Repository: klaim ganda tidak menambah reward.
- Arena: rating ELO, tiket/regen, pemilihan lawan, penjaga plausibility hasil.
- Sosial (`SocialLogicTest`, 13 test): TTL mail, batas teman 50, hadiah 1×/hari, donasi 3×/hari, damage boss ≤25%, batas minggu/hari, normalisasi nama/tag guild.

### 8.2 UI test
- Login guest → home.
- Formasi 3 → battle → result.
- Beli item → coin berkurang tepat sekali (double tap).
- Rotasi layar saat battle tidak restart.

### 8.3 Device matrix
Low-end (RAM 2 GB, Android 7), mid (Android 11), high (Android 14), tablet 16:10.

---

## 9. ROADMAP

| Milestone | Isi | Estimasi (solo, hari kerja) | Risiko |
|---|---|---|---|
| **M0 Fondasi** | Rapikan Gradle, tema, orientasi, font, `FirebaseRefs`, `Prefs`, `core` | 2 | Rendah |
| **M1 Auth + Shell** | Guest + Google, bootstrap player, splash, login, home, currency bar, settings dasar | 5 | Sedang (SHA-1/OAuth) |
| **M2 Roster + Formasi** | Model roster, static chars, grid, detail, formasi 3, CP | 5 | Rendah |
| **M3 Battle Engine** | Engine, damage, targeting, element, AI, unit test determinisme | 6 | **Tinggi** |
| **M4 Battle UI** | SurfaceView, sprite, HP bar, energy, speed, log, result, replay | 7 | Tinggi (visual) |
| **M5 Progression** | Level, breakthrough, equipment, enhance, inventory, skill upgrade | 8 | Sedang |
| **M6 Gacha + Shop** | Pool, pity, hasil, riwayat, shop, batas beli | 5 | Sedang (RNG & audit) |
| **M7 Arena Async** | Snapshot, matchmaking, battle, rating, ranking | 6 | Tinggi (race) |
| **M8 Sosial** | Guild, chat, boss, friend, mail | 7 | Sedang |
| **M9 Event + Polish** | Event, mission, notifikasi, audio, animasi, loading/empty/error | 6 | Sedang |
| **M10 QA + Rilis** | Bugfix, performa, anti-cheat, rules, Play Console | 6 | Sedang |
| **Total MVP** | | **± 63 hari kerja** | |

Dengan 1 developer penuh waktu: **± 3 bulan** untuk MVP layak rilis; **5–6 bulan** untuk versi dengan guild/event/polish.
Part-time (4 jam/hari): kalikan ± 2 → 6–8 bulan.

> **Status 2026-11-10:** M0–M8 sudah diimplementasikan dan lolos `assembleDebug`, seluruh unit test (termasuk `SocialLogicTest`), dan `lintDebug` (0 error). M7 berjalan sebagai fase 1 tanpa Cloud Functions: snapshot + query rating + settlement client dengan penjaga plausibility (lihat `04-BACKEND-API.md` §4.1). M8 menambahkan mailbox, teman + hadiah harian, guild (buat/gabung/keluar, anggota, chat, donasi 3×/hari), dan boss guild mingguan yang bertarung lewat `BattleActivity`. Berikutnya M9 Event + Polish.

**Jalur cepat (vertical slice 3 minggu):** M0 + M1 + M2 + M3 + M4 versi sederhana (kotak warna, tanpa sprite final) → bisa dimainkan end-to-end lebih dulu, lalu ganti aset.

---

## 10. KOMPLEKSITAS PER FITUR

| Fitur | Kompleksitas | Catatan |
|---|---|---|
| Auth guest/Google | Rendah | Konfigurasi lebih ribet daripada kode |
| Roster + formasi | Rendah | — |
| Battle engine | Tinggi | Determinisme & balance |
| Battle render | Tinggi | Sprite, timing, performa |
| Inventory/equipment | Sedang | Banyak aturan slot & stack |
| Gacha | Sedang | RNG, pity, audit |
| Arena async | Tinggi | Race condition, snapshot, validasi |
| Guild | Tinggi | Izin, transaksi, chat |
| Event | Sedang | Data-driven |
| Admin panel | Sedang | Bisa web terpisah |
| Live PvP | Sangat tinggi | Butuh server authoritative; jangan di MVP |

---

## 11. KONFIGURASI ANDROID

### 11.1 Manifest
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<application
    android:name=".TheMythApp"
    android:allowBackup="false"
    android:hardwareAccelerated="true"
    android:theme="@style/Theme.TheMyth">
    <activity android:name=".ui.splash.SplashActivity" android:exported="true"
        android:screenOrientation="sensorLandscape"
        android:theme="@style/Theme.TheMyth.Splash">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>
    ... activity lain dengan screenOrientation sensorLandscape ...
</application>
```
`allowBackup=false` karena data akun ada di Firebase; backup lokal bisa menyebabkan state ganda.

### 11.2 Theme
```xml
<style name="Theme.TheMyth" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:statusBarColor">#070B1A</item>
    <item name="android:navigationBarColor">#070B1A</item>
    <item name="android:windowLightStatusBar">false</item>
    <item name="android:fontFamily">@font/plus_jakarta_sans</item>
</style>
```

### 11.3 Rasio & ukuran
- Layout memakai `ConstraintLayout` + `layout_constraintGuide_percent` (bukan hardcode px).
- Referensi desain 1920×1080; aman untuk 16:9, 18:9, 19.5:9, tablet.
- `dp` untuk UI, `sp` untuk teks.

### 11.4 Release
- `versionCode` naik setiap rilis; simpan di `meta/minAppVersion`.
- R8/minify: aktifkan setelah stabil, tambahkan `keep` untuk model Firebase (`@Keep`).
- Play App Signing + SHA-1 release → daftarkan di Firebase.
- Data safety form: kumpulkan UID, progres game; tidak ada lokasi/kontak.
- Target SDK mengikuti Play (36 saat ini), 64-bit AAB wajib.

---

## 12. POLA KODE KUNCI

### 12.1 Repository dengan callback + idempotency
```java
public final class MailRepository {
    private final FirebaseDatabase db;
    private final Executor executor;

    public void claim(String uid, String mailId, String requestId, Callback<Reward> cb) {
        DatabaseReference req = db.getReference("requests/" + uid + "/" + requestId);
        req.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData data) {
                if (data.child("status").getValue(String.class) != null) {
                    return Transaction.abort(); // sudah diproses
                }
                data.child("type").setValue("CLAIM_MAIL");
                data.child("status").setValue("PENDING");
                return Transaction.success(data);
            }
            @Override public void onComplete(DatabaseError e, boolean ok, DataSnapshot snap) {
                if (!ok) { cb.onAlreadyProcessed(); return; }
                // lanjut: transaksi mail.claimed + reward
            }
        });
    }
}
```

### 12.2 Presence
```java
DatabaseReference ref = db.getReference("presence/" + uid);
Map<String, Object> online = new HashMap<>();
online.put("online", true);
online.put("lastSeen", ServerValue.TIMESTAMP);
online.put("state", "lobby");
ref.setValue(online);
ref.onDisconnect().setValue(offline(uid));
```

### 12.3 Energy regen
```java
long elapsedMs = now - energyUpdatedAt;
long gained = elapsedMs / (constants.energyRegenSeconds * 1000L);
int newEnergy = (int) Math.min(constants.energyCap, energy + gained);
long newUpdatedAt = energyUpdatedAt + gained * constants.energyRegenSeconds * 1000L;
```

### 12.4 Static data loader
```java
// baca meta/version, bandingkan dengan prefs; jika beda, unduh static/* sekali
db.getReference("meta/version").addListenerForSingleValueEvent(...)
// simpan JSON mentah ke file; parse ke template map saat splash
```

---

## 13. KINERJA & MEMORI

| Target | Nilai |
|---|---|
| FPS battle | 60 (30 low-end) |
| Waktu buka app | < 3 detik (warm < 1,5) |
| Memori | < 220 MB |
| Ukuran APK | < 80 MB |
| Battle engine | < 15 ms |
| Listener RTDB aktif | ≤ 6 |

Aturan:
- Lepas listener di `onStop()`.
- Jangan menyimpan Bitmap penuh per karakter; gunakan atlas + `BitmapRegionDecoder`.
- Hindari alokasi di loop render (reuse `Paint`, `Rect`).
- `RecyclerView` dengan `setHasFixedSize(true)` dan view holder ringan.

---

## 14. CHECKLIST RILIS

- [ ] `google-services.json` dengan `oauth_client` terisi (SHA-1 debug & release).
- [ ] Anonymous + Google aktif di Firebase Console.
- [ ] Rules produksi terpasang & diuji (Rules Playground).
- [ ] Index `rankings` & `matchmaking` dibuat.
- [ ] Back up database & ekspor `static/*`.
- [ ] Login guest, Google, link akun, hapus akun diuji di device nyata.
- [ ] Battle determinisme: unit test lulus 100 iterasi.
- [ ] Klaim ganda (mail/daily/reward) diuji.
- [ ] Double tap pembelian diuji.
- [ ] Offline: banner + antrean tulis.
- [ ] Rotasi layar saat battle.
- [ ] Notifikasi izin Android 13+.
- [ ] Bahasa Indonesia & Inggris lengkap (tidak ada hardcoded string).
- [ ] Target SDK, AAB, Play App Signing, data safety.
- [ ] Kebijakan privasi & syarat penggunaan.
- [ ] Load test: >100 koneksi (jika tetap Spark, siapkan antrean/limit).

---

## 15. YANG TIDAK DIBANGUN DI v1

- Live PvP realtime (butuh server authoritative).
- Cloud Storage / unduh aset runtime.
- Trading antar pemain.
- Chat global (hanya guild).
- Cross-server.
- Cloud Functions (dokumen sudah menyiapkan jalur migrasinya).
