package applab.pulse.client.service;

import java.util.Timer;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import applab.client.service.ApplabService;

/**
 * Needed to create ApplabSearchService mainly to handle synchronization. TODO: Should consider moving
 * SynchronizationManager to CommonClient, so that the ApplabService can access it
 *
 * @since 3.1
 */
public class ApplabPulseService extends ApplabService {
    private static final String TAG = "ApplabSearchService";

    public ApplabPulseService() {
        super();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "ApplabPulseService Created");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        Log.v(TAG, "ApplabPulseService -- onStart()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "ApplabSearchService Destroyed");
    }
}
