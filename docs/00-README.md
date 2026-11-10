# THE MYTH — Dokumentasi Desain & Arsitektur

**2D Anime Mitologi RPG · 3 VS 3 Idle Auto Battle · Android · Firebase**

Versi dokumen: 1.4 (2026-11-10)
Status kode: M0–M9 selesai (fondasi, auth, roster/formasi, battle engine + UI, progression, gacha + shop, arena async, sosial, meta harian). `assembleDebug`, unit test, dan `lintDebug` **SUKSES**.
Status dokumen: lengkap untuk fase desain; implementasi mengikuti roadmap di `05-TECHNICAL-ANDROID.md` (berikutnya M10 Event + Polish, lalu QA rilis).

---

## 1. Indeks Dokumen

| File | Isi | Dipakai untuk |
|---|---|---|
| `00-README.md` | Indeks, prasyarat, temuan teknis, keputusan | Onboarding |
| `01-GDD.md` | Game Design Document lengkap (konsep s/d monetisasi, UI/UX 20 halaman) | Acuan desain seluruh fitur |
| `02-BATTLE-SYSTEM.md` | Algoritma battle 3v3, formula, AI, contoh kode | Implementasi BattleEngine |
| `03-DATABASE.md` | Skema Firebase Realtime Database, contoh JSON, rules, index, transaksi | Implementasi repository |
| `04-BACKEND-API.md` | Auth, matchmaking, session, API, anti-cheat, admin panel | Backend & integrasi |
| `05-TECHNICAL-ANDROID.md` | Arsitektur Android, struktur folder, roadmap, estimasi | Eksekusi coding |
| `06-BALANCE-SHEETS.md` | Angka konkret: stat, EXP, drop rate, ekonomi, idle reward | Tuning & data statis |

Urutan baca yang disarankan: `01` → `02` → `06` → `03` → `04` → `05`.

---

## 2. Identitas Proyek

- **Nama game:** The Myth
- **Package:** `zhaf.media.themyth`
- **Project Firebase:** `lima-sekawan` (dari `app/google-services.json` yang sudah terpasang)
- **Realtime Database URL:** `https://lima-sekawan-default-rtdb.firebaseio.com`
- **Genre:** 2D anime mitologi RPG, idle auto battle 3v3
- **Orientasi:** Landscape (locked)
- **Bahasa:** Indonesia + English (resource `values/` dan `values-en/`)
- **Target:** Android, `minSdk 24`, `targetSdk 36`, `compileSdk 36`

---

## 3. Batasan Teknis yang Disepakati

Hanya layanan Firebase berikut yang dipakai:

1. **Firebase Authentication** — Anonymous (Guest) + Google Sign-In
2. **Firebase Realtime Database** — seluruh data game

**Tidak dipakai:** Cloud Storage, Hosting, Firestore, Cloud Functions berbayar, Analytics berbayar.
Seluruh aset gambar/audio dibundel di APK (`res/drawable`, `assets/`), bukan diunduh dari Storage.

### Konsekuensi yang harus disadari

| Hal | Dampak | Mitigasi |
|---|---|---|
| Tanpa Storage | Update aset = rilis APK baru / Asset Bundle lokal | Bundel sprite sheet terkompresi; desain atlas sejak awal |
| RTDB **Spark (gratis)** | Maks. **100 koneksi simultan** dan 1 GB transfer/bulan | Untuk "ribuan pemain" perlu **Blaze** (bayar per pemakaian) |
| Tanpa server/Cloud Functions | Validasi hasil battle murni di client, rawan cheat | Fase 1: deterministik + seed + rules ketat; Fase 2: validator server (Functions/Cloud Run) |
| RTDB rules | Tidak bisa menghitung damage | Rules hanya menjaga integritas data, bukan simulasi |

> **Rekomendasi jujur:** target "ribuan pemain aktif" tidak realistis di Spark. Untuk skala itu, anggarkan Blaze (Functions + RTDB) atau server ringan (Ktor di Cloud Run free tier). Arsitektur di dokumen ini sudah disiapkan agar migrasi tersebut tidak membongkar client.

---

## 4. Temuan Teknis Saat Ini (setelah M0–M1)

1. **`oauth_client` di `google-services.json` sudah terisi** (Web client + Android client dengan SHA-1 debug `C5:6E:7D:91…D1:09`). Google Sign-In siap dipakai di build debug. Untuk **release**, tambahkan SHA-1 keystore release di Firebase Console lalu unduh ulang `google-services.json`.
   - Authentication → Sign-in method: pastikan **Google** dan **Anonymous** aktif.
   - SHA-1: `./gradlew signingReport`.
2. **Versi Kotlin sudah diseragamkan** ke `2.0.21` (root plugin + `libs.versions.toml`); dependency Compose/kapt yang tidak terpakai sudah dihapus.
3. **Tema sudah memakai identitas game:** `Theme.TheMyth` (Material3 Dark, navy `#070B1A` + purple/cyan/gold), splash fullscreen, dan style tombol/panel di `res/values/themes.xml`.
4. **Alur M1 berjalan:** `SplashActivity` → cek sesi → `LoginActivity` (Guest + Google) → bootstrap `players/{uid}` via transaksi → `HomeActivity` (profil, currency, formasi).
5. **Gradle JVM:** gunakan JBR Android Studio agar tidak kena PKIX:
   `./gradlew assembleDebug -Dorg.gradle.java.home="C:/Program Files/Android/Android Studio/jbr"`
6. **Antivirus SSL scanning (Avast):** jika dependency baru gagal diunduh dengan `PKIX path building failed`, buat truststore berisi root CA Avast lalu jalankan Gradle dengan:
   `-Dorg.gradle.jvmargs="-Djavax.net.ssl.trustStore=C:/Users/<user>/.gradle/certs/avast-cacerts.jks -Djavax.net.ssl.trustStorePassword=changeit"`
   Hal yang sama membuat emulator gagal TLS ke Firebase (muncul "Trust anchor not found") — solusi: nonaktifkan HTTPS scanning Avast, pakai device fisik, atau system image yang bisa `adb root` untuk menambah CA.

---

## 5. Prinsip Desain

1. **Original 100%.** Nama karakter, dunia, skill, dan cerita dibuat sendiri; tidak menyalin IP lain. Referensi mitologi dunia dipakai sebagai *tema*, bukan aset.
2. **Adil, bukan pay-to-win.** Semua karakter bisa didapat F2P; Diamond hanya mempercepat.
3. **Idle-friendly, aktif-opsional.** Progres tetap jalan saat offline, tapi pemain aktif dapat lebih banyak.
4. **Deterministik.** Battle dengan seed yang sama + state yang sama harus menghasilkan hasil yang sama (dasar replay & anti-cheat).
5. **Data-driven.** Angka balance ada di node `static/`, bukan hardcode; bisa diubah tanpa update APK.
6. **Modular.** Setiap sistem punya repository + model sendiri, mudah ditambah mode baru.

---

## 6. Konvensi Dokumen

- Semua waktu disimpan **epoch milliseconds (Long)**.
- Semua ID memakai `lower_snake_case` untuk statis, dan `uid` Firebase untuk pemain.
- Angka persen di database disimpan sebagai **basis point** (10000 = 100%) untuk menghindari floating error.
- Daftar prioritas fitur memakai **MoSCoW** (Must/Should/Could/Won't) pada roadmap.
- Contoh JSON memakai placeholder `<uid>`, `<charId>`.

---

## 7. Langkah Berikutnya

Roadmap implementasi (lihat `05-TECHNICAL-ANDROID.md` §9):

| Milestone | Status |
|---|---|
| M0 Fondasi (Gradle, tema, core, `FirebaseRefs`) | Selesai |
| M1 Auth + Shell (Guest + Google, bootstrap player, home) | Selesai |
| M2 Roster + Formasi 3v3 | Selesai |
| M3 Battle Engine + unit test determinisme | Selesai |
| M4 Battle UI (SurfaceView, HP bar, result, replay) | Selesai |
| M5 Progression (level, breakthrough, equipment, skill) | Selesai |
| M6 Gacha + Shop (pity, riwayat, batas beli harian) | Selesai — versi 4.5 |
| M7 Arena Async (snapshot, matchmaking, rating, ranking) | Selesai — fase 1 tanpa Functions |
| M8 Sosial (mail, teman, guild, chat, donasi, boss guild) | Selesai — fase 1 tanpa Functions |
| M9 Meta harian (login 7 hari, idle chest, misi harian/mingguan, profil + badge/title, ranking) | Selesai — fase 1 tanpa Functions |
| M10 Event + Polish | Belum |
| M11 QA + Rilis | Belum |

Catatan M6:
- Data pool ada di `app/src/main/assets/seed/gacha.json`, data shop di `seed/shop.json` (dibaca lokal, bukan dari RTDB — sesuai batasan tanpa Storage).
- Seluruh roll dan pembelian dijalankan lewat satu transaksi pada `players/{uid}` agar saldo, roster, pity, dan batas beli tidak bisa terdesinkronisasi.
- Unit test `GachaRollerTest` memverifikasi 1.000.000 roll tetap di dalam rate yang dipublikasikan.

Catatan M7 (Arena Async, fase 1 tanpa Cloud Functions):
- Alur: `ArenaActivity` memublikasikan snapshot formasi ke `arena/snapshots/{uid}`, mencari lawan lewat `orderByChild("rating")` dengan jendela melebar (150 → 300 → 600 tiap 10 detik, batas 60 detik), lalu membuat `battles/{bid}` dan membuka `BattleActivity` dengan `EXTRA_BATTLE_ID`.
- Penyerang selalu tim A. Hasil dihitung ulang secara deterministik dari seed; hanya hasil simulasi lokal yang boleh ditulis, tepat satu kali, oleh `aUid`.
- Rating ELO K 32/24/16 (bot K 8), bonus streak mulai 3 kemenangan, musim `sYYYY_MM` (UTC), papan peringkat `rankings/{season}/arena` (top 20 di UI).
- Tiket 5 maksimum, regen 30 menit, waktu dihitung client dari `arena/ticketAt`.
- Pembela tidak perlu online: hasil masuk ke `players/{bUid}/arenaInbox/{bid}`, lalu diverifikasi ulang client pembela terhadap node `battles/{bid}` sebelum ratingnya diubah (ditandai `arena/defended/{bid}` agar idempoten).
- Batas fase 1 (disadari): settlement tetap dijalankan client penyerang dan hanya dijaga `ArenaResultGuard` (winner konsisten dengan HP, jumlah tick wajar, hash ada). Sebelum reward nyata, pindahkan verifikasi ke Cloud Functions.
- Emulator pengembangan saat ini gagal handshake TLS ke Firebase (SSL scanning Avast memotong sertifikat), sehingga alur jaringan hanya terverifikasi lewat unit test dan state UI; uji dua perangkat diperlukan di jaringan bersih.

Catatan M8 (Sosial, fase 1 tanpa Cloud Functions):
- Tiga layar baru: **Mail** (seed sambutan + event, klaim lampiran idempoten, hapus kedaluwarsa), **Teman** (cari lewat `directory/{uid}`, request dua arah, hadiah 50 coin 1×/hari, maks 50 teman), dan **Guild** (buat 200.000 coin, gabung/keluar, anggota, chat 50 pesan, donasi 3×/hari, boss mingguan).
- Tombol Mail/Teman/Guild ditambahkan di Home; `publishDirectory()` dipanggil otomatis agar pemain bisa ditemukan tanpa menebak UID.
- Boss guild bertarung lewat `BattleActivity` (`EXTRA_GUILD_ID`): damage dihitung dari sisa HP boss pada simulasi, dibatasi 25% `hpMax`, satu serangan per anggota per hari, lalu reward coin guild/poin diklaim sekali per minggu.
- Klaim (mail/teman/boss) memakai penanda di dalam transaksi `players/{uid}`; donasi dan serangan boss memakai transaksi terpisah dengan rollback.
- Batas fase 1: rules hanya membatasi besar damage/frekuensi, bukan memvalidasi simulasi boss; join guild tertutup belum bisa dicegah penuh; presence dan notifikasi sosial menyusul di M9.

Catatan M9 (Meta harian, fase 1 tanpa Cloud Functions):
- Tiga layar baru: **Misi & Hadiah** (login 7 hari beruntun, idle chest, misi harian/mingguan), **Profil** (statistik, gelar, badge), dan **Ranking** (top 20 arena musim berjalan + posisi sendiri).
- Login harian: siklus 7 hari, hadiah makin besar (coin/diamond/energy/tiket), putus sehari → kembali ke hari 1; klaim sekali per hari UTC lewat transaksi `meta/login`.
- Idle chest: 5 coin/menit, 1 energy tiap 30 menit, minimal 10 menit, cap 8 jam + 6 menit/level (maks 24 jam); sisa detik dipertahankan saat klaim.
- Misi harian: menang 3×, masuk Arena 1×, summon 1×, donasi guild 1×. Misi mingguan: menang 15×, menang Arena 5×, serang boss guild 3×. Progres ditulis sebagai efek samping aksi yang sudah tervalidasi (battle, arena, summon, donasi), bukan dari input bebas client.
- Idempoten: penanda klaim `meta/claims/{periode}_{missionId}`, `meta/login/lastDay`, `meta/idle/at`, dan progres per periode (`meta/missions/d/{yyyy-MM-dd}`, `meta/missions/w/{weekKey}`) — periode baru otomatis mulai dari 0 tanpa job reset.
- Profil menampilkan gelar/badge yang bisa diganti; daftar terkunci mengikuti statistik nyata (kemenangan arena, rating, level, roster mitos, serangan boss, guild).
- Ranking memakai papan `rankings/{season}/arena` yang sudah ditulis arena; UI memuat top 20 dan membaca `rankings/{season}/arena/{uid}` untuk posisi sendiri. Di luar top 20, posisi pasti tidak bisa dihitung tanpa Functions, jadi UI menampilkan skor tanpa nomor.
- Batas fase 1: progres misi dan reward tetap ditulis client dalam transaksi; validasi server penuh menyusul bersama migrasi Cloud Functions.
