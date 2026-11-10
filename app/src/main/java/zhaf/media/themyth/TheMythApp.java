package zhaf.media.themyth;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

public class TheMythApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (RuntimeException ignored) {
            // Persistence can only be enabled before any database usage.
        }
        FirebaseDatabase.getInstance().setLogLevel(
                BuildConfig.DEBUG ? Logger.Level.DEBUG : Logger.Level.NONE
        );
    }
}
