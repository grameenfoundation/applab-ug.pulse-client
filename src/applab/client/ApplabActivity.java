package applab.client;

import android.app.Activity;
import android.os.Bundle;

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
        ApplabActivity.initialize(this);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplabActivity.initialize(this);
    }
    
    static void initialize(Activity activity) {
        Handset.initializeImei(activity);    	
    }
}