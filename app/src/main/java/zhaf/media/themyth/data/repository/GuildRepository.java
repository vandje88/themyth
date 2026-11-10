package zhaf.media.themyth.data.repository;

import androidx.annotation.StringRes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.data.social.GuildBoss;
import zhaf.media.themyth.data.social.GuildBossLogic;
import zhaf.media.themyth.data.social.GuildChatMessage;
import zhaf.media.themyth.data.social.GuildInfo;
import zhaf.media.themyth.data.social.GuildLogic;
import zhaf.media.themyth.data.social.GuildMember;
import zhaf.media.themyth.data.social.GuildSummary;
import zhaf.media.themyth.data.social.SocialDay;

public final class GuildRepository {

    public interface MyGuildCallback {
        void onData(String gid, String role);

        void onError(DatabaseError error);
    }

    public interface InfoCallback {
        void onData(GuildInfo info);

        void onError(DatabaseError error);
    }

    public interface MembersCallback {
        void onData(List<GuildMember> members);

        void onError(DatabaseError error);
    }

    public interface ChatCallback {
        void onData(List<GuildChatMessage> messages);

        void onError(DatabaseError error);
    }

    public interface BossCallback {
        void onData(GuildBoss boss);

        void onError(DatabaseError error);
    }

    public interface DirectoryCallback {
        void onData(List<GuildSummary> guilds);

        void onError(DatabaseError error);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@StringRes int messageRes);
    }

    public interface AttackCallback {
        void onSuccess(int damage, int hpLeft);

        void onError(@StringRes int messageRes);
    }

    public interface RewardCallback {
        void onSuccess(int coin, int points);

        void onError(@StringRes int messageRes);
    }

    public static Handle observeMyGuild(String uid, MyGuildCallback callback) {
        DatabaseReference ref = FirebaseRefs.guildOf(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onData(snapshot.child("id").getValue(String.class),
                        snapshot.child("role").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static Handle observeInfo(String gid, InfoCallback callback) {
        DatabaseReference ref = FirebaseRefs.guild(gid).child("info");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GuildInfo info = GuildInfo.from(snapshot, gid);
                if (info != null) {
                    callback.onData(info);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static Handle observeMembers(String gid, MembersCallback callback) {
        DatabaseReference ref = FirebaseRefs.guild(gid).child("members");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<GuildMember> members = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GuildMember member = GuildMember.from(child, child.getKey());
                    if (member != null) {
                        members.add(member);
                    }
                }
                members.sort((left, right) -> {
                    int rank = GuildLogic.roleRank(right.role) - GuildLogic.roleRank(left.role);
                    return rank != 0 ? rank : Long.compare(left.joinedAt, right.joinedAt);
                });
                callback.onData(members);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public static Handle observeChat(String gid, ChatCallback callback) {
        Query query = FirebaseRefs.guild(gid).child("chat")
                .limitToLast(GuildLogic.CHAT_LIMIT);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<GuildChatMessage> messages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GuildChatMessage message = GuildChatMessage.from(child);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                callback.onData(messages);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        query.addValueEventListener(listener);
        return new Handle(query, listener);
    }

    public static Handle observeBoss(String gid, BossCallback callback) {
        DatabaseReference ref = FirebaseRefs.guild(gid).child("boss");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GuildBoss boss = GuildBoss.from(snapshot);
                if (boss != null) {
                    callback.onData(boss);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error);
            }
        };
        ref.addValueEventListener(listener);
        return new Handle(ref, listener);
    }

    public void loadBoss(String gid, BossCallback callback) {
        FirebaseRefs.guild(gid).child("boss")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        GuildBoss boss = GuildBoss.from(snapshot);
                        if (boss == null) {
                            callback.onError(null);
                        } else {
                            callback.onData(boss);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    public void loadDirectory(DirectoryCallback callback) {
        FirebaseRefs.guildDirectory()
                .orderByChild("memberCount")
                .limitToLast(30)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<GuildSummary> guilds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            GuildSummary summary = GuildSummary.from(child, child.getKey());
                            if (summary != null && !summary.gid.isEmpty()) {
                                guilds.add(summary);
                            }
                        }
                        guilds.sort((left, right) ->
                                Integer.compare(right.memberCount, left.memberCount));
                        callback.onData(guilds);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    public void create(String uid, String myName, int myLevel, String name, String tag,
                       ActionCallback callback) {
        if (!GuildLogic.validName(name)) {
            callback.onError(R.string.guild_error_name);
            return;
        }
        if (!GuildLogic.validTag(tag)) {
            callback.onError(R.string.guild_error_tag);
            return;
        }
        String nameLower = GuildLogic.normalizeName(name);
        FirebaseRefs.guildIndex().child(nameLower)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onError(R.string.guild_error_taken);
                            return;
                        }
                        createAfterCheck(uid, myName, myLevel, name.trim(),
                                GuildLogic.normalizeTag(tag), nameLower, callback);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(R.string.social_error_network);
                    }
                });
    }

    private void createAfterCheck(String uid, String myName, int myLevel, String name,
                                  String tag, String nameLower, ActionCallback callback) {
        long now = System.currentTimeMillis();
        String gid = GuildLogic.guildId(nameLower, now);
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                String current = player.child("guild").child("id").getValue(String.class);
                if (current != null && !current.isEmpty()) {
                    return Transaction.abort();
                }
                MutableData coin = player.child("currencies").child("coin");
                long balance = coin.getValue() instanceof Number
                        ? ((Number) coin.getValue()).longValue() : 0;
                if (balance < GuildLogic.CREATE_COST_COIN) {
                    return Transaction.abort();
                }
                coin.setValue(balance - GuildLogic.CREATE_COST_COIN);
                MutableData guild = player.child("guild");
                guild.child("id").setValue(gid);
                guild.child("role").setValue(GuildLogic.ROLE_LEADER);
                guild.child("coin").setValue(0);
                guild.child("joinedAt").setValue(now);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(error == null
                            ? R.string.guild_error_create
                            : R.string.social_error_network);
                    return;
                }
                Map<String, Object> info = new HashMap<>();
                info.put("id", gid);
                info.put("name", name);
                info.put("nameLower", nameLower);
                info.put("tag", tag);
                info.put("level", 1);
                info.put("exp", 0);
                info.put("leaderUid", uid);
                info.put("memberCount", 1);
                info.put("maxMembers", GuildLogic.MAX_MEMBERS);
                info.put("minLevel", Math.max(1, myLevel));
                info.put("open", true);
                info.put("notice", "Bersama kita tumbuh.");
                info.put("noticeEn", "Together we grow.");
                info.put("bank", 0);
                info.put("createdAt", now);
                info.put("updatedAt", now);

                Map<String, Object> member = new HashMap<>();
                member.put("role", GuildLogic.ROLE_LEADER);
                member.put("name", safeName(myName));
                member.put("joinedAt", now);
                member.put("weeklyPoints", 0);
                member.put("totalPoints", 0);

                Map<String, Object> summary = new HashMap<>();
                summary.put("gid", gid);
                summary.put("name", name);
                summary.put("tag", tag);
                summary.put("memberCount", 1);
                summary.put("minLevel", Math.max(1, myLevel));
                summary.put("open", true);
                summary.put("updatedAt", now);

                Map<String, Object> updates = new HashMap<>();
                updates.put("guilds/" + gid + "/info", info);
                updates.put("guilds/" + gid + "/members/" + uid, member);
                updates.put("guildIndex/" + nameLower, gid);
                updates.put("guildDirectory/" + gid, summary);
                FirebaseRefs.root().updateChildren(updates, (updateError, ref) -> {
                    if (updateError == null) {
                        callback.onSuccess();
                    } else {
                        rollbackCreate(uid, gid, callback);
                    }
                });
            }
        });
    }

    private void rollbackCreate(String uid, String gid, ActionCallback callback) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                String current = player.child("guild").child("id").getValue(String.class);
                if (!gid.equals(current)) {
                    return Transaction.abort();
                }
                MutableData coin = player.child("currencies").child("coin");
                long balance = coin.getValue() instanceof Number
                        ? ((Number) coin.getValue()).longValue() : 0;
                coin.setValue(balance + GuildLogic.CREATE_COST_COIN);
                player.child("guild").setValue(null);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                callback.onError(R.string.guild_error_create);
            }
        });
    }

    public void join(String uid, String myName, int myLevel, GuildSummary summary,
                     ActionCallback callback) {
        if (!summary.open) {
            callback.onError(R.string.guild_error_closed);
            return;
        }
        if (summary.memberCount >= GuildLogic.MAX_MEMBERS) {
            callback.onError(R.string.guild_error_full);
            return;
        }
        if (myLevel < summary.minLevel) {
            callback.onError(R.string.guild_error_level);
            return;
        }
        long now = System.currentTimeMillis();
        int newCount = summary.memberCount + 1;

        Map<String, Object> member = new HashMap<>();
        member.put("role", GuildLogic.ROLE_MEMBER);
        member.put("name", safeName(myName));
        member.put("joinedAt", now);
        member.put("weeklyPoints", 0);
        member.put("totalPoints", 0);

        Map<String, Object> updates = new HashMap<>();
        updates.put("guilds/" + summary.gid + "/members/" + uid, member);
        updates.put("guilds/" + summary.gid + "/info/memberCount", newCount);
        updates.put("guilds/" + summary.gid + "/info/updatedAt", now);
        updates.put("guildDirectory/" + summary.gid + "/memberCount", newCount);
        updates.put("guildDirectory/" + summary.gid + "/updatedAt", now);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error != null) {
                callback.onError(R.string.social_error_action);
                return;
            }
            FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData player) {
                    if (player.getValue() == null) {
                        return Transaction.abort();
                    }
                    String current = player.child("guild").child("id").getValue(String.class);
                    if (current != null && !current.isEmpty()) {
                        return Transaction.abort();
                    }
                    MutableData guild = player.child("guild");
                    guild.child("id").setValue(summary.gid);
                    guild.child("role").setValue(GuildLogic.ROLE_MEMBER);
                    guild.child("joinedAt").setValue(now);
                    return Transaction.success(player);
                }

                @Override
                public void onComplete(DatabaseError playerError, boolean committed,
                                       DataSnapshot snapshot) {
                    if (committed) {
                        callback.onSuccess();
                    } else {
                        rollbackJoin(uid, summary, newCount, callback);
                    }
                }
            });
        });
    }

    private void rollbackJoin(String uid, GuildSummary summary, int count, ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("guilds/" + summary.gid + "/members/" + uid, null);
        updates.put("guilds/" + summary.gid + "/info/memberCount", count - 1);
        updates.put("guildDirectory/" + summary.gid + "/memberCount", count - 1);
        FirebaseRefs.root().updateChildren(updates, (error, ref) ->
                callback.onError(R.string.social_error_action));
    }

    public void leave(String uid, GuildInfo info, List<GuildMember> members,
                      ActionCallback callback) {
        if (info == null || members == null) {
            callback.onError(R.string.guild_error_leave);
            return;
        }
        GuildMember me = null;
        Map<String, String> roles = new HashMap<>();
        for (GuildMember member : members) {
            roles.put(member.uid, member.role);
            if (member.uid.equals(uid)) {
                me = member;
            }
        }
        if (me == null) {
            callback.onError(R.string.guild_error_leave);
            return;
        }
        long now = System.currentTimeMillis();
        int newCount = Math.max(0, members.size() - 1);
        if (me.isLeader()) {
            String next = GuildLogic.nextLeader(roles, uid);
            if (next == null) {
                disband(uid, info, callback);
            } else {
                transferAndLeave(uid, info, next, newCount, now, callback);
            }
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("guilds/" + info.gid + "/members/" + uid, null);
        updates.put("guilds/" + info.gid + "/info/memberCount", newCount);
        updates.put("guilds/" + info.gid + "/info/updatedAt", now);
        updates.put("guildDirectory/" + info.gid + "/memberCount", newCount);
        updates.put("guildDirectory/" + info.gid + "/updatedAt", now);
        updates.put("players/" + uid + "/guild", null);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    private void transferAndLeave(String uid, GuildInfo info, String next, int newCount,
                                  long now, ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("guilds/" + info.gid + "/members/" + next + "/role",
                GuildLogic.ROLE_LEADER);
        updates.put("guilds/" + info.gid + "/members/" + uid, null);
        updates.put("guilds/" + info.gid + "/info/leaderUid", next);
        updates.put("guilds/" + info.gid + "/info/memberCount", newCount);
        updates.put("guilds/" + info.gid + "/info/updatedAt", now);
        updates.put("guildDirectory/" + info.gid + "/memberCount", newCount);
        updates.put("guildDirectory/" + info.gid + "/updatedAt", now);
        updates.put("players/" + uid + "/guild", null);
        FirebaseRefs.root().updateChildren(updates, (error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.social_error_action);
            }
        });
    }

    private void disband(String uid, GuildInfo info, ActionCallback callback) {
        Map<String, Object> indexUpdate = new HashMap<>();
        indexUpdate.put("guildIndex/" + info.nameLower, null);
        indexUpdate.put("guildDirectory/" + info.gid, null);
        indexUpdate.put("players/" + uid + "/guild", null);
        FirebaseRefs.root().updateChildren(indexUpdate, (error, ref) -> {
            if (error != null) {
                callback.onError(R.string.social_error_action);
                return;
            }
            FirebaseRefs.guild(info.gid).removeValue((removeError, removeRef) -> {
                if (removeError == null) {
                    callback.onSuccess();
                } else {
                    callback.onError(R.string.social_error_action);
                }
            });
        });
    }

    public void sendChat(String uid, String myName, String gid, String text,
                         ActionCallback callback) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            callback.onError(R.string.guild_chat_empty);
            return;
        }
        if (trimmed.length() > GuildLogic.MAX_MESSAGE) {
            trimmed = trimmed.substring(0, GuildLogic.MAX_MESSAGE);
        }
        Map<String, Object> message = new HashMap<>();
        message.put("uid", uid);
        message.put("name", safeName(myName));
        message.put("text", trimmed);
        message.put("at", System.currentTimeMillis());
        FirebaseRefs.guild(gid).child("chat").push().setValue(message,
                (error, ref) -> {
                    if (error == null) {
                        callback.onSuccess();
                    } else {
                        callback.onError(R.string.social_error_action);
                    }
                });
    }

    public void donate(String uid, String gid, String myName, ActionCallback callback) {
        long now = System.currentTimeMillis();
        String day = SocialDay.dayKey(now);
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                String current = player.child("guild").child("id").getValue(String.class);
                if (current == null || current.isEmpty()) {
                    return Transaction.abort();
                }
                MutableData donations = player.child("guild").child("donations");
                int used = day.equals(donations.child("day").getValue(String.class))
                        ? intOf(donations.child("count"), 0) : 0;
                if (used >= GuildLogic.DONATIONS_PER_DAY) {
                    return Transaction.abort();
                }
                MutableData coin = player.child("currencies").child("coin");
                long balance = coin.getValue() instanceof Number
                        ? ((Number) coin.getValue()).longValue() : 0;
                if (balance < GuildLogic.DONATION_COST_COIN) {
                    return Transaction.abort();
                }
                coin.setValue(balance - GuildLogic.DONATION_COST_COIN);
                donations.child("day").setValue(day);
                donations.child("count").setValue(used + 1);
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (!committed) {
                    callback.onError(R.string.guild_donate_failed);
                    return;
                }
                int points = GuildLogic.weeklyPoints(GuildLogic.DONATION_COST_COIN);
                FirebaseRefs.guild(gid).child("info").child("bank").runTransaction(
                        new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData bank) {
                                long value = bank.getValue() instanceof Number
                                        ? ((Number) bank.getValue()).longValue() : 0;
                                bank.setValue(value + GuildLogic.DONATION_COST_COIN);
                                return Transaction.success(bank);
                            }

                            @Override
                            public void onComplete(DatabaseError bankError, boolean bankCommitted,
                                                   DataSnapshot bankSnapshot) {
                                if (!bankCommitted) {
                                    refundDonation(uid, day, callback);
                                    return;
                                }
                                addMemberPoints(uid, gid, points);
                                callback.onSuccess();
                            }
                        });
            }
        });
    }

    private void refundDonation(String uid, String day, ActionCallback callback) {
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData coin = player.child("currencies").child("coin");
                long balance = coin.getValue() instanceof Number
                        ? ((Number) coin.getValue()).longValue() : 0;
                coin.setValue(balance + GuildLogic.DONATION_COST_COIN);
                MutableData donations = player.child("guild").child("donations");
                if (day.equals(donations.child("day").getValue(String.class))) {
                    int count = intOf(donations.child("count"), 0);
                    donations.child("count").setValue(Math.max(0, count - 1));
                }
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                callback.onError(R.string.guild_donate_failed);
            }
        });
    }

    private void addMemberPoints(String uid, String gid, int points) {
        if (points <= 0) {
            return;
        }
        FirebaseRefs.guild(gid).child("members").child(uid)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData member) {
                        if (member.getValue() == null) {
                            return Transaction.abort();
                        }
                        member.child("weeklyPoints").setValue(
                                intOf(member.child("weeklyPoints"), 0) + points);
                        member.child("totalPoints").setValue(
                                intOf(member.child("totalPoints"), 0) + points);
                        return Transaction.success(member);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed,
                                           DataSnapshot snapshot) {
                    }
                });
    }

    public void ensureBoss(String gid, int memberCount, ActionCallback callback) {
        long now = System.currentTimeMillis();
        String weekKey = SocialDay.weekKey(now);
        FirebaseRefs.guild(gid).child("boss").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData boss) {
                if (boss.getValue() != null
                        && weekKey.equals(boss.child("weekKey").getValue(String.class))) {
                    return Transaction.abort();
                }
                GuildBoss fresh = GuildBoss.fresh(memberCount, now);
                boss.setValue(fresh.toMap());
                return Transaction.success(boss);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (error != null) {
                    callback.onError(R.string.social_error_network);
                } else {
                    callback.onSuccess();
                }
            }
        });
    }

    public void attackBoss(String uid, String gid, int damage, String hash,
                           AttackCallback callback) {
        long now = System.currentTimeMillis();
        String day = SocialDay.dayKey(now);
        final int[] applied = new int[1];
        final int[] left = new int[1];
        FirebaseRefs.guild(gid).child("boss").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData boss) {
                if (boss.getValue() == null) {
                    return Transaction.abort();
                }
                String weekKey = boss.child("weekKey").getValue(String.class);
                if (!SocialDay.weekKey(now).equals(weekKey)) {
                    return Transaction.abort();
                }
                if (Boolean.TRUE.equals(boss.child("attacks").child(uid).child(day)
                        .getValue(Boolean.class))) {
                    return Transaction.abort();
                }
                int hpMax = intOf(boss.child("hpMax"), 1);
                int hpLeft = intOf(boss.child("hpLeft"), hpMax);
                if (hpLeft <= 0) {
                    return Transaction.abort();
                }
                int capped = GuildBossLogic.capDamage(hpMax, damage);
                if (capped <= 0) {
                    return Transaction.abort();
                }
                int dealt = Math.min(capped, hpLeft);
                boss.child("hpLeft").setValue(hpLeft - dealt);
                boss.child("damage").child(uid).setValue(
                        intOf(boss.child("damage").child(uid), 0) + dealt);
                boss.child("attacks").child(uid).child(day).setValue(true);
                boss.child("lastBattle").child(uid).setValue(hash == null ? "" : hash);
                applied[0] = dealt;
                left[0] = hpLeft - dealt;
                return Transaction.success(boss);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (committed) {
                    callback.onSuccess(applied[0], left[0]);
                } else {
                    callback.onError(R.string.guild_boss_already_attacked);
                }
            }
        });
    }

    public void claimBossReward(String uid, String gid, String weekKey, int damage,
                                RewardCallback callback) {
        if (damage <= 0 || weekKey == null) {
            callback.onSuccess(0, 0);
            return;
        }
        int points = GuildBossLogic.weeklyPoints(damage);
        int coin = GuildBossLogic.guildCoin(damage);
        final boolean[] pointsApplied = new boolean[1];
        FirebaseRefs.guild(gid).child("members").child(uid)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData member) {
                        if (member.getValue() == null) {
                            return Transaction.abort();
                        }
                        if (member.child("bossClaims").child(weekKey).getValue() != null) {
                            return Transaction.abort();
                        }
                        member.child("weeklyPoints").setValue(
                                intOf(member.child("weeklyPoints"), 0) + points);
                        member.child("totalPoints").setValue(
                                intOf(member.child("totalPoints"), 0) + points);
                        member.child("bossClaims").child(weekKey).setValue(true);
                        pointsApplied[0] = true;
                        return Transaction.success(member);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed,
                                           DataSnapshot snapshot) {
                        claimBossCoin(uid, weekKey, coin,
                                pointsApplied[0] ? points : 0, callback);
                    }
                });
    }

    private void claimBossCoin(String uid, String weekKey, int coin, int points,
                               RewardCallback callback) {
        final boolean[] coinApplied = new boolean[1];
        FirebaseRefs.player(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData player) {
                if (player.getValue() == null) {
                    return Transaction.abort();
                }
                MutableData claims = player.child("guild").child("bossClaims");
                if (claims.child(weekKey).getValue() != null) {
                    return Transaction.abort();
                }
                MutableData guildCoin = player.child("currencies").child("guildCoin");
                long balance = guildCoin.getValue() instanceof Number
                        ? ((Number) guildCoin.getValue()).longValue() : 0;
                guildCoin.setValue(balance + coin);
                claims.child(weekKey).setValue(System.currentTimeMillis());
                coinApplied[0] = true;
                return Transaction.success(player);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                callback.onSuccess(coinApplied[0] ? coin : 0, points);
            }
        });
    }

    private static String safeName(String name) {
        String value = name == null || name.trim().isEmpty() ? "Pemanggil" : name.trim();
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    private static int intOf(MutableData data, int fallback) {
        Object value = data.getValue();
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    public static final class Handle {
        private final Query query;
        private final ValueEventListener listener;

        Handle(Query query, ValueEventListener listener) {
            this.query = query;
            this.listener = listener;
        }

        public void detach() {
            if (query != null && listener != null) {
                query.removeEventListener(listener);
            }
        }
    }
}
