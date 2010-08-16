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

import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import applab.client.Handset;

public class PulseTabs extends TabActivity {
    private final String TAG = "PulseTabs";
    private Locate locate;
    private static final int PROGRESS_DIALOG = 1;
    private static final int ABOUT_ID = Menu.FIRST;
    private static final int REFRESH_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int EXIT_ID = Menu.FIRST + 3;
    private PulseDataCollector dataCollector;
    private List<TabInfo> tabList;

    public PulseTabs() {
        super();
    }

    /**
     * Runs when the application is launched.
     * 
     * Responsible for getting and saving a few global settings, and setting up the UI: getting a list of tabs from the
     * dataCollector, and calling dataCollector to load their contents.
     * 
     * @param savedInstanceState
     *            Saved state from the last run of the application. We are not using this yet, but we may do so in order
     *            to save the contents of the tabs between application runs.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.dataCollector = new PulseDataCollector(handler);
        TabHost tabHost = this.getTabHost();
        LayoutInflater.from(this).inflate(R.layout.tabs, tabHost.getTabContentView(), true);

        // populate IMEI setting (TODO: we need a TabActivity subclass to handle this in applab.client)
        Handset.getImei(this);

        // populate global server URL
        getServerUrl();

        // create Locate object
        this.locate = new Locate((LocationManager)getSystemService(Context.LOCATION_SERVICE));

        // Get saved tab data in case there was a configuration (orientation)
        // change so we don't reload it from the network
        List<TabInfo> savedTabData = (List<TabInfo>)getLastNonConfigurationInstance();
        if (savedTabData == null) {
            // create and populate tabs
            createTabs();
            refreshTabData();
        }
        else {
            this.tabList = savedTabData;
            createTabs();
            updateTabs();
        }
    }

    /**
     * Creates tabs based on the list retrieved by the dataCollector
     */
    private void createTabs() {
        if (tabList == null) {
            tabList = dataCollector.getTabList();
        }
        TabHost tabHost = getTabHost();

        for (int i = 0; i < tabList.size(); i++) {
            TabInfo tabInfo = tabList.get(i);
            TabSpec tabSpec = tabHost.newTabSpec(tabInfo.tag);
            tabSpec.setIndicator(getString(tabInfo.indicator));
            if (!tabInfo.supportsRefresh) { // this is the Support tab
                Intent intent = new Intent(this, SupportForm.class);
                intent.putExtra("tab", "support");
                tabSpec.setContent(intent);
            }
            else {
                tabSpec.setContent(tabInfo.contentID);
            }
            tabHost.addTab(tabSpec);
        }
    }

    /**
     * Displays a progress dialog and then calls the dataCollector to refresh the data asynchronously
     */
    private void refreshTabData() {
        showDialog(PROGRESS_DIALOG);
        Global.cancel = false;
        Global.refresh = false;
        Log.i(TAG, "updating tab data...");
        dataCollector.backgroundRefresh();
    }

    /**
     * Populates tabs with data that has already been retrieved in the background by the dataCollector
     */
    private void updateTabs() {
        for (int i = 0; i < tabList.size(); i++) {
            TabInfo tabInfo = tabList.get(i);

            if (tabInfo.supportsRefresh) { // short term hack to skip support
                // tab; fix!
                WebView browser = (WebView)findViewById(tabInfo.contentID);
                String tabData = tabInfo.storedTabData == null ? Global.errorHtml : tabInfo.storedTabData;
                browser.loadData(tabData, "text/html", "UTF-8");
            }
        }
    }

    /**
     * Creates and stores the base URL to use for server requests.
     */
    private void getServerUrl() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String url = settings.getString(Settings.KEY_SERVER, getString(R.string.default_server));
        if (!url.endsWith("/")) {
            url = url.concat("/");
        }

        Global.server_url = url.concat(getString(R.string.server_path1));
    }

    /**
     * Handles responses from the data collection layer and updates the tab contents
     */
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!Global.cancel) {
                dismissDialog(PROGRESS_DIALOG);
            }
            Global.cancel = false;
            Log.e(TAG, "canceled"); // why are we logging an error here?
            updateTabs();
            locate.cancel();
        }
    };

    /**
     * Called when the device orientation changes to save tab data
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.tabList;
    }

    /**
     * Creates progress dialog
     * 
     * @param id
     *            Identifier of the dialog to create.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                ProgressDialog progressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Global.cancel = true;
                    }
                };
                progressDialog.setTitle(getString(R.string.downloading_data));
                progressDialog.setMessage(getString(R.string.loading));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return progressDialog;
        }
        return null;
    }

    /**
     * Creates the options menu
     * 
     * @param menu
     *            The menu to display.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, REFRESH_ID, 0, "Refresh").setIcon(R.drawable.refresh);
        menu.add(0, ABOUT_ID, 0, "About").setIcon(R.drawable.about);
        menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(R.drawable.settings);
        menu.add(0, EXIT_ID, 0, "Exit").setIcon(R.drawable.exit);

        return result;
    }

    /**
     * Handles menu item selection.
     * 
     * @param item
     *            The selected item.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case REFRESH_ID:
                Global.refresh = true;
                refreshTabData();
                return true;
            case ABOUT_ID:
                startActivity(new Intent(getApplicationContext(), About.class));
                return true;
            case SETTINGS_ID:
                startActivity(new Intent(getApplicationContext(), Settings.class));
                return true;
            case EXIT_ID:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}