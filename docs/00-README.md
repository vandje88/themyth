# THE MYTH — Dokumentasi Desain & Arsitektur

**2D Anime Mitologi RPG · 3 VS 3 Idle Auto Battle · Android · Firebase**

Versi dokumen: 1.0 (2026-11-10)
Status kode: baseline template Android Studio, `assembleDebug` **SUKSES**.
Status dokumen: lengkap untuk fase desain; implementasi mengikuti roadmap di `05-TECHNICAL-ANDROID.md`.

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

Setelah dokumen ini disetujui:

1. M1 — Fondasi: tema, navigasi, Auth guest + Google, bootstrap player node.
2. M2 — Roster & formasi 3v3 + layar karakter.
3. M3 — BattleEngine + layar battle (offline/PvE) + replay.
4. M4 — Progression, inventory, equipment, gacha.
5. M5 — Arena async, ranking, guild, mail, event.
6. M6 — Polish, anti-cheat, load test, rilis.

Detail estimasi ada di `05-TECHNICAL-ANDROID.md` bagian Roadmap.
