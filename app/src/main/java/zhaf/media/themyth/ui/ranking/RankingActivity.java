package zhaf.media.themyth.ui.ranking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.List;

import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.core.FirebaseRefs;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.arena.ArenaRating;
import zhaf.media.themyth.data.repository.ArenaRepository;
import zhaf.media.themyth.databinding.ActivityRankingBinding;
import zhaf.media.themyth.databinding.ItemRankingBinding;
import zhaf.media.themyth.ui.login.LoginActivity;

public class RankingActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 12_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();

    private ActivityRankingBinding binding;
    private String uid;
    private String season;
    private DatabaseReference ownRef;
    private ValueEventListener ownListener;

    private boolean boardLoaded;
    private boolean ownLoaded;
    private int ownScore;
    private boolean ownExists;
    private List<ArenaRepository.LeaderRow> rows;

    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (boardLoaded || isFinishing() || isDestroyed()) {
                return;
            }
            showLoading(false);
            binding.rankingErrorBox.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = new AuthRepository(this).currentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        season = ArenaRating.seasonKey(System.currentTimeMillis());
        binding.btnRankingBack.setOnClickListener(v -> finish());
        binding.btnRankingRetry.setOnClickListener(v -> load());
        binding.tvRankingSeason.setText(getString(R.string.ranking_season,
                season.length() > 1 ? season.substring(1).replace('_', '.') : season));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) {
            return;
        }
        load();
    }

    @Override
    protected void onStop() {
        detachOwn();
        handler.removeCallbacks(loadTimeout);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(loadTimeout);
        super.onDestroy();
    }

    private void load() {
        boardLoaded = false;
        ownLoaded = false;
        rows = null;
        showLoading(true);
        binding.rankingErrorBox.setVisibility(View.GONE);
        binding.tvRankingEmpty.setVisibility(View.GONE);
        binding.tvRankingSelfTitle.setVisibility(View.GONE);
        binding.tvRankingSelf.setVisibility(View.GONE);
        binding.rankingList.removeAllViews();
        handler.removeCallbacks(loadTimeout);
        handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);

        ArenaRepository.loadLeaderboard(season, new ArenaRepository.LeaderboardCallback() {
            @Override
            public void onData(List<ArenaRepository.LeaderRow> data) {
                handler.removeCallbacks(loadTimeout);
                boardLoaded = true;
                rows = data;
                showLoading(false);
                render();
            }

            @Override
            public void onError(DatabaseError error) {
                handler.removeCallbacks(loadTimeout);
                boardLoaded = true;
                showLoading(false);
                binding.rankingErrorBox.setVisibility(View.VISIBLE);
                binding.tvRankingError.setText(UiErrors.database(RankingActivity.this, error));
            }
        });

        detachOwn();
        ownRef = FirebaseRefs.ranking(season, "arena", uid);
        ownListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ownLoaded = true;
                ownExists = snapshot.exists();
                Long score = snapshot.child("score").getValue(Long.class);
                ownScore = score == null ? 0 : score.intValue();
                render();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                ownLoaded = true;
                render();
            }
        };
        ownRef.addValueEventListener(ownListener);
    }

    private void render() {
        if (rows == null) {
            return;
        }
        binding.rankingList.removeAllViews();
        if (rows.isEmpty()) {
            binding.tvRankingEmpty.setVisibility(View.VISIBLE);
            binding.tvRankingSelfTitle.setVisibility(View.VISIBLE);
            binding.tvRankingSelf.setVisibility(View.VISIBLE);
            binding.tvRankingSelf.setText(ownLoaded && ownExists
                    ? getString(R.string.ranking_outside, numberFormat.format(ownScore))
                    : getString(R.string.ranking_unranked));
            return;
        }
        binding.tvRankingEmpty.setVisibility(View.GONE);

        int ownRank = -1;
        for (int i = 0; i < rows.size(); i++) {
            ArenaRepository.LeaderRow row = rows.get(i);
            ItemRankingBinding item = ItemRankingBinding.inflate(
                    LayoutInflater.from(this), binding.rankingList, false);
            item.tvRankPosition.setText(getString(R.string.ranking_position, i + 1));
            boolean self = uid.equals(row.uid);
            item.tvRankName.setText(self
                    ? row.username + " · " + getString(R.string.ranking_you)
                    : row.username);
            if (self) {
                ownRank = i + 1;
                item.tvRankName.setTextColor(getColor(R.color.cyan));
            }
            item.tvRankScore.setText(numberFormat.format(row.score));
            binding.rankingList.addView(item.getRoot());
        }

        binding.tvRankingSelfTitle.setVisibility(View.VISIBLE);
        binding.tvRankingSelf.setVisibility(View.VISIBLE);
        if (ownRank > 0) {
            binding.tvRankingSelf.setText(getString(R.string.ranking_self_row,
                    ownRank, numberFormat.format(rows.get(ownRank - 1).score)));
        } else if (ownExists) {
            binding.tvRankingSelf.setText(getString(R.string.ranking_outside,
                    numberFormat.format(ownScore)));
        } else {
            binding.tvRankingSelf.setText(R.string.ranking_unranked);
        }
    }

    private void detachOwn() {
        if (ownRef != null && ownListener != null) {
            ownRef.removeEventListener(ownListener);
        }
        ownRef = null;
        ownListener = null;
    }

    private void showLoading(boolean visible) {
        binding.progressRanking.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            binding.tvRankingEmpty.setVisibility(View.GONE);
            binding.tvRankingSelfTitle.setVisibility(View.GONE);
            binding.tvRankingSelf.setVisibility(View.GONE);
        }
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
