package zhaf.media.themyth.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static final String FILE = "themyth_prefs";
    private static final String KEY_SFX = "sfx_enabled";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_PERMISSION_ASKED = "notif_permission_asked";
    private static final String KEY_LAST_IDLE = "notif_last_idle";
    private static final String KEY_LAST_DAILY = "notif_last_daily";
    private static final String KEY_LAST_MISSION = "notif_last_mission";
    private static final String KEY_LAST_EVENT = "notif_last_event";
    private static final String KEY_LAST_MAIL = "notif_last_mail";

    private Prefs() {
    }

    private static SharedPreferences get(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean sfxEnabled(Context context) {
        return get(context).getBoolean(KEY_SFX, true);
    }

    public static void setSfxEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_SFX, enabled).apply();
    }

    public static boolean notificationsEnabled(Context context) {
        return get(context).getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public static boolean permissionAsked(Context context) {
        return get(context).getBoolean(KEY_PERMISSION_ASKED, false);
    }

    public static void setPermissionAsked(Context context, boolean asked) {
        get(context).edit().putBoolean(KEY_PERMISSION_ASKED, asked).apply();
    }

    public static long lastIdleMarker(Context context) {
        return get(context).getLong(KEY_LAST_IDLE, -1L);
    }

    public static void setLastIdleMarker(Context context, long marker) {
        get(context).edit().putLong(KEY_LAST_IDLE, marker).apply();
    }

    public static String lastDailyMarker(Context context) {
        return get(context).getString(KEY_LAST_DAILY, "");
    }

    public static void setLastDailyMarker(Context context, String marker) {
        get(context).edit().putString(KEY_LAST_DAILY, marker).apply();
    }

    public static String lastMissionMarker(Context context) {
        return get(context).getString(KEY_LAST_MISSION, "");
    }

    public static void setLastMissionMarker(Context context, String marker) {
        get(context).edit().putString(KEY_LAST_MISSION, marker).apply();
    }

    public static String lastEventMarker(Context context) {
        return get(context).getString(KEY_LAST_EVENT, "");
    }

    public static void setLastEventMarker(Context context, String marker) {
        get(context).edit().putString(KEY_LAST_EVENT, marker).apply();
    }

    public static String lastMailMarker(Context context) {
        return get(context).getString(KEY_LAST_MAIL, "");
    }

    public static void setLastMailMarker(Context context, String marker) {
        get(context).edit().putString(KEY_LAST_MAIL, marker).apply();
    }
}
