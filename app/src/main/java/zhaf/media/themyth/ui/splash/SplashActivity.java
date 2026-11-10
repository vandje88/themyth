package zhaf.media.themyth.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import zhaf.media.themyth.BuildConfig;
import zhaf.media.themyth.R;
import zhaf.media.themyth.auth.AuthRepository;
import zhaf.media.themyth.databinding.ActivitySplashBinding;
import zhaf.media.themyth.ui.home.HomeActivity;
import zhaf.media.themyth.ui.login.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private AuthRepository authRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private boolean navigating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.tvVersion.setText(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
        binding.btnSplashRetry.setOnClickListener(v -> route());

        binding.imgEmblem.setAlpha(0f);
        binding.imgEmblem.setScaleX(0.8f);
        binding.imgEmblem.setScaleY(0.8f);
        binding.imgEmblem.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500L)
                .start();

        handler.postDelayed(this::route, 450L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void route() {
        if (navigating || busy) {
            return;
        }
        setBusy(true);

        FirebaseUser user = authRepository.currentUser();
        if (user == null) {
            goLogin();
            return;
        }

        authRepository.ensureProfile(user, new AuthRepository.Callback() {
            @Override
            public void onSuccess(FirebaseUser ignored) {
                goHome();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        binding.progressSplash.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.btnSplashRetry.setVisibility(View.GONE);
        binding.tvSplashStatus.setText(R.string.splash_preparing);
        binding.tvSplashStatus.setTextColor(getColor(R.color.text_muted));
    }

    private void showError(String message) {
        busy = false;
        binding.progressSplash.setVisibility(View.GONE);
        binding.tvSplashStatus.setText(message);
        binding.tvSplashStatus.setTextColor(getColor(R.color.danger));
        binding.btnSplashRetry.setVisibility(View.VISIBLE);
    }

    private void goLogin() {
        navigating = true;
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goHome() {
        navigating = true;
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
