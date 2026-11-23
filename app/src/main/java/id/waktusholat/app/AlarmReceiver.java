package id.waktusholat.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    static final String CHANNEL = "zikir";

    @Override
    public void onReceive(Context c, Intent in) {
        int prayer = in.getIntExtra("prayer", -1);
        long at = in.getLongExtra("at", 0);
        // abaikan alarm basi (mis. perangkat baru menyala setelah waktunya lewat jauh)
        if (prayer >= 0 && prayer < 5 && (at == 0 || System.currentTimeMillis() - at < 60 * 60000L)) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Pengingat zikir", NotificationManager.IMPORTANCE_DEFAULT));
                Intent open = new Intent(c, MainActivity.class);
                open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                open.putExtra("prayer", PrayerTimes.IDS[prayer]);
                PendingIntent pi = PendingIntent.getActivity(c, 100 + prayer, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                String name = PrayerTimes.NAMES[prayer];
                Notification n = new Notification.Builder(c, CHANNEL)
                        .setSmallIcon(R.drawable.ic_notif)
                        .setContentTitle("Waktunya zikir ba’da " + name)
                        .setContentText("Ketuk untuk membuka zikir ba’da sholat " + name + ".")
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();
                nm.notify(200 + prayer, n);
            }
        }
        NotifScheduler.schedule(c); // perpanjang jadwal
    }
}
