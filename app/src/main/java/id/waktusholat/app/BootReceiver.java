package id.waktusholat.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Jadwalkan ulang setelah perangkat menyala, aplikasi diperbarui, atau zona waktu berubah. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        NotifScheduler.schedule(c);
    }
}
