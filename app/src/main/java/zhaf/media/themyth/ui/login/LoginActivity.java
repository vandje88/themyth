package zhaf.media.themyth.ui.login;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.core.UiErrors;
import zhaf.media.themyth.databinding.ActivityLoginBinding;
import zhaf.media.themyth.ui.home.HomeActivity;

public class LoginActivity extends AppCompatActivity {

    private static final int GOOGLE_DEVELOPER_ERROR = 10;
    private static final int GOOGLE_CANCELLED = 12501;

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private GoogleSignInClient googleClient;
    private String webClientId;
    private String pendingIdToken;
    private boolean busy;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onGoogleResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        webClientId = resolveWebClientId();

        binding.btnGuest.setOnClickListener(v -> startGuest());
        binding.btnGoogle.setOnClickListener(v -> startGoogle());

        FirebaseUser existing = authRepository.currentUser();
        if (existing != null) {
            setBusy(true, getString(R.string.login_already_signed_in));
            authRepository.ensureProfile(existing, baseCallback());
        }
    }

    private void startGuest() {
        if (busy) {
            return;
        }
        setBusy(true);
        authRepository.signInAsGuest(baseCallback());
    }

    private void startGoogle() {
        if (busy) {
            return;
        }
        if (webClientId == null || webClientId.trim().isEmpty()) {
            showGoogleSetupDialog();
            return;
        }
        setBusy(true);
        try {
            googleLauncher.launch(getGoogleClient().getSignInIntent());
        } catch (RuntimeException error) {
            setBusy(false);
            showError(getString(R.string.login_google_failed));
        }
    }

    private void onGoogleResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            setBusy(false);
            return;
        }

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account != null ? account.getIdToken() : null;
            if (idToken == null) {
                setBusy(false);
                showError(getString(R.string.login_google_failed));
                return;
            }
            pendingIdToken = idToken;
            authRepository.signInWithGoogleToken(idToken, baseCallback());
        } catch (ApiException error) {
            setBusy(false);
            int code = error.getStatusCode();
            if (code == GOOGLE_DEVELOPER_ERROR) {
                showGoogleSetupDialog();
                return;
            }
            if (code == GOOGLE_CANCELLED) {
                return;
            }
            showError(UiErrors.auth(this, error));
        }
    }

    private AuthRepository.Callback baseCallback() {
        return new AuthRepository.Callback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                goHome();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showError(message);
            }

            @Override
            public void onAccountConflict() {
                setBusy(false);
                new MaterialAlertDialogBuilder(LoginActivity.this)
                        .setTitle(R.string.login_conflict_title)
                        .setMessage(R.string.login_conflict_message)
                        .setPositiveButton(R.string.login_conflict_continue,
                                (dialog, which) -> switchToGoogleAccount())
                        .setNegativeButton(R.string.home_logout_cancel, null)
                        .show();
            }
        };
    }

    private void switchToGoogleAccount() {
        if (pendingIdToken == null) {
            showError(getString(R.string.login_google_failed));
            return;
        }
        setBusy(true);
        authRepository.signInWithGoogleTokenForce(pendingIdToken, baseCallback());
    }

    private void showGoogleSetupDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_google_unavailable_title)
                .setMessage(R.string.login_google_unavailable_message)
                .setPositiveButton(R.string.login_ok, null)
                .setNegativeButton(R.string.login_guest, (dialog, which) -> startGuest())
                .show();
    }

    private GoogleSignInClient getGoogleClient() {
        if (googleClient == null) {
            GoogleSignInOptions options =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestProfile()
                            .requestIdToken(webClientId)
                            .build();
            googleClient = GoogleSignIn.getClient(this, options);
        }
        return googleClient;
    }

    private String resolveWebClientId() {
        int resourceId = getResources()
                .getIdentifier("default_web_client_id", "string", getPackageName());
        if (resourceId == 0) {
            return null;
        }
        try {
            return getString(resourceId);
        } catch (Resources.NotFoundException error) {
            return null;
        }
    }

    private void setBusy(boolean value) {
        setBusy(value, getString(R.string.login_connecting));
    }

    private void setBusy(boolean value, String label) {
        busy = value;
        binding.loadingOverlay.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.tvLoading.setText(label);
        binding.btnGuest.setEnabled(!value);
        binding.btnGoogle.setEnabled(!value);
        binding.btnGuest.setAlpha(value ? 0.5f : 1f);
        binding.btnGoogle.setAlpha(value ? 0.5f : 1f);
        if (value) {
            hideError();
        }
    }

    private void showError(String message) {
        setBusy(false);
        binding.tvLoginError.setText(message);
        binding.tvLoginError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvLoginError.setVisibility(View.GONE);
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
