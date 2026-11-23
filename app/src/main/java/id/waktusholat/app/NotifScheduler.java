package id.waktusholat.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;
import java.util.TimeZone;

/** Menjadwalkan notifikasi zikir ba'da sholat (7 hari ke depan; diperpanjang tiap alarm berbunyi). */
public final class NotifScheduler {
    private NotifScheduler() {}

    static final String PREFS = "notif";
    private static final int DAYS = 7;
    private static final int MAX_CODES = 64;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static boolean hasLocation(SharedPreferences p) { return p.contains("lat") && p.contains("lon"); }

    /** Waktu sholat hari ini (jam:menit) untuk lokasi tersimpan. */
    public static String[] todayTimes(Context c) {
        SharedPreferences p = prefs(c);
        if (!hasLocation(p)) return null;
        Calendar now = Calendar.getInstance();
        double tz = TimeZone.getDefault().getOffset(now.getTimeInMillis()) / 3600000.0;
        double[] t = PrayerTimes.compute(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), Double.longBitsToDouble(p.getLong("lat", 0)),
                Double.longBitsToDouble(p.getLong("lon", 0)), tz);
        String[] out = new String[5];
        for (int i = 0; i < 5; i++) {
            if (Double.isNaN(t[i])) { out[i] = "--:--"; continue; }
            int mins = (int) Math.round(t[i] * 60);
            out[i] = String.format("%02d:%02d", (mins / 60) % 24, mins % 60);
        }
        return out;
    }

    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < MAX_CODES; i++) am.cancel(pending(c, i, -1, 0));

        SharedPreferences p = prefs(c);
        if (!p.getBoolean("on", false) || !hasLocation(p)) return;
        double lat = Double.longBitsToDouble(p.getLong("lat", 0));
        double lon = Double.longBitsToDouble(p.getLong("lon", 0));
        int delay = p.getInt("delay", 15);
        int mask = p.getInt("mask", 31);
        long now = System.currentTimeMillis();
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();

        int code = 0;
        for (int day = 0; day < DAYS && code < MAX_CODES; day++) {
            Calendar d = Calendar.getInstance();
            d.add(Calendar.DAY_OF_MONTH, day);
            double tz = TimeZone.getDefault().getOffset(d.getTimeInMillis()) / 3600000.0;
            double[] t = PrayerTimes.compute(d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
                    d.get(Calendar.DAY_OF_MONTH), lat, lon, tz);
            d.set(Calendar.HOUR_OF_DAY, 0); d.set(Calendar.MINUTE, 0); d.set(Calendar.SECOND, 0); d.set(Calendar.MILLISECOND, 0);
            long midnight = d.getTimeInMillis();
            for (int i = 0; i < 5; i++) {
                if ((mask & (1 << i)) == 0 || Double.isNaN(t[i])) continue;
                long at = midnight + Math.round((t[i] * 60 + delay) * 60000.0);
                if (at <= now) continue;
                PendingIntent pi = pending(c, code++, i, at);
                try {
                    if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } catch (SecurityException e) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                }
                if (code >= MAX_CODES) break;
            }
        }
    }

    private static PendingIntent pending(Context c, int code, int prayer, long at) {
        Intent it = new Intent(c, AlarmReceiver.class);
        it.putExtra("prayer", prayer);
        it.putExtra("at", at);
        return PendingIntent.getBroadcast(c, code, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
