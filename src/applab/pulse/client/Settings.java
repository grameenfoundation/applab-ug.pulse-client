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

package applab.pulse.client;

import java.net.MalformedURLException;
import java.net.URL;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import applab.client.PropertyStorage;
import applab.client.pulse.R;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static String KEY_SERVER = "server";
    public static final int DONE_ID = Menu.FIRST;
    private static String cachedServerUrl;
    private static final String defaultServer = "http://ckwapps.applab.org:8888";
    boolean preferencesRefreshed;

    public static String getServerUrl() {
        if (cachedServerUrl == null) {
            cachedServerUrl = PropertyStorage.getLocal().getValue(KEY_SERVER, defaultServer);
        }

        return cachedServerUrl;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setTitle(getString(R.string.settings));
        refreshSetting();
    }

    private void refreshSetting() {
        EditTextPreference etp = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_SERVER);
        String textContents = etp.getText().trim();
        if (isValidUrl(textContents)) {
            etp.setText(textContents);
            etp.setSummary(textContents);
            preferencesRefreshed = true;
            PropertyStorage.getLocal().setValue(KEY_SERVER, textContents);
            cachedServerUrl = textContents;
            preferencesRefreshed = false;
        }
        else {
            etp.setText((String)etp.getSummary());
            Toast.makeText(getApplicationContext(), getString(R.string.invalid_url), Toast.LENGTH_SHORT).show();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!preferencesRefreshed) {
            refreshSetting();
        }
    }

    private static boolean isValidUrl(String url) {

        try {
            new URL(url);
            return true;
        }
        catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, DONE_ID, 0, getString(R.string.done)).setIcon(R.drawable.done);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case DONE_ID:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
}
