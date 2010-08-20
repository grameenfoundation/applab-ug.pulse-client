package applab.client;

import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;

public class Handset {
    static String cachedImei;

    /**
     * Get the cached IMEI value from either local storage or Activity initialization
     */
    public static String getImei() {
        if (cachedImei == null) {
            throw new IllegalStateException("getImei is only valid after an ApplabActivity has been initialized");
        }
        
        return cachedImei;
    }
    
    // overload that allows the user to pass in an Activity (used for non ApplabActivity codepaths)
    public static String getImei(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }
        initializeImei(activity);
        return getImei();
    }
    
    // internal method for initializing the IMEI from an activity. Called from ApplabActivity initialization
    static void initializeImei(Activity activity) {
        assert(activity != null);
        // initialize our IMEI so that we can access this information from 
        // non-Activity code without having to always thread a reference through
        if (cachedImei == null) {
            TelephonyManager telephonyManager = (TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE);
            cachedImei = telephonyManager.getDeviceId();
        }
    }
}
