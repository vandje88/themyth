package zhaf.media.themyth.ui.mail;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.data.repository.MailRepository;
import zhaf.media.themyth.data.social.MailMessage;
import zhaf.media.themyth.databinding.ActivityMailBinding;
import zhaf.media.themyth.ui.login.LoginActivity;

public class MailActivity extends AppCompatActivity implements MailAdapter.Listener {

    private static final long LOAD_TIMEOUT_MS = 12_000L;

    private ActivityMailBinding binding;
    private MailRepository repository;
    private MailAdapter adapter;

    private MailRepository.Handle handle;
    private String uid;

    private final List<MailMessage> mails = new ArrayList<>();
    private boolean loaded;
    private boolean busy;
    private String busyId;
    private boolean seeded;
    private boolean deliveryChecked;
    private boolean cleanupDone;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (loaded || isFinishing() || isDestroyed()) {
                return;
            }
            showLoading(false);
            showError(getString(R.string.social_error_network));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goLogin();
            return;
        }
        uid = user.getUid();
        repository = new MailRepository();
        adapter = new MailAdapter(this);

        binding.mailList.setLayoutManager(new LinearLayoutManager(this));
        binding.mailList.setAdapter(adapter);

        binding.btnMailBack.setOnClickListener(v -> finish());
        binding.btnMailClaimAll.setOnClickListener(v -> claimAll());
        binding.btnMailRetry.setOnClickListener(v -> retry());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attach();
        if (!seeded && uid != null) {
            seeded = true;
            repository.seedWelcome(uid);
        }
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
            showLoading(true);
            armTimeout();
        }
        handle = MailRepository.observe(uid, new MailRepository.MailCallback() {
            @Override
            public void onData(List<MailMessage> data) {
                disarmTimeout();
                mails.clear();
                mails.addAll(data);
                loaded = true;
                showLoading(false);
                showError(null);
                render();
                if (!deliveryChecked) {
                    deliveryChecked = true;
                    repository.deliverPending(uid, mails, noopCallback());
                }
                if (!cleanupDone) {
                    cleanupDone = true;
                    repository.cleanup(uid, mails);
                }
            }

            @Override
            public void onError(DatabaseError error) {
                disarmTimeout();
                showLoading(false);
                showError(UiErrors.database(MailActivity.this, error));
            }
        });
    }

    private void detach() {
        disarmTimeout();
        if (handle != null) {
            handle.detach();
            handle = null;
        }
    }

    private void armTimeout() {
        timeoutHandler.removeCallbacks(loadTimeout);
        timeoutHandler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);
    }

    private void disarmTimeout() {
        timeoutHandler.removeCallbacks(loadTimeout);
    }

    @Override
    protected void onDestroy() {
        disarmTimeout();
        super.onDestroy();
    }

    private void retry() {
        detach();
        loaded = false;
        deliveryChecked = false;
        cleanupDone = false;
        attach();
    }

    private void render() {
        adapter.submit(mails);
        binding.tvMailEmpty.setVisibility(mails.isEmpty() ? View.VISIBLE : View.GONE);
        binding.mailList.setVisibility(mails.isEmpty() ? View.GONE : View.VISIBLE);
        binding.btnMailClaimAll.setEnabled(!busy && anyClaimable());
        binding.btnMailClaimAll.setAlpha(busy ? 0.6f : 1f);
    }

    private boolean anyClaimable() {
        long now = System.currentTimeMillis();
        for (MailMessage mail : mails) {
            if (mail.canClaim(now)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMailClick(MailMessage mail) {
        if (mail.read) {
            return;
        }
        repository.markRead(uid, mail.id);
    }

    @Override
    public void onClaim(MailMessage mail) {
        if (busy) {
            return;
        }
        beginBusy(mail.id);
        repository.claim(uid, mail, new MailRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                releaseBusy();
                showMessage(R.string.mail_claim_success);
            }

            @Override
            public void onError(@StringRes int messageRes) {
                releaseBusy();
                showMessage(messageRes);
            }
        });
    }

    private void claimAll() {
        if (busy) {
            return;
        }
        if (!anyClaimable()) {
            showMessage(R.string.mail_nothing_claim);
            return;
        }
        beginBusy(null);
        repository.claimAll(uid, new ArrayList<>(mails),
                new MailRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        releaseBusy();
                        showMessage(R.string.mail_claim_all_success);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        releaseBusy();
                        showMessage(messageRes);
                    }
                });
    }

    private void beginBusy(String mailId) {
        busy = true;
        busyId = mailId;
        adapter.setBusyId(busyId);
        binding.btnMailClaimAll.setEnabled(false);
        binding.btnMailClaimAll.setAlpha(0.6f);
    }

    private void releaseBusy() {
        busy = false;
        busyId = null;
        adapter.setBusyId(null);
        binding.btnMailClaimAll.setEnabled(anyClaimable());
        binding.btnMailClaimAll.setAlpha(1f);
    }

    private MailRepository.ActionCallback noopCallback() {
        return new MailRepository.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(@StringRes int messageRes) {
            }
        };
    }

    private void showLoading(boolean show) {
        binding.progressMail.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (message == null) {
            binding.mailErrorBox.setVisibility(View.GONE);
            binding.tvMailEmpty.setVisibility(mails.isEmpty() && loaded
                    ? View.VISIBLE : View.GONE);
            binding.mailList.setVisibility(mails.isEmpty() ? View.GONE : View.VISIBLE);
            return;
        }
        binding.tvMailError.setText(message);
        binding.mailErrorBox.setVisibility(View.VISIBLE);
        binding.tvMailEmpty.setVisibility(View.GONE);
        binding.mailList.setVisibility(View.GONE);
    }

    private void showMessage(@StringRes int messageRes) {
        Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_SHORT).show();
    }

    private void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
