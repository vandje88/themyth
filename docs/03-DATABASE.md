# 03 — DATABASE ARCHITECTURE (Firebase Realtime Database)

Project: `lima-sekawan` · URL: `https://lima-sekawan-default-rtdb.firebaseio.com`

---

## 1. PRINSIP

1. **Pisahkan statis dan dinamis.** `static/` = konten game (read-only untuk client), `players/` = data pemain.
2. **Denormalisasi sadar.** RTDB tidak punya JOIN; data yang sering dibaca bersama disimpan bersama (snapshot formasi di arena).
3. **Tulis kecil.** Update satu field, jangan tulis ulang seluruh node.
4. **Idempoten.** Semua klaim/pembelian memakai `requestId`.
5. **Server time.** Gunakan `ServerValue.TIMESTAMP` untuk timestamp transaksi.
6. **Batasi kedalaman.** Maks 4 level di bawah `players/{uid}` untuk data yang sering dibaca.
7. **Tanpa Storage.** Semua path aset (`spriteKey`, `portraitKey`) merujuk ke resource APK.

---

## 2. POHON DATABASE

```
meta/
  version, maintenance, minAppVersion, season, notices
static/
  characters/{charId}
  skills/{skillId}
  items/{itemId}
  equipment/{equipId}
  gachaPools/{poolId}
  shops/{shopId}
  stages/{stageId}
  missions/{missionId}
  achievements/{achievementId}
  badges/{badgeId}
  titles/{titleId}
  avatars/{avatarId}
  events/{eventId}
  elementMatrix, constants
players/{uid}/
  profile, settings, currencies, roster, formation, inventory,
  equipment, skills, progress, missions, achievements, badges,
  titles, avatars, shopLimits, summons, charShards, idle, stats, guild, friends,
  mailIndex, presence, flags
battles/{battleId}/
battleLogs/{uid}/{battleId}/
arena/
  snapshots/{uid}
  defenses/{uid}
matchmaking/{mode}/{bracket}/{uid}
rankings/{season}/{mode}/{uid}
guilds/{guildId}/
  info, members, chat, missions, boss, war, logs
guildIndex/{guildNameLower}: guildId
friends/{uid}/list/{friendUid}, requests/{fromUid}
mail/{uid}/{mailId}
presence/{uid}
requests/{uid}/{requestId}
admin/
  bans/{uid}, announcements/{id}, config, audit/{id}
```

---

## 3. STATIC DATA

### 3.1 `static/characters/{charId}`
```json
{
  "id": "aruna",
  "name": "Aruna",
  "title": "Bara Senja",
  "element": "fire",
  "role": "assassin",
  "rarity": "legendary",
  "portraitKey": "char_aruna_portrait",
  "spriteKey": "char_aruna_atlas",
  "base": {
    "hp": 4200, "atk": 420, "def": 180, "spd": 128,
    "critRate": 1800, "critDmg": 17500, "acc": 300, "eva": 900, "res": 500
  },
  "growth": {
    "hp": 168, "atk": 17.2, "def": 7.2, "spd": 0.8
  },
  "skills": {
    "basic": "aruna_basic",
    "skill1": "aruna_s1",
    "skill2": "aruna_s2",
    "ultimate": "aruna_ult",
    "passive": "aruna_passive",
    "awakening": ["aruna_aw1", "aruna_aw2", "aruna_aw3", "aruna_aw4"]
  },
  "maxLevel": 80,
  "releasePatch": 1
}
```

### 3.2 `static/skills/{skillId}`
```json
{
  "id": "aruna_ult",
  "name": "Fajar Terakhir",
  "type": "ULTIMATE",
  "target": "LOWEST_HP_ENEMY",
  "power": 42000,
  "hits": 1,
  "element": "fire",
  "cooldown": 0,
  "energyCost": 100,
  "effect": { "id": "burn", "chance": 8000, "duration": 2, "value": 500 },
  "vfx": "vfx_aruna_ult",
  "sfx": "sfx_ult_fire"
}
```

### 3.3 Item, equipment, stage, mission
```json
{
  "static/items/expscroll_m": {
    "id": "expscroll_m", "name": "EXP Scroll (M)", "category": "EXP_MATERIAL",
    "rarity": "rare", "stack": 999, "expValue": 1200, "sellPrice": 300,
    "iconKey": "item_exp_m", "desc": "Menambah 1.200 EXP karakter."
  },
  "static/items/shard_aruna": {
    "id": "shard_aruna", "name": "Aruna Shard", "category": "CHARACTER_SHARD",
    "rarity": "legendary", "stack": 999, "charId": "aruna", "sellPrice": 0,
    "iconKey": "item_shard_aruna", "desc": "60 shard untuk bintang Aruna."
  }
}
```
```json
{
  "static/equipment/eq_flame_blade": {
    "id": "eq_flame_blade", "name": "Flame Blade", "slot": "WEAPON",
    "rarity": "epic", "level": 1, "maxLevel": 15,
    "mainStat": { "key": "atk", "value": 180, "perLevel": 7 },
    "subStats": [
      { "key": "critRate", "min": 200, "max": 600 },
      { "key": "critDmg", "min": 800, "max": 2400 }
    ],
    "setId": "set_flame", "iconKey": "eq_flame_blade"
  },
  "static/equipmentSets/set_flame": {
    "id": "set_flame", "name": "Flame Set",
    "bonus2": { "atkPct": 800 }, "bonus4": { "elemPower": 1200 }
  }
}
```
```json
{
  "static/stages/1_1": {
    "id": "1_1", "chapter": 1, "index": 1, "name": "Gerbang Karang Agni",
    "energy": 6, "enemies": ["char_kabut_1", "char_kabut_2", "char_kabut_3"],
    "enemyLevel": 2, "firstClear": { "diamond": 30, "coin": 500 },
    "dropTable": "dt_stage_1"
  },
  "static/dropTables/dt_stage_1": [
    { "itemId": "expscroll_s", "min": 1, "max": 2, "chance": 6000 },
    { "itemId": "eq_iron_blade", "min": 1, "max": 1, "chance": 500 }
  ]
}
```

### 3.4 Gacha pool (dibundel di APK: `assets/seed/gacha.json`)
```json
{
  "id": "premium",
  "name": "Pemanggilan Bara Senja",
  "nameEn": "Twilight Ember Summon",
  "desc": "Aruna, Bara Senja, muncul lebih sering...",
  "costDiamond1": 350, "costDiamond10": 3150,
  "costTicket1": 1, "costTicket10": 10,
  "rates": {
    "common": 4500, "uncommon": 3000, "rare": 1500,
    "epic": 750, "legendary": 220, "mythic": 30
  },
  "pityEpic": 20, "pityLegendary": 80, "guaranteeRareIn10": true,
  "rateUpCharId": "aruna"
}
```
Aturan yang dipegang client (`GachaRoller`): pity menaikkan rarity ke epic pada hitungan ke-20 dan legendary pada ke-80, jaminan rare+ di pull ke-10, dan `rateUpCharId` mendapat 50% peluang saat rarity-nya (legendary+) keluar. Duplikat dikonversi ke shard: common 10, uncommon 15, rare 25, epic 40, legendary 60, mythic 100.

### 3.5 Shop offer (dibundel di APK: `assets/seed/shop.json`)
```json
{
  "id": "o_ticket", "itemId": "ticketSummon", "qty": 1,
  "currency": "diamond", "price": 150, "dailyLimit": 3
}
```
`currency` hanya `coin` atau `diamond`. Bila `itemId` adalah mata uang (`ticketSummon`, `ticketPvp`, `ticketDungeon`, `energy`), pembelian menambah node `currencies`; selain itu menambah `inventory/{itemId}/qty`.

### 3.6 `static/constants`
```json
{
  "maxAccountLevel": 100,
  "maxCharLevel": 80,
  "energyCap": 120,
  "energyRegenSeconds": 360,
  "idleCapHours": 12,
  "battleVersion": 1,
  "dailyResetHour": 4,
  "seasonId": "s1",
  "ratesVersion": 3
}
```

> **Update konten:** naive way = ubah node statis manual di Console. Semua client membaca `meta/version` saat login; jika berubah, tampilkan "konten diperbarui". Tidak perlu Storage.

---

## 4. PLAYER DATA

### 4.1 Contoh lengkap `players/{uid}`
```json
{
  "profile": {
    "uid": "Xk29sd...",
    "username": "PemanggilNama",
    "usernameLower": "pemanggilnama",
    "avatarId": "av_default_1",
    "frameId": "frame_bronze",
    "level": 12, "exp": 3400,
    "rank": "silver_2", "rating": 1285,
    "cpBest": 18400, "cpCurrent": 17620,
    "totalBattles": 84, "wins": 51, "losses": 33,
    "highestRank": "gold_1",
    "favoriteCharId": "aruna",
    "titleId": "rookie_myth",
    "badges": ["first_victory", "collector_10", "arena_50"],
    "guildId": "g_ab12",
    "createdAt": 1731000000000,
    "lastOnline": 1731300000000,
    "country": "ID"
  },
  "settings": {
    "language": "id", "bgm": 70, "sfx": 80, "battleSpeed": 2,
    "notifications": true, "strategy": "BALANCED"
  },
  "currencies": {
    "coin": 152300, "diamond": 820, "energy": 84,
    "ticketSummon": 3, "ticketPvp": 4, "ticketDungeon": 2,
    "arenaPoint": 640, "guildCoin": 120, "eventCoin": 0
  },
  "energyUpdatedAt": 1731300000000,
  "roster": {
    "aruna": {
      "charId": "aruna", "level": 42, "exp": 12500,
      "stars": 3, "awakening": 1, "breakthrough": 2,
      "skillLevels": { "basic": 3, "skill1": 3, "skill2": 2, "ultimate": 2 },
      "equipment": {
        "WEAPON": "eqinst_88a1", "ARMOR": "eqinst_91b2",
        "HELMET": null, "GLOVES": null, "BOOTS": null, "ACCESSORY": null
      },
      "locked": true, "obtainedAt": 1731010000000
    }
  },
  "formation": {
    "slots": ["aruna", "wira_bumi", "samudra_wening"],
    "strategy": "FOCUS_BACKLINE",
    "updatedAt": 1731290000000,
    "cp": 17620
  },
  "inventory": {
    "expscroll_m": { "qty": 24, "locked": false },
    "shard_aruna": { "qty": 35, "locked": true },
    "ticket_summon": { "qty": 3, "locked": false }
  },
  "equipment": {
    "eqinst_88a1": {
      "instanceId": "eqinst_88a1", "templateId": "eq_flame_blade",
      "level": 7, "rarity": "epic",
      "mainStat": { "key": "atk", "value": 229 },
      "subStats": [
        { "key": "critRate", "value": 420 },
        { "key": "critDmg", "value": 1410 }
      ],
      "equippedBy": "aruna", "slot": "WEAPON", "locked": true
    }
  },
  "progress": {
    "chapter": 3, "stage": 7, "highestStage": "3_7",
    "towerFloor": 18, "maxTowerFloor": 18,
    "dungeonClears": { "exp_mon": 2, "exp_tue": 1 },
    "stageStars": { "1_1": 3, "1_2": 3, "2_1": 2 }
  },
  "missions": {
    "daily": {
      "resetAt": 1731340800000,
      "items": {
        "win_3_battle": { "progress": 2, "target": 3, "claimed": false },
        "summon_1": { "progress": 1, "target": 1, "claimed": true }
      }
    },
    "weekly": { "resetAt": 1731600000000, "items": { } },
    "main": { "chapter_1": { "progress": 12, "target": 12, "claimed": true } }
  },
  "achievements": { "total_battles_100": { "progress": 84, "claimed": false } },
  "badges": { "first_victory": { "unlockedAt": 1731018000000, "seen": true } },
  "titles": { "rookie_myth": { "unlockedAt": 1731018000000 } },
  "avatars": { "av_default_1": { "unlockedAt": 1731000000000 } },
  "shopLimits": {
    "o_exp_s": { "count": 2, "resetAt": 1731340800000 },
    "o_ticket": { "count": 3, "resetAt": 1731340800000 }
  },
  "charShards": {
    "nyi_tirta": 35,
    "aruna": 60
  },
  "summons": {
    "normal": {
      "epicPity": 7, "legendaryPity": 31,
      "totalPulls": 49, "lastPullAt": 1731290000000,
      "history": {
        "p_1731290000000_00": {
          "charId": "nyi_tirta", "rarity": "rare",
          "isNew": false, "at": 1731290000000
        }
      }
    },
    "premium": {
      "epicPity": 3, "legendaryPity": 12,
      "totalPulls": 68, "lastPullAt": 1731295000000,
      "history": { }
    }
  },
  "idle": {
    "lastClaimAt": 1731295000000,
    "stageIndex": 34,
    "pendingCoin": 0
  },
  "stats": {
    "damageDealt": 1840000, "healingDone": 320000,
    "arenaWins": 22, "arenaLosses": 11, "streak": 3,
    "lastDailyLoginAt": 1731280000000, "loginStreak": 6
  },
  "guild": { "id": "g_ab12", "role": "member", "joinedAt": 1731100000000, "weeklyPoints": 240 },
  "flags": { "tutorialDone": true, "starterClaimed": true, "banned": false }
}
```

**Catatan:** `roster` adalah sumber kebenaran karakter yang dimiliki; `formation` hanya referensi `charId`.
Item stack disimpan sebagai map `{qty, locked}` agar update murah; item unik (equipment) di node terpisah.

### 4.2 Inventory dibatasi
- Maks 300 jenis item; maks 500 instance equipment.
- Jika penuh, equipment baru masuk mail (bukan hilang).

---

## 5. BATTLE & LOG

```json
{
  "battles/B20261110_ab12": {
    "id": "B20261110_ab12",
    "type": "ARENA",
    "mode": "ranked",
    "season": "s1",
    "aUid": "Xk29sd...",
    "bUid": "Yp82ld...",
    "aTeam": ["aruna", "wira_bumi", "samudra_wening"],
    "bTeam": ["kala_rudra", "raka_baja", "nyi_tirta"],
    "aSnapshot": { "cp": 17620, "levels": [42, 40, 38] },
    "seed": 73014444032,
    "engineVersion": 1,
    "status": "RESULT_SUBMITTED",
    "result": {
      "winner": 0, "turns": 41, "durationMs": 24600,
      "teamHpPct": { "a": 62, "b": 0 }, "mvp": "aruna",
      "hash": "b1c9f0e7a34d..."
    },
    "createdAt": 1731300000000,
    "resolvedAt": 1731300030000
  }
}
```
`battleLogs/{uid}/{battleId}` menyimpan `events[]` (maks 20 battle, auto-prune).

Aturan: `battles/{id}` **hanya bisa dibuat sekali**; `result` hanya bisa ditulis jika `status` masih `PENDING`; setelah `SETTLED` tidak bisa diubah.

---

## 6. ARENA, MATCHMAKING, RANKING

### 6.1 Snapshot lawan (`arena/snapshots/{uid}`)

Dibuat ulang setiap pemain membuka Arena atau mengganti formasi. Isinya sudah "matang tempur" (stat final + skill), sehingga penyerang tidak perlu membaca `players/{bUid}` (yang memang tertutup rules).

```json
{
  "arena/snapshots/Yp82ld...": {
    "uid": "Yp82ld...", "username": "Bayu", "avatar": "av_epic_2", "title": "elite_warrior",
    "cp": 17240, "rating": 1310, "strategy": "FOCUS_BACKLINE",
    "units": {
      "0": {
        "charId": "kala_rudra", "level": 44, "stars": 3,
        "stats": { "hp": 18770, "atk": 1290, "def": 817, "spd": 119, "critRate": 1200, "critDmg": 16000, "acc": 200, "eva": 700, "res": 600, "elemPower": 800 },
        "basic": "kala_basic", "skillOne": "kala_s1", "skillTwo": "kala_s2", "skillUltimate": "kala_ult"
      }
    },
    "updatedAt": 1731299990000
  }
}
```

`units` adalah map slot (`"0"`,`"1"`,`"2"`), bukan array, dan minimal berisi 3 unit sebelum snapshot dianggap layak tanding. Index wajib: `".indexOn": ["rating"]` pada `arena/snapshots`.

### 6.2 Matchmaking fase 1

Node `matchmaking/{mode}/{bracket}/{uid}` di pohon §2 masih **dicadangkan** dan belum dipakai. Fase 1 memakai query langsung ke snapshot:

1. `arena/snapshots.orderByChild("rating").startAt(rating - window).endAt(rating + window)`.
2. Jendela melebar 150 → 300 → 600 tiap 10 detik, total maksimum 60 detik.
3. Kandidat disaring `ArenaMatchmaker.pick` (bukan diri sendiri, rating terdekat, uid stabil sebagai tie-break).
4. Jika tidak ada kandidat sama sekali → bot `bot_<uid>` dengan rating & CP mendekati pemain (K = 8, tanpa inbox, tanpa entri ranking).

### 6.3 Battle (`battles/{bid}`)

```json
{
  "battles/B_1731300000000_ab12": {
    "aUid": "Xk29sd...", "bUid": "Yp82ld...", "mode": "ARENA",
    "seed": 73014444032, "createdAt": 1731300000000, "status": "SETTLED",
    "a": { "uid": "Xk29sd...", "cp": 17620, "rating": 1285, "units": { "0": { "charId": "aruna" } } },
    "b": { "uid": "Yp82ld...", "cp": 17240, "rating": 1310, "units": { "0": { "charId": "kala_rudra" } } },
    "result": {
      "winner": 0, "ticks": 412, "hpA": 62, "hpB": 0,
      "hash": "b1c9f0e7a34d...", "by": "Xk29sd...", "at": 1731300030000
    }
  }
}
```

- `bid` dibuat oleh penyerang (`push().getKey()`), dan hanya `aUid` yang boleh membuat node.
- `result` hanya bisa ditulis sekali (`!data.exists()`), hanya oleh `aUid`, dan hanya selama `status` masih `PENDING`.
- `status` hanya berpindah `PENDING` → `SETTLED`, ditulis oleh `aUid` bersamaan dengan `result`.
- `ticks` 1..900 (tick = 100 ms), `hpA`/`hpB` persen 0..100, `winner` -1 (draw) / 1 / 0 — tim A adalah penyerang.
- Snapshot tim disalin ke `a.units`/`b.units` saat create agar hasil bisa diaudit walau formasi berubah.

### 6.4 Rating pemain & papan peringkat

```json
{
  "players/Xk29sd.../arena": {
    "rating": 1310, "wins": 40, "losses": 18, "streak": 3, "points": 640,
    "tickets": 5, "ticketAt": 1731304800000,
    "settled": { "B_1731300000000_ab12": true },
    "defended": { "B_1731299000000_cd34": true }
  },
  "players/Yp82ld.../arenaInbox/B_1731300000000_ab12": {
    "winner": 0, "ratingDelta": -16, "by": "Xk29sd...", "at": 1731300030000, "rating": 1310
  },
  "rankings/2026_11/arena/Xk29sd...": {
    "uid": "Xk29sd...", "username": "PemanggilNama", "score": 1310,
    "wins": 40, "losses": 18, "cp": 17620, "updatedAt": 1731300030000
  }
}
```

- Musim = `sYYYY_MM` dalam UTC (contoh di atas disingkat `2026_11`). Papan dibaca `orderByChild("score").limitToLast(20)` dan ditampilkan terbalik (skor tertinggi di atas).
- `settled/{bid}` dan `defended/{bid}` adalah penanda idempoten: transaksi membaca penanda sebelum menambah rating/koin, sehingga settle yang di-retry tidak menggandakan reward.
- `tickets` maksimum 5; regen 1 tiket / 30 menit dihitung dari `ticketAt` saat transaksi tiket berjalan.
- `arenaInbox` ditulis penyerang ke node pembela **pada update multi-path yang sama** dengan `result` dan `rankings` (atomik di RTDB). Pembela memverifikasi setiap entri terhadap `battles/{bid}` (aUid, bUid, `result.by`, `result.winner`) sebelum menerapkan `ratingDelta`, lalu menghapus entri dan menulis `defended/{bid}`. Entri yang tidak valid diabaikan dan dihapus.
- Inbox dibatasi maksimum 8 entri per pembela (validasi rules); pembela yang jarang online tetap memproses antrean terlama lebih dulu.

### 6.5 Settlement atomik (fase 1)

`ArenaRepository.settleBattle` menulis satu `root().updateChildren(...)` berisi:

```
battles/{bid}/result       (write-once)
battles/{bid}/status       = "SETTLED"
rankings/{season}/arena/{uid}
players/{bUid}/arenaInbox/{bid}   (hanya lawan manusia)
```

Rating, koin, poin, dan tiket pemain sendiri ditulis lebih dulu lewat satu transaksi pada `players/{uid}` (dengan penanda `arena/settled/{bid}`). Jika update multi-path gagal karena jaringan, retry aman: transaksi melihat penanda dan tidak menambah reward lagi, lalu update multi-path diulang.

> Rules hanya menjaga bentuk data (rentang nilai, sekali tulis, kepemilikan). Validasi bahwa hasil pertandingan memang layak tetap ada di client (`ArenaResultGuard`) sampai Cloud Functions tersedia — lihat `04-BACKEND-API.md` §4.1.

---

## 7. GUILD

```json
{
  "guilds/g_ab12": {
    "info": {
      "id": "g_ab12", "name": "Arunika", "nameLower": "arunika",
      "tag": "ARN", "level": 5, "exp": 12400,
      "leaderUid": "Xk29sd...", "memberCount": 12, "maxMembers": 30,
      "minLevel": 10, "open": true, "notice": "Aktif malam",
      "createdAt": 1731100000000, "region": "ID"
    },
    "members": {
      "Xk29sd...": { "role": "leader", "joinedAt": 1731100000000, "weeklyPoints": 340, "lastActive": 1731299000000 },
      "Yp82ld...": { "role": "officer", "joinedAt": 1731110000000, "weeklyPoints": 220, "lastActive": 1731298000000 }
    },
    "chat": {
      "m_1731299000000_Xk29sd": { "uid": "Xk29sd...", "name": "PemanggilNama", "text": "Boss jam 8 ya", "at": 1731299000000 }
    },
    "boss": {
      "bossId": "boss_kabut_1", "weekStart": 1731283200000,
      "hpMax": 5000000, "hpLeft": 3120000,
      "damage": { "Xk29sd...": 420000, "Yp82ld...": 310000 }
    },
    "missions": { "donate_10": { "progress": 6, "target": 10 } },
    "war": { "active": false, "opponentGuildId": null, "score": 0 }
  },
  "guildIndex/arunika": "g_ab12"
}
```
Chat dibatasi 100 pesan terakhir; client memangkas dengan `limitToLast(100)`.
Boss HP dikurangi lewat **transaksi**.

---

## 8. FRIEND, MAIL, PRESENCE

```json
{
  "friends/Xk29sd...": {
    "list": {
      "Yp82ld...": { "since": 1731200000000, "lastGiftAt": 1731280000000, "name": "Bayu" }
    },
    "requests": {
      "Zp11aa...": { "fromUid": "Zp11aa...", "name": "Citra", "at": 1731290000000 }
    }
  },
  "mail/Xk29sd.../m_1731290000000": {
    "id": "m_1731290000000", "type": "REWARD", "title": "Hadiah Arena",
    "body": "Terima kasih telah bertarung!", "attachments": [
      { "type": "currency", "id": "diamond", "qty": 100 },
      { "type": "item", "id": "ticket_summon", "qty": 1 }
    ],
    "createdAt": 1731290000000, "expiresAt": 1733882000000,
    "read": false, "claimed": false
  },
  "presence/Xk29sd...": { "online": true, "lastSeen": 1731300000000, "state": "lobby" }
}
```

Presence memakai:
```java
presenceRef.onDisconnect().setValue(offlineMap);
```

---

## 9. EVENT, SHOP LIMIT, SUMMON

```json
{
  "static/events/ev_summer_1": {
    "id": "ev_summer_1", "name": "Bara Musim Panas",
    "startsAt": 1731600000000, "endsAt": 1732204800000,
    "currency": "eventCoin", "bannerKey": "banner_summer_1",
    "featuredCharId": "rani_puspa",
    "missions": ["ev_win_10", "ev_pull_20"],
    "shopId": "event_summer_1"
  },
  "players/Xk29sd.../shopLimits/o_exp_s": {
    "count": 2, "resetAt": 1731340800000
  },
  "players/Xk29sd.../summons/normal/history/p_1731290000000_00": {
    "charId": "nyi_tirta", "rarity": "rare", "isNew": false,
    "at": 1731290000000
  },
  "players/Xk29sd.../charShards/nyi_tirta": 35
}
```

### 9.1 Summon (gacha)
- Satu pull = satu transaksi pada **`players/{uid}`**: memotong `currencies/diamond` atau `currencies/ticketSummon`, menulis `roster/{charId}` (unit baru) atau menambah `charShards/{charId}` (duplikat), memperbarui pity, lalu menambah `history`.
- Hasil disimpan satu entri per karakter dengan key `p_{epochMillis}_{index}` (contoh `p_1731290000000_00`), field `charId`, `rarity`, `isNew`, `at`.
- `history` dipangkas client ke **100 entri terakhir** per pool; UI hanya membaca 30 terbaru. Rules membolehkan penghapusan (`null`) agar pruning tidak ditolak.
- `epicPity` maksimum 20, `legendaryPity` maksimum 80 (divalidasi rules). Keduanya mereset saat rarity terkait keluar.
- Karena RNG berjalan di client, nilai diamond yang bisa dicurangi tetap ada; sebelum gacha berbayar nyata, pindahkan roll ke Cloud Functions (lihat §14).

### 9.2 Shop
- `shopLimits/{offerId}` menyimpan `count` (pembelian hari ini) dan `resetAt` (epoch ms).
- Reset harian pukul **04:00 waktu lokal perangkat**; jika `resetAt` sudah lewat, transaksi mengembalikan `count` ke 0 dan menulis `resetAt` berikutnya.
- Batas beli (`dailyLimit`), harga, dan isi paket berasal dari `assets/seed/shop.json`, bukan dari RTDB, sehingga server tidak bisa memvalidasi harga tanpa Functions. Untuk produksi, salin `shop.json` ke `static/shops/` dan baca dari sana.

---

## 10. IDEMPOTENCY

```json
{
  "requests/Xk29sd.../req_8f21": {
    "type": "CLAIM_DAILY",
    "status": "DONE",
    "result": { "coin": 500, "diamond": 20 },
    "createdAt": 1731300000000
  }
}
```
Pola: client membuat `requestId` UUID; transaksi memeriksa `requests/{uid}/{id}/status`. Jika sudah `DONE`, kembalikan hasil lama. Rules melarang menulis ulang `DONE`.

---

## 11. SECURITY RULES (CONTOH PRODUKSI AWAL)

> Simpan di Firebase Console → Realtime Database → Rules. Ini versi yang cukup ketat untuk mencegah kerusakan data, **bukan** anti-cheat penuh (lihat §14).

```json
{
  "rules": {
    ".read": false,
    ".write": false,

    "meta": {
      ".read": true
    },
    "static": {
      ".read": true
    },

    "players": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid",

        "profile": {
          "uid": { ".validate": "newData.val() === $uid" },
          "createdAt": { ".validate": "!data.exists() || newData.val() === data.val()" },
          "username": { ".validate": "newData.isString() && newData.val().length >= 3 && newData.val().length <= 16" },
          "usernameLower": { ".validate": "newData.val() === newData.parent().child('username').val().toLowerCase()" },
          "level": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 100" },
          "rating": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5000" },
          "wins": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "losses": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "guildId": { ".validate": "newData.isString() || newData.val() === null" },
          "lastOnline": { ".validate": "newData.isNumber()" },
          "$other": { ".validate": true }
        },

        "currencies": {
          "coin": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 999999999" },
          "diamond": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 9999999" },
          "energy": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 999" },
          "ticketSummon": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 999" },
          "ticketPvp": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 99" },
          "$other": { ".validate": "newData.isNumber() && newData.val() >= 0" }
        },

        "roster": {
          "$charId": {
            "level": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 80" },
            "stars": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 6" },
            "awakening": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 4" },
            "breakthrough": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 4" },
            "skillLevels": {
              "$skill": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 5" }
            },
            "$other": { ".validate": true }
          }
        },

        "inventory": {
          "$itemId": {
            "qty": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 99999" }
          }
        },

        "formation": {
          "slots": { ".validate": "newData.hasChildren(['0','1','2'])" },
          "cp": { ".validate": "newData.isNumber() && newData.val() >= 0" }
        },

        "summons": {
          "$poolId": {
            "epicPity": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 20" },
            "legendaryPity": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 80" },
            "totalPulls": { ".validate": "newData.isNumber() && newData.val() >= 0" },
            "lastPullAt": { ".validate": "newData.isNumber()" },
            "history": {
              "$pullId": {
                ".validate": "newData.val() === null || (newData.hasChildren(['charId','rarity','at']) && newData.child('at').isNumber())"
              }
            },
            "$other": { ".validate": true }
          }
        },

        "charShards": {
          "$charId": { ".validate": "newData.isNumber() && newData.val() >= 0" }
        },

        "shopLimits": {
          "$offerId": {
            "count": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 999" },
            "resetAt": { ".validate": "newData.isNumber() && newData.val() >= 0" },
            "$other": { ".validate": true }
          }
        },

        "guild": { ".read": true },
        "mailIndex": { ".read": true },
        "requests": { "$reqId": { ".validate": "newData.child('status').val() === 'DONE'" } },

        "arena": {
          "rating": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5000" },
          "wins": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "losses": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "streak": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 99" },
          "points": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "tickets": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5" },
          "ticketAt": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "settled": { "$bid": { ".validate": "newData.val() === true" } },
          "defended": { "$bid": { ".validate": "newData.val() === true" } },
          "$other": { ".validate": true }
        },

        "arenaInbox": {
          "$bid": {
            ".write": "auth != null && (!data.exists() || data.child('by').val() === auth.uid)",
            ".validate": "newData.hasChildren(['winner','ratingDelta','by','at']) && newData.child('by').val() === auth.uid && newData.numChildren() <= 8",
            "winner": { ".validate": "newData.isNumber() && newData.val() >= -1 && newData.val() <= 1" },
            "ratingDelta": { ".validate": "newData.isNumber() && newData.val() >= -200 && newData.val() <= 200" },
            "by": { ".validate": "newData.isString() && newData.val().length >= 1" },
            "at": { ".validate": "newData.isNumber() && newData.val() >= 0" },
            "rating": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5000" },
            "$other": { ".validate": true }
          }
        },

        "$other": { ".validate": true }
      }
    },

    "guilds": {
      ".read": "auth != null",
      "$gid": {
        "info": {
          ".write": "auth != null && (root.child('guilds').child($gid).child('members').child(auth.uid).exists() || !data.exists())",
          "memberCount": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 30" },
          "level": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 30" }
        },
        "members": {
          ".write": "auth != null && root.child('players').child(auth.uid).child('guild').child('id').val() === $gid",
          "$memberUid": {
            "role": { ".validate": "newData.val() === 'leader' || newData.val() === 'officer' || newData.val() === 'member'" }
          }
        },
        "chat": {
          ".read": "auth != null && root.child('guilds').child($gid).child('members').child(auth.uid).exists()",
          ".write": "auth != null && root.child('guilds').child($gid).child('members').child(auth.uid).exists()",
          "$msgId": {
            ".validate": "newData.hasChildren(['uid','text','at']) && newData.child('uid').val() === auth.uid && newData.child('text').val().length <= 200"
          }
        },
        "boss": {
          "hpLeft": { ".validate": "newData.isNumber() && newData.val() >= 0" }
        }
      }
    },

    "battles": {
      "$bid": {
        ".read": "auth != null && (data.child('aUid').val() === auth.uid || data.child('bUid').val() === auth.uid)",
        ".write": "auth != null && !data.exists()",
        "aUid": { ".validate": "newData.val() === auth.uid" },
        "bUid": { ".validate": "newData.isString() && newData.val().length >= 1 && newData.val() !== auth.uid" },
        "mode": { ".validate": "newData.val() === 'ARENA' || newData.val() === 'PVP'" },
        "seed": { ".validate": "newData.isNumber()" },
        "createdAt": { ".validate": "newData.isNumber()" },
        "status": {
          ".write": "auth != null && root.child('battles').child($bid).child('aUid').val() === auth.uid && (!data.exists() || data.val() === 'PENDING')",
          ".validate": "newData.val() === 'PENDING' || newData.val() === 'SETTLED'"
        },
        "result": {
          ".write": "auth != null && !data.exists() && root.child('battles').child($bid).child('aUid').val() === auth.uid",
          ".validate": "newData.hasChildren(['winner','ticks','hpA','hpB','hash','by','at']) && newData.numChildren() <= 8",
          "winner": { ".validate": "newData.isNumber() && newData.val() >= -1 && newData.val() <= 1" },
          "ticks": { ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 900" },
          "hpA": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 100" },
          "hpB": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 100" },
          "hash": { ".validate": "newData.isString()" },
          "by": { ".validate": "newData.val() === root.child('battles').child($bid).child('aUid').val()" },
          "at": { ".validate": "newData.isNumber()" },
          "$other": { ".validate": true }
        },
        "a": {
          ".validate": "newData.hasChildren(['uid','units','cp','rating'])",
          "uid": { ".validate": "newData.val() === root.child('battles').child($bid).child('aUid').val()" },
          "units": { ".validate": "newData.hasChildren() && newData.numChildren() <= 3" },
          "$other": { ".validate": true }
        },
        "b": {
          ".validate": "newData.hasChildren(['uid','units','cp','rating'])",
          "uid": { ".validate": "newData.val() === root.child('battles').child($bid).child('bUid').val()" },
          "units": { ".validate": "newData.hasChildren() && newData.numChildren() <= 3" },
          "$other": { ".validate": true }
        },
        "$other": { ".validate": true }
      }
    },

    "battleLogs": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid"
      }
    },

    "arena": {
      "snapshots": {
        ".read": "auth != null",
        ".indexOn": ["rating"],
        "$uid": {
          ".write": "auth != null && auth.uid === $uid",
          ".validate": "newData.hasChildren(['uid','username','cp','rating','units','updatedAt']) && newData.child('uid').val() === $uid",
          "uid": { ".validate": "newData.val() === $uid" },
          "username": { ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 16" },
          "cp": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "rating": { ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 5000" },
          "updatedAt": { ".validate": "newData.isNumber() && newData.val() >= 0" },
          "units": { ".validate": "newData.hasChildren() && newData.numChildren() <= 3" },
          "$other": { ".validate": true }
        }
      }
    },

    "matchmaking": {
      "$mode": {
        "$bracket": {
          ".read": "auth != null",
          "$uid": {
            ".write": "auth != null && auth.uid === $uid",
            "status": { ".validate": "newData.val() === 'SEARCHING' || newData.val() === 'MATCHED'" }
          }
        }
      }
    },

    "rankings": {
      "$season": {
        "$mode": {
          ".read": "auth != null",
          ".indexOn": ["score"],
          "$uid": {
            ".write": "auth != null && auth.uid === $uid",
            "score": { ".validate": "newData.isNumber() && newData.val() >= 0" }
          }
        }
      }
    },

    "friends": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid"
      }
    },

    "mail": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid",
        "$mailId": {
          "claimed": { ".validate": "newData.isBoolean()" },
          "read": { ".validate": "newData.isBoolean()" },
          "$other": { ".validate": true }
        }
      }
    },

    "presence": {
      ".read": "auth != null",
      "$uid": { ".write": "auth != null && auth.uid === $uid" }
    },

    "requests": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid",
        "$reqId": {
          "status": { ".validate": "newData.val() === 'PENDING' || newData.val() === 'DONE'" }
        }
      }
    },

    "admin": {
      ".read": "auth != null && root.child('admin').child('admins').child(auth.uid).val() === true",
      ".write": "auth != null && root.child('admin').child('admins').child(auth.uid).val() === true"
    }
  }
}
```

**File siap deploy:** [`database.rules.json`](../database.rules.json) di root project adalah versi otoritatif; blok di atas diringkas agar mudah dibaca, dan file JSON tersebut sudah memuat detail tambahan (bootstrap, progression, gacha, shop, arena, inbox). Deploy dengan `firebase use lima-sekawan` lalu `firebase deploy --only database`.

**Index yang disarankan** (menu Rules → tambahkan `".indexOn"`):
| Path | Index |
|---|---|
| `rankings/$season/$mode` | `score` |
| `arena/snapshots` | `rating` (wajib untuk matchmaking fase 1) |
| `matchmaking/arena/$bracket` | `rating` (cadangan, belum dipakai) |
| `guildIndex` | (key lookup) |
| `presence` | `lastSeen` |
| `mail/$uid` | `createdAt` |

> Rules di atas **tidak bisa** memvalidasi apakah player benar-benar menang. Karena itu hadiah rank tidak boleh langsung diberikan oleh client. Lihat `04-BACKEND-API.md` §6.

---

## 12. TRANSAKSI

### 12.1 Belanja coin (client-side, aman dari race)
```java
DatabaseReference coinRef = db.getReference("players/" + uid + "/currencies/coin");
coinRef.runTransaction(new Transaction.Handler() {
    @Override public Transaction.Result doTransaction(MutableData data) {
        Long coin = data.getValue(Long.class);
        if (coin == null || coin < price) return Transaction.abort();
        data.setValue(coin - price);
        return Transaction.success(data);
    }
    @Override public void onComplete(DatabaseError e, boolean committed, DataSnapshot s) { }
});
```

### 12.2 Klaim hadiah (idempoten)
1. `runTransaction` pada `requests/{uid}/{reqId}/status`: jika `DONE` → batal (sudah diklaim).
2. Transaksi pada `mail/{uid}/{mailId}/claimed` → `true`.
3. Transaksi pada `currencies` menambah reward.
4. Set `requests/.../status = DONE`.

Jika langkah 3 gagal, langkah 4 tidak dijalankan; retry dengan `reqId` sama akan melanjutkan. Ini **bukan** atomik lintas node (RTDB tidak punya multi-path transaction), jadi desain reward idempoten + rekonsiliasi saat login.

### 12.3 Guild boss damage
```java
db.getReference("guilds/" + gid + "/boss").runTransaction(...) // kurangi hpLeft, tambah damage[uid]
```

---

## 13. RETENSI & KUOTA

| Data | Retensi | Alasan |
|---|---|---|
| `battleLogs` | 20 terakhir/pemain | Hemat kuota |
| `battles` | 7 hari (cleanup client/admin) | Audit singkat |
| `mail` | 30 hari / 100 item | Batas UX |
| `guild.chat` | 100 pesan | Batas UX |
| `summons.history` | 100 pull | Transparansi |
| `presence` | live | Reset saat offline |
| `rankings` | per season | Reset musiman |

Estimasi ukuran satu pemain aktif: ± 18–30 KB (dengan 20 karakter + 50 equipment). 10.000 pemain ≈ 250 MB — masih di bawah 1 GB (Spark) tetapi **koneksi** tetap batas utama (100).

---

## 14. BATAS KEAMANAN & JALAN KE SERVER-AUTHORITATIVE

| Masalah | Yang bisa dilakukan rules | Yang tidak bisa |
|---|---|---|
| Ubah coin jadi negatif | Ya (validate ≥ 0) | — |
| Ubah coin jadi 999 juta | Tidak bisa dicegah sepenuhnya | Butuh server |
| Klaim mail 2x | Bisa (idempoten + claimed) | — |
| Menulis hasil battle palsu | Bisa dicegah menulis 2x | Butuh validasi ulang |
| Naikkan level karakter | Bisa dibatasi ≤ 80 | Butuh cek material |
| Spam chat | Bisa batasi panjang | Rate limit butuh Functions |

**Fase 1 (sekarang, Spark):** rules + idempotency + audit plausibility (client melaporkan aktivitas; admin memantau anomali).
**Fase 2 (wajib sebelum ranking berhadiah nyata):** Cloud Functions (Blaze, masih ada kuota gratis) untuk:
- `onCreate(battles/{id})` → simulasi ulang, bandingkan hash, tulis hasil final.
- `onWrite(players/{uid}/requests)` → validasi klaim.
- `onCall` untuk purchase, summon (RNG di server), dan leaderboard.

Struktur database di atas **sudah kompatibel** dengan Fase 2: cukup menambahkan `admin/` dan memindahkan penulisan `result`/reward ke Function. Client tidak perlu diubah besar.

---

## 15. MIGRASI & VERSI

- `meta/schemaVersion` (integer). Client menolak jalan jika `schemaVersion` lebih baru dari yang didukung (paksa update).
- `static/*/releasePatch` untuk konten baru.
- `players/{uid}/flags/dataVersion` untuk migrasi data pemain satu kali.
- Back up ekspor JSON berkala dari Console sebelum perubahan rules besar.
