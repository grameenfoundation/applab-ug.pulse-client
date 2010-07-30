/**
 * Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
 */
package yo.applab.pulse;

import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;

public class Global {
    /** Network connection and read timeout **/
    public static final int TIMEOUT = 30000;
    public static String server_url;
    public static boolean cancel;
    public static boolean refresh;
    public static final String errorHtml = "<html>" + "<body>" + "<h1>Unable to establish a connection</h1>"
            + "<p><strong>Please try again later.</strong></p>" + "</body>" + "</html>";

    static String cachedImei;

    // TODO: move this into our shared client library call that runs on initialization 
    /**
     * Gets the IMEI from the phone and stores it; we need this to construct the
     * URL for server requests.
     */
    public static String getImei(Activity activity) {
        if (cachedImei == null && activity != null) {
            TelephonyManager telephonyManager = (TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE);
            cachedImei = telephonyManager.getDeviceId();
        }
        
        return cachedImei;
    }
}
