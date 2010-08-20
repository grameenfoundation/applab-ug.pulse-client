package applab.client;

import android.app.TabActivity;
import android.os.Bundle;

/**
 * An abstract base class to use for all of the Android Activities 
 * that power Applab applications.
 * 
 * There are cases where applications also provide their own intermediate 
 * base class to provide shared logic at the application-scope
 *
 */
public abstract class ApplabTabActivity extends TabActivity {
    protected ApplabTabActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplabActivity.initialize(this);
    }    
}