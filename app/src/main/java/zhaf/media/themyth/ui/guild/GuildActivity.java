package zhaf.media.themyth.ui.guild;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.model.PlayerProfile;
import zhaf.media.themyth.data.repository.GuildRepository;
import zhaf.media.themyth.data.repository.SocialRepository;
import zhaf.media.themyth.data.social.GuildBoss;
import zhaf.media.themyth.data.social.GuildChatMessage;
import zhaf.media.themyth.data.social.GuildInfo;
import zhaf.media.themyth.data.social.GuildLogic;
import zhaf.media.themyth.data.social.GuildMember;
import zhaf.media.themyth.data.social.GuildSummary;
import zhaf.media.themyth.data.social.SocialDay;
import zhaf.media.themyth.databinding.ActivityGuildBinding;
import zhaf.media.themyth.databinding.ItemGuildChatBinding;
import zhaf.media.themyth.databinding.ItemGuildMemberBinding;
import zhaf.media.themyth.databinding.ItemGuildRowBinding;
import zhaf.media.themyth.ui.battle.BattleActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class GuildActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 12_000L;

    private ActivityGuildBinding binding;
    private GuildRepository repository;
    private MemberAdapter memberAdapter;
    private ChatAdapter chatAdapter;

    private GuildRepository.Handle myGuildHandle;
    private final List<GuildRepository.Handle> guildHandles = new ArrayList<>();
    private DatabaseReference donationsRef;
    private ValueEventListener donationsListener;

    private String uid;
    private String gid;
    private String myName = "Pemanggil";
    private int myLevel = 1;

    private GuildInfo info;
    private GuildBoss boss;
    private final List<GuildMember> members = new ArrayList<>();
    private final List<GuildChatMessage> chats = new ArrayList<>();
    private final List<GuildSummary> directory = new ArrayList<>();
    private final Set<String> bossRewardWeeks = new HashSet<>();

    private boolean myGuildLoaded;
    private boolean directoryLoaded;
    private boolean directoryLoading;
    private boolean bossEnsured;
    private boolean actionBusy;
    private boolean profileRequested;
    private int donationsLeft = GuildLogic.DONATIONS_PER_DAY;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (gid != null && info != null) {
                return;
            }
            binding.progressGuild.setVisibility(View.GONE);
            binding.guildContent.setVisibility(View.GONE);
            binding.guildNoContent.setVisibility(View.GONE);
            binding.guildErrorBox.setVisibility(View.VISIBLE);
        }
    };
    private final Runnable directoryTimeout = new Runnable() {
        @Override
        public void run() {
            if (directoryLoaded || !directoryLoading || isFinishing() || isDestroyed()) {
                return;
            }
            directoryLoading = false;
            binding.tvGuildDirectoryEmpty.setText(R.string.guild_discover_retry);
            binding.tvGuildDirectoryEmpty.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuildBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        repository = new GuildRepository();

        memberAdapter = new MemberAdapter();
        chatAdapter = new ChatAdapter();
        binding.guildMembersList.setLayoutManager(new LinearLayoutManager(this));
        binding.guildMembersList.setAdapter(memberAdapter);
        binding.guildChatList.setLayoutManager(new LinearLayoutManager(this));
        binding.guildChatList.setAdapter(chatAdapter);

        binding.btnGuildBack.setOnClickListener(v -> finish());
        binding.btnGuildCreate.setOnClickListener(v -> createGuild());
        binding.btnGuildLeave.setOnClickListener(v -> confirmLeave());
        binding.btnGuildDonate.setOnClickListener(v -> donate());
        binding.btnGuildAttack.setOnClickListener(v -> attackBoss());
        binding.btnGuildSend.setOnClickListener(v -> sendChat());
        binding.btnGuildRetry.setOnClickListener(v -> retry());
        binding.tvGuildDirectoryEmpty.setOnClickListener(v -> loadDirectory());
        binding.etGuildChat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChat();
                return true;
            }
            return false;
        });

        binding.btnGuildCreate.setText(getString(R.string.guild_create,
                Texts.number(GuildLogic.CREATE_COST_COIN)));
        binding.btnGuildDonate.setText(getString(R.string.guild_donate,
                Texts.number(GuildLogic.DONATION_COST_COIN)));
        renderDonate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachMyGuild();
        loadProfile();
    }

    @Override
    protected void onStop() {
        detachMyGuild();
        detachGuildHandles();
        super.onStop();
    }

    private void attachMyGuild() {
        if (myGuildHandle != null || uid == null) {
            return;
        }
        if (!myGuildLoaded) {
            binding.progressGuild.setVisibility(View.VISIBLE);
            armLoadTimeout();
        }
        myGuildHandle = GuildRepository.observeMyGuild(uid,
                new GuildRepository.MyGuildCallback() {
                    @Override
                    public void onData(String guildId, String role) {
                        disarmLoadTimeout();
                        myGuildLoaded = true;
                        if (guildId == null || guildId.isEmpty()) {
                            if (gid != null) {
                                detachGuildHandles();
                                gid = null;
                                resetGuildData();
                            }
                            showNoGuild();
                            publishDirectory();
                            return;
                        }
                        if (guildId.equals(gid)) {
                            if (guildHandles.isEmpty()) {
                                attachGuild();
                            }
                            return;
                        }
                        detachGuildHandles();
                        gid = guildId;
                        resetGuildData();
                        attachGuild();
                        publishDirectory();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        disarmLoadTimeout();
                        binding.progressGuild.setVisibility(View.GONE);
                        binding.guildContent.setVisibility(View.GONE);
                        binding.guildNoContent.setVisibility(View.GONE);
                        binding.guildErrorBox.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void detachMyGuild() {
        disarmLoadTimeout();
        if (myGuildHandle != null) {
            myGuildHandle.detach();
            myGuildHandle = null;
        }
    }

    private void detachGuildHandles() {
        disarmLoadTimeout();
        for (GuildRepository.Handle handle : guildHandles) {
            handle.detach();
        }
        guildHandles.clear();
        if (donationsRef != null && donationsListener != null) {
            donationsRef.removeEventListener(donationsListener);
        }
        donationsRef = null;
        donationsListener = null;
        bossEnsured = false;
    }

    private void resetGuildData() {
        info = null;
        boss = null;
        members.clear();
        chats.clear();
        memberAdapter.submit(new ArrayList<>());
        chatAdapter.submit(new ArrayList<>());
        binding.tvGuildMembersEmpty.setVisibility(View.GONE);
        binding.tvGuildChatEmpty.setVisibility(View.VISIBLE);
        donationsLeft = GuildLogic.DONATIONS_PER_DAY;
    }

    private void attachGuild() {
        if (gid == null) {
            return;
        }
        binding.progressGuild.setVisibility(View.VISIBLE);
        binding.guildNoContent.setVisibility(View.GONE);
        binding.guildErrorBox.setVisibility(View.GONE);
        binding.guildContent.setVisibility(View.VISIBLE);
        armLoadTimeout();

        guildHandles.add(GuildRepository.observeInfo(gid,
                new GuildRepository.InfoCallback() {
                    @Override
                    public void onData(GuildInfo data) {
                        disarmLoadTimeout();
                        info = data;
                        binding.progressGuild.setVisibility(View.GONE);
                        renderInfo();
                        if (!bossEnsured) {
                            bossEnsured = true;
                            repository.ensureBoss(gid, data.memberCount,
                                    new GuildRepository.ActionCallback() {
                                        @Override
                                        public void onSuccess() {
                                        }

                                        @Override
                                        public void onError(@StringRes int messageRes) {
                                            showMessage(messageRes);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        binding.progressGuild.setVisibility(View.GONE);
                        binding.guildContent.setVisibility(View.GONE);
                        binding.guildNoContent.setVisibility(View.GONE);
                        binding.guildErrorBox.setVisibility(View.VISIBLE);
                    }
                }));
        guildHandles.add(GuildRepository.observeMembers(gid,
                new GuildRepository.MembersCallback() {
                    @Override
                    public void onData(List<GuildMember> data) {
                        members.clear();
                        members.addAll(data);
                        memberAdapter.submit(members);
                        binding.tvGuildMembersEmpty.setVisibility(
                                members.isEmpty() ? View.VISIBLE : View.GONE);
                        renderInfo();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        showMessage(R.string.guild_error_load);
                    }
                }));
        guildHandles.add(GuildRepository.observeChat(gid,
                new GuildRepository.ChatCallback() {
                    @Override
                    public void onData(List<GuildChatMessage> data) {
                        chats.clear();
                        chats.addAll(data);
                        chatAdapter.submit(chats);
                        binding.tvGuildChatEmpty.setVisibility(
                                chats.isEmpty() ? View.VISIBLE : View.GONE);
                        if (!chats.isEmpty()) {
                            binding.guildChatList.post(() -> binding.guildChatList
                                    .scrollToPosition(chats.size() - 1));
                        }
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        showMessage(R.string.guild_error_load);
                    }
                }));
        guildHandles.add(GuildRepository.observeBoss(gid,
                new GuildRepository.BossCallback() {
                    @Override
                    public void onData(GuildBoss data) {
                        boss = data;
                        renderBoss();
                        maybeClaimBossReward();
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        showMessage(R.string.guild_error_load);
                    }
                }));

        donationsRef = FirebaseRefs.player(uid).child("guild").child("donations");
        donationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                donationsLeft = GuildLogic.donationsLeft(
                        snapshot.child("day").getValue(String.class),
                        intOf(snapshot.child("count").getValue(), 0),
                        SocialDay.dayKey(System.currentTimeMillis()));
                renderDonate();
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };
        donationsRef.addValueEventListener(donationsListener);
    }

    private void showNoGuild() {
        disarmLoadTimeout();
        binding.progressGuild.setVisibility(View.GONE);
        binding.guildErrorBox.setVisibility(View.GONE);
        binding.guildContent.setVisibility(View.GONE);
        binding.guildNoContent.setVisibility(View.VISIBLE);
        if (!directoryLoaded && !directoryLoading) {
            loadDirectory();
        }
    }

    private void loadDirectory() {
        if (directoryLoading) {
            return;
        }
        directoryLoading = true;
        binding.tvGuildDirectoryEmpty.setVisibility(View.GONE);
        binding.guildDirectoryList.removeAllViews();
        timeoutHandler.removeCallbacks(directoryTimeout);
        timeoutHandler.postDelayed(directoryTimeout, LOAD_TIMEOUT_MS);
        repository.loadDirectory(new GuildRepository.DirectoryCallback() {
            @Override
            public void onData(List<GuildSummary> guilds) {
                timeoutHandler.removeCallbacks(directoryTimeout);
                directoryLoading = false;
                directoryLoaded = true;
                renderDirectory(guilds);
            }

            @Override
            public void onError(DatabaseError error) {
                timeoutHandler.removeCallbacks(directoryTimeout);
                directoryLoading = false;
                binding.tvGuildDirectoryEmpty.setText(R.string.guild_discover_retry);
                binding.tvGuildDirectoryEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderDirectory(List<GuildSummary> guilds) {
        directory.clear();
        if (guilds != null) {
            directory.addAll(guilds);
        }
        binding.guildDirectoryList.removeAllViews();
        if (guilds == null || guilds.isEmpty()) {
            binding.tvGuildDirectoryEmpty.setText(R.string.guild_discover_empty);
            binding.tvGuildDirectoryEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvGuildDirectoryEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (GuildSummary summary : guilds) {
            ItemGuildRowBinding row = ItemGuildRowBinding.inflate(
                    inflater, binding.guildDirectoryList, false);
            row.tvGuildRowName.setText(getString(R.string.guild_summary_row,
                    summary.name, summary.tag, summary.memberCount));
            row.tvGuildRowMeta.setText(getString(R.string.guild_summary_level,
                    summary.minLevel));
            int check = GuildLogic.joinCheck(myLevel, summary.minLevel,
                    summary.memberCount, summary.open, gid, summary.gid);
            row.btnGuildRowJoin.setEnabled(check == GuildLogic.JOIN_OK && !actionBusy);
            row.btnGuildRowJoin.setAlpha(row.btnGuildRowJoin.isEnabled() ? 1f : 0.55f);
            row.btnGuildRowJoin.setOnClickListener(v -> join(summary));
            binding.guildDirectoryList.addView(row.getRoot());
        }
    }

    private void createGuild() {
        if (actionBusy) {
            return;
        }
        String name = textOf(binding.etGuildName);
        String tag = textOf(binding.etGuildTag);
        setActionBusy(true);
        binding.btnGuildCreate.setEnabled(false);
        repository.create(uid, myName, myLevel, name, tag,
                new GuildRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setActionBusy(false);
                        binding.btnGuildCreate.setEnabled(true);
                        binding.etGuildName.setText("");
                        binding.etGuildTag.setText("");
                        showMessage(R.string.guild_created);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setActionBusy(false);
                        binding.btnGuildCreate.setEnabled(true);
                        showMessage(messageRes);
                    }
                });
    }

    private void join(GuildSummary summary) {
        if (actionBusy) {
            return;
        }
        int check = GuildLogic.joinCheck(myLevel, summary.minLevel,
                summary.memberCount, summary.open, gid, summary.gid);
        if (check == GuildLogic.JOIN_CLOSED) {
            showMessage(R.string.guild_error_closed);
            return;
        }
        if (check == GuildLogic.JOIN_FULL) {
            showMessage(R.string.guild_error_full);
            return;
        }
        if (check == GuildLogic.JOIN_LEVEL) {
            showMessage(R.string.guild_error_level);
            return;
        }
        if (check == GuildLogic.JOIN_SAME) {
            return;
        }
        setActionBusy(true);
        repository.join(uid, myName, myLevel, summary,
                new GuildRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setActionBusy(false);
                        directoryLoaded = false;
                        showMessage(R.string.guild_joined);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setActionBusy(false);
                        showMessage(messageRes);
                    }
                });
    }

    private void confirmLeave() {
        if (info == null || members.isEmpty() || actionBusy) {
            showMessage(R.string.guild_error_leave);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.guild_leave_confirm_title)
                .setMessage(R.string.guild_leave_confirm_message)
                .setNegativeButton(R.string.guild_cancel, null)
                .setPositiveButton(R.string.guild_leave_yes, (dialog, which) -> leave())
                .show();
    }

    private void leave() {
        if (info == null || members.isEmpty() || actionBusy) {
            return;
        }
        setActionBusy(true);
        repository.leave(uid, info, new ArrayList<>(members),
                new GuildRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setActionBusy(false);
                        directoryLoaded = false;
                        info = null;
                        boss = null;
                        showMessage(R.string.guild_left);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setActionBusy(false);
                        showMessage(messageRes);
                    }
                });
    }

    private void donate() {
        if (actionBusy || gid == null) {
            return;
        }
        if (donationsLeft <= 0) {
            showMessage(R.string.guild_donate_failed);
            return;
        }
        setActionBusy(true);
        repository.donate(uid, gid, myName, new GuildRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setActionBusy(false);
                showMessage(getString(R.string.guild_donate_done,
                        GuildLogic.weeklyPoints(GuildLogic.DONATION_COST_COIN)));
            }

            @Override
            public void onError(@StringRes int messageRes) {
                setActionBusy(false);
                showMessage(messageRes);
            }
        });
    }

    private void attackBoss() {
        if (gid == null || boss == null || actionBusy) {
            return;
        }
        if (boss.defeated()) {
            showMessage(R.string.guild_boss_defeated);
            return;
        }
        if (boss.attackedToday(uid, System.currentTimeMillis())) {
            showMessage(R.string.guild_boss_already_attacked);
            return;
        }
        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra(BattleActivity.EXTRA_GUILD_ID, gid);
        startActivity(intent);
    }

    private void sendChat() {
        if (actionBusy || gid == null) {
            return;
        }
        String text = textOf(binding.etGuildChat);
        if (text.isEmpty()) {
            showMessage(R.string.guild_chat_empty_message);
            return;
        }
        setActionBusy(true);
        repository.sendChat(uid, myName, gid, text,
                new GuildRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setActionBusy(false);
                        binding.etGuildChat.setText("");
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        setActionBusy(false);
                        showMessage(messageRes);
                    }
                });
    }

    private void renderInfo() {
        if (info == null) {
            return;
        }
        binding.tvGuildName.setText(info.name);
        binding.tvGuildTag.setText(info.tag);
        binding.tvGuildLevel.setText(getString(R.string.guild_level, info.level));
        binding.tvGuildMembers.setText(getString(R.string.guild_members,
                info.memberCount, GuildLogic.MAX_MEMBERS));
        binding.tvGuildBank.setText(getString(R.string.guild_bank,
                Texts.number(info.bank)));

        int myPoints = 0;
        for (GuildMember member : members) {
            if (member.uid.equals(uid)) {
                myPoints = member.weeklyPoints;
                break;
            }
        }
        binding.tvGuildMyPoints.setText(getString(R.string.guild_my_points, myPoints));

        if (TextUtils.isEmpty(info.displayNotice())) {
            binding.tvGuildNoticeTitle.setVisibility(View.GONE);
            binding.tvGuildNotice.setVisibility(View.GONE);
        } else {
            binding.tvGuildNoticeTitle.setVisibility(View.VISIBLE);
            binding.tvGuildNotice.setVisibility(View.VISIBLE);
            binding.tvGuildNotice.setText(info.displayNotice());
        }
    }

    private void renderBoss() {
        if (boss == null) {
            binding.tvGuildBossName.setText(R.string.guild_loading);
            binding.pbGuildBoss.setProgress(0);
            binding.tvGuildBossHp.setText("");
            binding.tvGuildBossDamage.setText("");
            binding.tvGuildBossEnds.setText("");
            binding.btnGuildAttack.setEnabled(false);
            binding.btnGuildAttack.setText(R.string.guild_boss_next);
            return;
        }
        binding.tvGuildBossName.setText(boss.displayName());
        binding.pbGuildBoss.setProgress(boss.hpPercent());
        binding.tvGuildBossHp.setText(getString(R.string.guild_boss_hp,
                Texts.number(boss.hpLeft), Texts.number(boss.hpMax)));
        binding.tvGuildBossDamage.setText(getString(R.string.guild_boss_my_damage,
                Texts.number(boss.damageOf(uid))));

        boolean attacked = boss.attackedToday(uid, System.currentTimeMillis());
        if (boss.defeated()) {
            binding.tvGuildBossEnds.setText(R.string.guild_boss_defeated);
            binding.btnGuildAttack.setText(R.string.guild_boss_next);
            binding.btnGuildAttack.setEnabled(false);
        } else {
            binding.tvGuildBossEnds.setText(getString(R.string.guild_boss_ends,
                    SocialDay.daysLeft(boss.endsAt, System.currentTimeMillis())));
            binding.btnGuildAttack.setText(attacked
                    ? R.string.guild_boss_attacked : R.string.guild_boss_attack);
            binding.btnGuildAttack.setEnabled(!attacked && !actionBusy);
        }
        binding.btnGuildAttack.setAlpha(
                binding.btnGuildAttack.isEnabled() ? 1f : 0.55f);
    }

    private void renderDonate() {
        binding.tvGuildDonateLeft.setText(getString(R.string.guild_donate_left,
                donationsLeft));
        binding.btnGuildDonate.setEnabled(donationsLeft > 0 && !actionBusy);
        binding.btnGuildDonate.setAlpha(binding.btnGuildDonate.isEnabled() ? 1f : 0.55f);
    }

    private void maybeClaimBossReward() {
        if (boss == null || gid == null) {
            return;
        }
        if (!boss.attackedToday(uid, System.currentTimeMillis())
                || boss.damageOf(uid) <= 0) {
            return;
        }
        if (!bossRewardWeeks.add(boss.weekKey)) {
            return;
        }
        repository.claimBossReward(uid, gid, boss.weekKey, boss.damageOf(uid),
                new GuildRepository.RewardCallback() {
                    @Override
                    public void onSuccess(int coin, int points) {
                        if (coin > 0 || points > 0) {
                            showMessage(getString(R.string.guild_boss_reward,
                                    Texts.number(coin), points));
                        }
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                    }
                });
    }

    private void setActionBusy(boolean busy) {
        actionBusy = busy;
        binding.btnGuildCreate.setEnabled(!busy);
        binding.btnGuildCreate.setAlpha(busy ? 0.6f : 1f);
        binding.btnGuildSend.setEnabled(!busy);
        binding.btnGuildSend.setAlpha(busy ? 0.6f : 1f);
        renderDonate();
        renderBoss();
        if (!directory.isEmpty() && binding.guildNoContent.getVisibility() == View.VISIBLE) {
            renderDirectory(directory);
        }
    }

    private void loadProfile() {
        if (profileRequested || uid == null) {
            return;
        }
        profileRequested = true;
        FirebaseRefs.profile(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    PlayerProfile profile = PlayerProfile.from(snapshot, uid);
                    myName = profile.username;
                    myLevel = profile.level;
                }
                publishDirectory();
                if (directoryLoaded) {
                    loadDirectory();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                publishDirectory();
            }
        });
    }

    private void publishDirectory() {
        if (uid == null) {
            return;
        }
        SocialRepository.publishDirectory(uid, myName, myLevel, gid);
    }

    private void armLoadTimeout() {
        timeoutHandler.removeCallbacks(loadTimeout);
        timeoutHandler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);
    }

    private void disarmLoadTimeout() {
        timeoutHandler.removeCallbacks(loadTimeout);
    }

    @Override
    protected void onDestroy() {
        disarmLoadTimeout();
        timeoutHandler.removeCallbacks(directoryTimeout);
        super.onDestroy();
    }

    private void retry() {
        detachMyGuild();
        detachGuildHandles();
        timeoutHandler.removeCallbacks(directoryTimeout);
        myGuildLoaded = false;
        directoryLoaded = false;
        gid = null;
        info = null;
        boss = null;
        resetGuildData();
        directory.clear();
        binding.guildDirectoryList.removeAllViews();
        binding.guildErrorBox.setVisibility(View.GONE);
        binding.progressGuild.setVisibility(View.VISIBLE);
        attachMyGuild();
    }

    @StringRes
    private static int roleLabel(String role) {
        if (GuildLogic.ROLE_LEADER.equals(role)) {
            return R.string.guild_role_leader;
        }
        if (GuildLogic.ROLE_OFFICER.equals(role)) {
            return R.string.guild_role_officer;
        }
        return R.string.guild_role_member;
    }

    private static String textOf(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private void showMessage(@StringRes int messageRes) {
        Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_SHORT).show();
    }

    private void showMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static final class MemberAdapter
            extends RecyclerView.Adapter<MemberAdapter.Holder> {

        private final List<GuildMember> items = new ArrayList<>();

        void submit(List<GuildMember> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemGuildMemberBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final ItemGuildMemberBinding binding;

            Holder(ItemGuildMemberBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(GuildMember member) {
                binding.tvMemberAvatar.setText(Texts.initial(member.name));
                binding.tvMemberName.setText(member.name);
                binding.tvMemberRole.setText(roleLabel(member.role));
                binding.tvMemberPoints.setText(binding.getRoot().getContext()
                        .getString(R.string.guild_points_short,
                                Texts.number(member.weeklyPoints)));
            }
        }
    }

    private static final class ChatAdapter
            extends RecyclerView.Adapter<ChatAdapter.Holder> {

        private final List<GuildChatMessage> items = new ArrayList<>();
        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        void submit(List<GuildChatMessage> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemGuildChatBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), timeFormat);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final ItemGuildChatBinding binding;

            Holder(ItemGuildChatBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(GuildChatMessage message, SimpleDateFormat timeFormat) {
                binding.tvChatName.setText(message.name);
                binding.tvChatText.setText(message.text);
                binding.tvChatTime.setText(message.at > 0
                        ? timeFormat.format(new Date(message.at)) : "");
            }
        }
    }
}
