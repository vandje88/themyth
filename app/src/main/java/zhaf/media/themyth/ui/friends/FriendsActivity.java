package zhaf.media.themyth.ui.friends;

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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.model.PlayerProfile;
import zhaf.media.themyth.data.repository.SocialRepository;
import zhaf.media.themyth.data.social.DirectoryEntry;
import zhaf.media.themyth.data.social.FriendEntry;
import zhaf.media.themyth.data.social.FriendLogic;
import zhaf.media.themyth.data.social.FriendRequest;
import zhaf.media.themyth.data.social.GiftEntry;
import zhaf.media.themyth.databinding.ActivityFriendsBinding;
import zhaf.media.themyth.databinding.ItemFriendBinding;
import zhaf.media.themyth.databinding.ItemFriendRequestBinding;
import zhaf.media.themyth.ui.login.LoginActivity;

public class FriendsActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 12_000L;

    private ActivityFriendsBinding binding;
    private SocialRepository repository;
    private RequestAdapter requestAdapter;
    private FriendAdapter friendAdapter;

    private SocialRepository.Handle handle;
    private String uid;
    private String myName = "Pemanggil";
    private int myLevel = 1;

    private SocialRepository.State state;
    private DirectoryEntry searchResult;
    private boolean loaded;
    private boolean actionBusy;
    private boolean searchBusy;
    private boolean giftClaiming;
    private boolean profileRequested;
    private final Set<String> giftAttempts = new HashSet<>();

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (loaded || isFinishing() || isDestroyed()) {
                return;
            }
            binding.progressFriends.setVisibility(View.GONE);
            binding.friendsErrorBox.setVisibility(View.VISIBLE);
            binding.friendsContent.setVisibility(View.GONE);
        }
    };
    private final Runnable searchTimeout = new Runnable() {
        @Override
        public void run() {
            if (!searchBusy || isFinishing() || isDestroyed()) {
                return;
            }
            searchBusy = false;
            binding.btnFriendSearch.setEnabled(true);
            binding.btnFriendSearch.setAlpha(1f);
            showSearchError(R.string.social_error_network);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        repository = new SocialRepository();

        requestAdapter = new RequestAdapter(new RequestAdapter.Listener() {
            @Override
            public void onAccept(FriendRequest request) {
                accept(request);
            }

            @Override
            public void onDecline(FriendRequest request) {
                decline(request);
            }
        });
        friendAdapter = new FriendAdapter(new FriendAdapter.Listener() {
            @Override
            public void onGift(FriendEntry friend) {
                sendGift(friend);
            }

            @Override
            public void onRemove(FriendEntry friend) {
                confirmRemove(friend);
            }
        });

        binding.requestsList.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsList.setAdapter(requestAdapter);
        binding.friendsList.setLayoutManager(new LinearLayoutManager(this));
        binding.friendsList.setAdapter(friendAdapter);

        binding.btnFriendsBack.setOnClickListener(v -> finish());
        binding.btnFriendSearch.setOnClickListener(v -> search());
        binding.btnFriendsRetry.setOnClickListener(v -> retry());
        binding.btnFriendAdd.setOnClickListener(v -> addSearched());
        binding.etFriendUid.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attach();
        loadProfile();
    }

    @Override
    protected void onStop() {
        detach();
        super.onStop();
    }

    private void attach() {
        if (handle != null || uid == null) {
            return;
        }
        if (!loaded) {
            binding.progressFriends.setVisibility(View.VISIBLE);
            armLoadTimeout();
        }
        handle = SocialRepository.observe(uid, new SocialRepository.StateCallback() {
            @Override
            public void onData(SocialRepository.State data) {
                disarmLoadTimeout();
                state = data;
                loaded = true;
                binding.progressFriends.setVisibility(View.GONE);
                binding.friendsErrorBox.setVisibility(View.GONE);
                binding.friendsContent.setVisibility(View.VISIBLE);
                render();
                maybeClaimGifts();
            }

            @Override
            public void onError(DatabaseError error) {
                disarmLoadTimeout();
                binding.progressFriends.setVisibility(View.GONE);
                binding.friendsErrorBox.setVisibility(View.VISIBLE);
                binding.friendsContent.setVisibility(View.GONE);
            }
        });
    }

    private void detach() {
        disarmLoadTimeout();
        timeoutHandler.removeCallbacks(searchTimeout);
        if (handle != null) {
            handle.detach();
            handle = null;
        }
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
        timeoutHandler.removeCallbacks(searchTimeout);
        super.onDestroy();
    }

    private void retry() {
        detach();
        loaded = false;
        attach();
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
            }

            @Override
            public void onCancelled(DatabaseError error) {
                publishDirectory();
            }
        });
    }

    private void publishDirectory() {
        FirebaseRefs.guildOf(uid).child("id").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        SocialRepository.publishDirectory(uid, myName, myLevel,
                                snapshot.getValue(String.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        SocialRepository.publishDirectory(uid, myName, myLevel, null);
                    }
                });
    }

    private void render() {
        if (state == null) {
            return;
        }
        binding.tvFriendsCount.setText(getString(R.string.friends_count,
                state.friends.size(), FriendLogic.MAX_FRIENDS));

        requestAdapter.submit(state.requests);
        binding.tvRequestsEmpty.setVisibility(
                state.requests.isEmpty() ? View.VISIBLE : View.GONE);

        friendAdapter.submit(state.friends);
        binding.tvFriendsEmpty.setVisibility(
                state.friends.isEmpty() ? View.VISIBLE : View.GONE);

        if (state.outgoing.isEmpty()) {
            binding.tvOutgoing.setVisibility(View.GONE);
        } else {
            binding.tvOutgoing.setVisibility(View.VISIBLE);
            binding.tvOutgoing.setText(getString(R.string.friends_outgoing,
                    state.outgoing.size()));
        }
        renderSearch();
    }

    private void renderSearch() {
        if (searchResult == null) {
            binding.searchResultCard.setVisibility(View.GONE);
            return;
        }
        if (searchResult.uid.equals(uid)) {
            binding.searchResultCard.setVisibility(View.GONE);
            showSearchError(R.string.friends_error_self);
            return;
        }
        binding.searchResultCard.setVisibility(View.VISIBLE);
        binding.tvSearchName.setText(searchResult.name);
        binding.tvSearchMeta.setText(getString(R.string.friends_level,
                searchResult.level));

        boolean friend = state != null && state.isFriend(searchResult.uid);
        boolean outgoing = state != null && state.hasOutgoing(searchResult.uid);
        boolean incoming = state != null && state.hasIncoming(searchResult.uid);
        if (friend) {
            binding.btnFriendAdd.setText(R.string.friends_friend);
            binding.btnFriendAdd.setEnabled(false);
        } else if (outgoing) {
            binding.btnFriendAdd.setText(R.string.friends_pending);
            binding.btnFriendAdd.setEnabled(false);
        } else if (incoming) {
            binding.btnFriendAdd.setText(R.string.friends_pending);
            binding.btnFriendAdd.setEnabled(false);
        } else {
            binding.btnFriendAdd.setText(R.string.friends_add);
            binding.btnFriendAdd.setEnabled(!actionBusy);
        }
        binding.btnFriendAdd.setAlpha(binding.btnFriendAdd.isEnabled() ? 1f : 0.6f);
    }

    private void search() {
        if (searchBusy) {
            return;
        }
        String query = binding.etFriendUid.getText() == null
                ? "" : binding.etFriendUid.getText().toString().trim();
        searchResult = null;
        renderSearch();
        hideSearchError();
        if (!FriendLogic.validUid(query)) {
            showSearchError(R.string.friends_error_uid);
            return;
        }
        searchBusy = true;
        binding.btnFriendSearch.setEnabled(false);
        binding.btnFriendSearch.setAlpha(0.6f);
        timeoutHandler.postDelayed(searchTimeout, LOAD_TIMEOUT_MS);
        repository.search(query, new SocialRepository.DirectoryCallback() {
            @Override
            public void onData(DirectoryEntry entry) {
                timeoutHandler.removeCallbacks(searchTimeout);
                searchBusy = false;
                binding.btnFriendSearch.setEnabled(true);
                binding.btnFriendSearch.setAlpha(1f);
                searchResult = entry;
                renderSearch();
            }

            @Override
            public void onError(@StringRes int messageRes) {
                timeoutHandler.removeCallbacks(searchTimeout);
                searchBusy = false;
                binding.btnFriendSearch.setEnabled(true);
                binding.btnFriendSearch.setAlpha(1f);
                showSearchError(messageRes);
            }
        });
    }

    private void addSearched() {
        if (actionBusy || searchResult == null || state == null) {
            return;
        }
        int check = FriendLogic.requestCheck(uid, searchResult.uid, true,
                state.isFriend(searchResult.uid), state.hasOutgoing(searchResult.uid),
                state.hasIncoming(searchResult.uid), state.friends.size());
        int message = checkMessage(check);
        if (message != 0) {
            showMessage(message);
            return;
        }
        actionBusy = true;
        binding.btnFriendAdd.setEnabled(false);
        repository.sendRequest(uid, myName, searchResult,
                new SocialRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionBusy = false;
                        showMessage(R.string.friends_sent);
                        renderSearch();
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        actionBusy = false;
                        renderSearch();
                        showMessage(messageRes);
                    }
                });
    }

    private void accept(FriendRequest request) {
        if (actionBusy) {
            return;
        }
        actionBusy = true;
        repository.acceptRequest(uid, myName, request,
                new SocialRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionBusy = false;
                        showMessage(R.string.friends_accepted);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        actionBusy = false;
                        showMessage(messageRes);
                    }
                });
    }

    private void decline(FriendRequest request) {
        if (actionBusy) {
            return;
        }
        actionBusy = true;
        repository.declineRequest(uid, request,
                new SocialRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionBusy = false;
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        actionBusy = false;
                        showMessage(messageRes);
                    }
                });
    }

    private void sendGift(FriendEntry friend) {
        if (actionBusy) {
            return;
        }
        if (!friend.giftReady(System.currentTimeMillis())) {
            showMessage(R.string.friends_gift_done);
            return;
        }
        actionBusy = true;
        repository.sendGift(uid, myName, friend,
                new SocialRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionBusy = false;
                        showMessage(R.string.friends_gift_sent);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        actionBusy = false;
                        showMessage(messageRes);
                    }
                });
    }

    private void confirmRemove(FriendEntry friend) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.friends_remove)
                .setMessage(getString(R.string.friends_remove_confirm, friend.name))
                .setNegativeButton(R.string.guild_cancel, null)
                .setPositiveButton(R.string.friends_remove, (dialog, which) ->
                        remove(friend))
                .show();
    }

    private void remove(FriendEntry friend) {
        if (actionBusy) {
            return;
        }
        actionBusy = true;
        repository.removeFriend(uid, friend.uid, new SocialRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                actionBusy = false;
                showMessage(R.string.friends_removed);
            }

            @Override
            public void onError(@StringRes int messageRes) {
                actionBusy = false;
                showMessage(messageRes);
            }
        });
    }

    private void maybeClaimGifts() {
        if (giftClaiming || state == null || state.gifts.isEmpty()) {
            return;
        }
        List<GiftEntry> pending = new ArrayList<>();
        for (GiftEntry gift : state.gifts) {
            if (giftAttempts.add(gift.key())) {
                pending.add(gift);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        giftClaiming = true;
        repository.claimGifts(uid, pending, new SocialRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                giftClaiming = false;
                showMessage(getString(R.string.friends_gift_received, pending.size(),
                        Texts.number((long) pending.size() * FriendLogic.GIFT_COIN)));
            }

            @Override
            public void onError(@StringRes int messageRes) {
                giftClaiming = false;
                showMessage(messageRes);
            }
        });
    }

    @StringRes
    private static int checkMessage(int check) {
        switch (check) {
            case FriendLogic.SELF:
                return R.string.friends_error_self;
            case FriendLogic.MISSING:
                return R.string.friends_error_missing;
            case FriendLogic.ALREADY_FRIEND:
                return R.string.friends_error_exists;
            case FriendLogic.PENDING:
                return R.string.friends_error_pending;
            case FriendLogic.INCOMING_PENDING:
                return R.string.friends_error_incoming;
            case FriendLogic.FULL:
                return R.string.friends_error_full;
            default:
                return 0;
        }
    }

    private void showSearchError(@StringRes int messageRes) {
        binding.tvSearchError.setText(messageRes);
        binding.tvSearchError.setVisibility(View.VISIBLE);
    }

    private void hideSearchError() {
        binding.tvSearchError.setVisibility(View.GONE);
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

    private static final class RequestAdapter
            extends RecyclerView.Adapter<RequestAdapter.Holder> {

        interface Listener {
            void onAccept(FriendRequest request);

            void onDecline(FriendRequest request);
        }

        private final List<FriendRequest> items = new ArrayList<>();
        private final Listener listener;

        RequestAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<FriendRequest> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemFriendRequestBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final ItemFriendRequestBinding binding;

            Holder(ItemFriendRequestBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(FriendRequest request, Listener listener) {
                binding.tvRequestAvatar.setText(Texts.initial(request.name));
                binding.tvRequestName.setText(request.name);
                binding.tvRequestUid.setText(request.uid);
                binding.btnRequestAccept.setOnClickListener(
                        v -> listener.onAccept(request));
                binding.btnRequestDecline.setOnClickListener(
                        v -> listener.onDecline(request));
            }
        }
    }

    private static final class FriendAdapter
            extends RecyclerView.Adapter<FriendAdapter.Holder> {

        interface Listener {
            void onGift(FriendEntry friend);

            void onRemove(FriendEntry friend);
        }

        private final List<FriendEntry> items = new ArrayList<>();
        private final Listener listener;

        FriendAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<FriendEntry> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemFriendBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final ItemFriendBinding binding;

            Holder(ItemFriendBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(FriendEntry friend, Listener listener) {
                binding.tvFriendAvatar.setText(Texts.initial(friend.name));
                binding.tvFriendName.setText(friend.name);
                binding.tvFriendUid.setText(friend.uid);
                boolean ready = friend.giftReady(System.currentTimeMillis());
                binding.btnFriendGift.setText(ready
                        ? R.string.friends_gift : R.string.friends_gift_done);
                binding.btnFriendGift.setEnabled(ready);
                binding.btnFriendGift.setAlpha(ready ? 1f : 0.55f);
                binding.btnFriendGift.setOnClickListener(v -> listener.onGift(friend));
                binding.btnFriendRemove.setOnClickListener(
                        v -> listener.onRemove(friend));
            }
        }
    }
}
