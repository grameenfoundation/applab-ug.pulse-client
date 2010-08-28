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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import applab.client.*;

/**
 * Activity that is displayed at startup time. This activity is responsible for dynamically determining the tabs to
 * display and their contents.
 * 
 */
public class MainWindow extends ApplabTabActivity {
    private static final String errorHtml = "<html><body>" + "<h1>Unable to establish a connection</h1>"
            + "<p><strong>Please try again later.</strong></p>" + "</body></html>";

    private final String TAG = "PulseTabs";
    private Locate locate;
    private static final int ABOUT_ID = Menu.FIRST;
    private static final int REFRESH_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int EXIT_ID = Menu.FIRST + 3;
    private PulseDataCollector dataCollector;
    private List<TabInfo> tabList;
    private ProgressDialog progressDialog;

    // set to true if we need to update our displayed tabs in displayTabs
    private boolean updateDisplayedTabs;

    public MainWindow() {
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

        this.dataCollector = new PulseDataCollector(new Handler() {
            @Override
            public void handleMessage(Message message) {
                // if we are showing a progress dialog, we can remove it
                if (progressDialog != null) {
                    progressDialog.cancel();
                    progressDialog = null;
                }

                // update our tab list if we've successfully updated the contents
                if (message.what == PulseDataCollector.UPDATES_DETECTED) {
                    tabList = dataCollector.getTabList();
                    updateDisplayedTabs = true;
                }

                if (updateDisplayedTabs) {
                    displayTabs();
                }
                updateDisplayedTabs = false;
                locate.cancel();
            }
        });
        TabHost tabHost = this.getTabHost();
        LayoutInflater.from(this).inflate(R.layout.tabs, tabHost.getTabContentView(), true);

        // create Locate object
        this.locate = new Locate((LocationManager)getSystemService(Context.LOCATION_SERVICE));

        // Get saved tab data in case there was a configuration (orientation)
        // change so we don't reload it from the network
        this.tabList = (List<TabInfo>)getLastNonConfigurationInstance();

        if (this.tabList == null) {
            refreshTabData();
            this.updateDisplayedTabs = true;

            // workaround another android bug of the touch screen interacting with tab host before there are tabs
            tabHost.setCurrentTab(0);
            // displayTabs will get called from our asynchronous handler, and we always want to update the first time
        }
        else {
            displayTabs();
        }
    }

    /**
     * Takes our tab configuration and displays it in the main window
     */
    private void displayTabs() {
        assert (this.tabList != null) : "we should always have a tab list populated by the time we reach displayTabs";

        TabHost tabHost = getTabHost();

        // TODO: we can be smarter about seeing if we have new tabs or just new content
        int currentTab = tabHost.getCurrentTab();
        
        // Android has a bug where it will Null-ref if you don't call setCurrentTab(0) before clearAllTabs
        // http://code.google.com/p/android/issues/detail?id=2772
        tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();
        if (this.tabList != null) {
            for (TabInfo tab : this.tabList) {
                addBrowserTab(tabHost, tab.getName(), tab.getContent());
            }
            
            // now reset the active tab
            if (this.tabList.size() > currentTab) {
                tabHost.setCurrentTab(currentTab);
            }
        }
        else {
            addBrowserTab(tabHost, "Error", MainWindow.errorHtml);
        }
    }

    private void addBrowserTab(TabHost tabHost, String tabName, String tabContent) {
        TabSpec tabSpec = tabHost.newTabSpec(tabName);
        tabSpec.setIndicator(tabName);
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_HTML_INTENT, tabContent);
        tabSpec.setContent(intent);
        tabHost.addTab(tabSpec);
    }

    /**
     * Displays a progress dialog and then calls the dataCollector to refresh the data asynchronously
     */
    private void refreshTabData() {
        // Show a progress dialog
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle(getString(R.string.downloading_data));
        this.progressDialog.setMessage(getString(R.string.loading));
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.setCancelable(false);
        DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                progressDialog = null;
            }
        };

        this.progressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
        this.progressDialog.show();
        Log.i(TAG, "updating tab data...");

        // and call our common refresh data (which will bring down the dialog when we are complete)
        this.dataCollector.backgroundRefresh();
    }

    /**
     * Called when the device orientation changes to save tab data
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.tabList;
    }

    /**
     * Creates the options menu
     * 
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