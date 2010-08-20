package applab.client;

import android.app.Activity;

/**
 * An abstract base class to use for all of the Android Activities 
 * that power Applab applications.
 * 
 * There are cases where applications also provide their own intermediate 
 * base class to provide shared logic at the application-scope
 *
 */
public abstract class ApplabActivity extends Activity {
    protected ApplabActivity() {
        super();
        Handset.initializeImei(this);
    }
}