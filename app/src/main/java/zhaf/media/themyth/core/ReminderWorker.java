package zhaf.media.themyth.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import java.util.concurrent.TimeUnit;

import zhaf.media.themyth.R;
import zhaf.media.themyth.data.event.EventDef;
import zhaf.media.themyth.data.event.EventLogic;
import zhaf.media.themyth.data.meta.IdleRewardLogic;
import zhaf.media.themyth.data.meta.MetaClock;
import zhaf.media.themyth.data.meta.MissionCatalog;
import zhaf.media.themyth.data.meta.MissionDef;
import zhaf.media.themyth.data.meta.MissionLogic;

public class ReminderWorker extends Worker {

    private static final String WORK_NAME = "themyth_reminders";
    private static final long TIMEOUT_SECONDS = 12L;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1L, TimeUnit.HOURS, 15L, TimeUnit.MINUTES)
                .setInitialDelay(10L, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !Notifications.canPost(context)) {
            return Result.success();
        }
        try {
            DataSnapshot player = Tasks.await(
                    FirebaseRefs.player(user.getUid()).get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long now = System.currentTimeMillis();
            checkIdle(context, player, now);
            checkDailyLogin(context, player, now);
            checkDailyMissions(context, player, now);
            checkEvent(context, now);
            checkMail(context, user.getUid());
        } catch (Exception ignored) {
            // Reminders retry on the next scheduled run.
        }
        return Result.success();
    }

    private void checkIdle(Context context, DataSnapshot player, long now) {
        DataSnapshot meta = player.child("meta");
        long idleAt = longOf(meta.child("idle").child("at").getValue(), 0L);
        int level = Math.max(1, (int) longOf(
                player.child("profile").child("level").getValue(), 1L));
        if (idleAt <= 0 || idleAt == Prefs.lastIdleMarker(context)) {
            return;
        }
        long accrued = IdleRewardLogic.accruedMs(idleAt, now, level);
        if (!IdleRewardLogic.ready(accrued)) {
            return;
        }
        if (Notifications.post(context, Notifications.ID_IDLE,
                context.getString(R.string.notif_idle_title),
                context.getString(R.string.notif_idle_text))) {
            Prefs.setLastIdleMarker(context, idleAt);
        }
    }

    private void checkDailyLogin(Context context, DataSnapshot player, long now) {
        String today = MetaClock.dayKey(now);
        String lastDay = player.child("meta").child("login").child("lastDay")
                .getValue(String.class);
        if (today.equals(lastDay) || today.equals(Prefs.lastDailyMarker(context))) {
            return;
        }
        if (Notifications.post(context, Notifications.ID_DAILY,
                context.getString(R.string.notif_daily_title),
                context.getString(R.string.notif_daily_text))) {
            Prefs.setLastDailyMarker(context, today);
        }
    }

    private void checkDailyMissions(Context context, DataSnapshot player, long now) {
        String today = MetaClock.dayKey(now);
        String marker = today + "_missions";
        if (marker.equals(Prefs.lastMissionMarker(context))) {
            return;
        }
        DataSnapshot progress = player.child("meta").child("missions").child("d").child(today);
        DataSnapshot claims = player.child("meta").child("claims");
        boolean claimable = false;
        for (MissionDef def : MissionCatalog.daily()) {
            int value = intOf(progress.child(def.id).getValue(), 0);
            boolean claimed = Boolean.TRUE.equals(
                    claims.child(today + "_" + def.id).getValue(Boolean.class));
            if (MissionLogic.canClaim(value, def.target, claimed)) {
                claimable = true;
                break;
            }
        }
        if (claimable && Notifications.post(context, Notifications.ID_MISSION,
                context.getString(R.string.notif_mission_title),
                context.getString(R.string.notif_mission_text))) {
            Prefs.setLastMissionMarker(context, marker);
        }
    }

    private void checkEvent(Context context, long now) {
        EventDef event = EventLogic.activeEvent(now);
        if (event == null || event.id.equals(Prefs.lastEventMarker(context))) {
            return;
        }
        if (Notifications.post(context, Notifications.ID_EVENT,
                context.getString(R.string.notif_event_title, event.title()),
                event.description())) {
            Prefs.setLastEventMarker(context, event.id);
        }
    }

    private void checkMail(Context context, String uid) throws Exception {
        DataSnapshot mail = Tasks.await(
                FirebaseRefs.mail(uid).get(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
        int unread = 0;
        long newest = 0L;
        for (DataSnapshot child : mail.getChildren()) {
            if (!Boolean.TRUE.equals(child.child("read").getValue(Boolean.class))) {
                unread++;
                newest = Math.max(newest, longOf(child.child("createdAt").getValue(), 0L));
            }
        }
        String marker = String.valueOf(newest);
        if (unread == 0 || marker.equals(Prefs.lastMailMarker(context))) {
            return;
        }
        if (Notifications.post(context, Notifications.ID_MAIL,
                context.getString(R.string.notif_mail_title),
                context.getResources().getQuantityString(R.plurals.notif_mail_text,
                        unread, unread))) {
            Prefs.setLastMailMarker(context, marker);
        }
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longOf(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}
