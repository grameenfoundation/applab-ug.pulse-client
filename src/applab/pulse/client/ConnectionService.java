package applab.pulse.client;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import applab.client.ApplabActivity;
import applab.client.pulse.R;

/**
 * Created by mugume david on 04-12-2015.
 */
public class ConnectionService extends Service {

    public static final int NOTIFICATION_ID = 0x2322;

    private PulseDataCollector dataCollector;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        getDataCollector().backgroundRefreshWithNotifications(new Notifier());
        return START_STICKY;
    }

    private PulseDataCollector getDataCollector() {
        if(dataCollector == null) {
            ApplabActivity.initialize(getApplicationContext());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            dataCollector = new PulseDataCollector(new Handler() {
                @Override
                public void handleMessage(Message message) {
                    handleDataCollectorMessage(message);
                }
            }, preferences.getString(Settings.KEY_SERVER, "http://grameenfoundation.force.com/pulse"));
        }
        return dataCollector;
    }

    private void handleDataCollectorMessage(Message message) {
        if (message.what == PulseDataCollector.UPDATES_DETECTED) {
            TabInfo.save(dataCollector.getTabList());
        }
    }

    protected class Notifier implements INotification {

        @Override
        public void notify(String param) {
            if(param != null) {
                final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                final Notification.Builder mBuilder = new Notification.Builder(ConnectionService.this);

                //vibrate
                final int dat = 70;
                final long[] pattern = {0, 3 * dat, dat, dat};
                mBuilder.setVibrate(pattern);

                //sound
                mBuilder.setSound(Uri.parse("content://settings/system/notification_sound"));

                mBuilder.setContentTitle(getString(R.string.new_notifications));

                String message = "";
                String startStr = "<br/></p><p>";
                String endStr = "</p></tr></td>";
                int start = param.indexOf(startStr) + startStr.length();
                int end = param.indexOf(endStr);
                message = param.substring(start, end);
                message = Html.fromHtml(message).toString();

                Log.d(ConnectionService.class.getSimpleName(), message);

                mBuilder.setContentText(message);
                mBuilder.setTicker(message.length() > 155 ? message.substring(0, 150) + "..." : message);

                Intent intent = new Intent(ConnectionService.this, PulseTabs.class);
                PendingIntent pintent = PendingIntent.getActivity(ConnectionService.this, 0, intent, 0);
                mBuilder.setContentIntent(pintent);

                mBuilder.setDefaults(0);
                mBuilder.setSmallIcon(R.drawable.icon);
                mBuilder.setLights(0xff00FF00, 2000, 3000);
                final Notification notification = mBuilder.getNotification();
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
            else {
                Log.d(ConnectionService.class.getSimpleName(), "refresh had no results");
            }
        }
    }
}
