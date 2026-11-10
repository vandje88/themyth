package zhaf.media.themyth.core;

import android.content.Context;

import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApiNotAvailableException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DatabaseError;

import zhaf.media.themyth.R;

public final class UiErrors {

    private UiErrors() {
    }

    public static String auth(Context context, Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return context.getString(R.string.error_network);
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return context.getString(R.string.error_too_many_requests);
        }
        if (error instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.error_session);
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.error_invalid_credential);
        }
        if (error instanceof FirebaseApiNotAvailableException) {
            return context.getString(R.string.error_play_services);
        }
        if (error instanceof ApiException) {
            int status = ((ApiException) error).getStatusCode();
            if (status == 12501) {
                return context.getString(R.string.login_cancelled);
            }
            if (status == 7) {
                return context.getString(R.string.error_network);
            }
        }
        return context.getString(R.string.error_generic);
    }

    public static String database(Context context, DatabaseError error) {
        if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
            return context.getString(R.string.error_db_permission);
        }
        if (error.getCode() == DatabaseError.DISCONNECTED) {
            return context.getString(R.string.error_db_disconnected);
        }
        return context.getString(R.string.error_db_generic);
    }
}
