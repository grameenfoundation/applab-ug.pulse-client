package applab.pulse.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Created by mugume david on 04-12-2015.
 */
public class EventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()) {
                // Wifi is connected
                triggerServiceStart(context);
            }
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected()) {
                // mobile data is connected
                triggerServiceStart(context);
            }
        }
        else if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Alarm alarm = new Alarm();
            alarm.setAlarm(context);
            triggerServiceStart(context);
        }
    }

    private void triggerServiceStart(Context context) {
        Intent mIntentForService = new Intent(context, ConnectionService.class);
        mIntentForService.setAction("refresh");
        context.startService(mIntentForService);
    }
}
