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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;
import applab.client.AboutDialog;
import applab.client.ApplabActivity;
import applab.client.ApplabTabActivity;
import applab.client.BrowserActivity;
import applab.client.location.GpsManager;
import applab.client.pulse.R;

/**
 * Activity that is displayed at startup time. This activity is responsible for dynamically determining the tabs to
 * display and their contents.
 * 
 */
public class PulseTabs extends ApplabTabActivity {
    private static final String errorHtml = "<html><body>" + "<h1>Unable to establish a connection</h1>"
            + "<p><strong>Please try again later.</strong></p>" + "</body></html>";

    private static final int ABOUT_ID = Menu.FIRST;
    private static final int REFRESH_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int EXIT_ID = Menu.FIRST + 3;

    private PulseDataCollector dataCollector;
    private List<TabInfo> currentTabs;
    private ProgressDialog progressDialog;

    /**
     * a global, increasing value that is appended to TabInfo tags in order to uniquely identify them over the course of
     * process execution.
     * 
     * This is necessary because Android will sometimes skip a content refresh if a tag name is unchanged.
     */
    // uniquely identify them over the course of process execution
    private static int currentTagVersion;

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

        // Set the app version
        ApplabActivity.setAppVersion(getString(R.string.app_name), getString(R.string.app_version));

        this.dataCollector = new PulseDataCollector(new Handler() {
            @Override
            public void handleMessage(Message message) {
                handleDataCollectorMessage(message);
            }
        });
        TabHost tabHost = this.getTabHost();
        LayoutInflater.from(this).inflate(R.layout.tabs, tabHost.getTabContentView(), true);

        // Get saved tab data in case there was a configuration (orientation)
        // change so we don't reload it from the network
        List<TabInfo> initialTabs = (List<TabInfo>)getLastNonConfigurationInstance();

        if (initialTabs == null) {
            // check our local storage for tab information from the previous run
            initialTabs = TabInfo.load();

            if (initialTabs.size() == 0) {
                // As a workaround for an android bug where the touch screen will crash the tab host if there are no
                // tabs available, we setup a temporary tab.
                TabInfo errorTab = new TabInfo("Error", "");
                errorTab.appendContent(PulseTabs.errorHtml);
                initialTabs.add(errorTab);
            }

            // and kick off a request to the server to make sure our information is up to date
            refreshTabData();
        }

        updateTabs(initialTabs);
    }

    /**
     * UI handler that processes messages sent by PulseDataCollector
     */
    private void handleDataCollectorMessage(Message message) {
        // if we are showing a progress dialog, we can remove it
        if (this.progressDialog != null) {
            this.progressDialog.cancel();
            this.progressDialog = null;
        }

        // update our tab list if we've received new data from the server
        if (message.what == PulseDataCollector.UPDATES_DETECTED) {
            updateTabs(this.dataCollector.getTabList());
        }

        else if (message.what == PulseDataCollector.NO_SERVER_CONNECTION) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.connection_error_message))
                    .setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();

                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * see if our current tab set (number + titles) matches the updated tab set
     */
    private boolean canReuseTabSpecs(List<TabInfo> newTabs) {
        if (this.currentTabs == null) {
            return false;
        }

        if (this.currentTabs.size() != newTabs.size()) {
            return false;
        }

        for (int tabIndex = 0; tabIndex < newTabs.size(); tabIndex++) {
            // All we care about for the purposes of reusing TabSpecs is title equality.
            if (!newTabs.get(tabIndex).getName().equals(this.currentTabs.get(tabIndex).getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Updates our internal tab configuration, and refreshes their display in the main window as necessary
     */
    private void updateTabs(List<TabInfo> newTabs) {
        assert (newTabs != null) : "newTabs must be non-null";

        TabHost tabHost = getTabHost();

        if (canReuseTabSpecs(newTabs)) {
            // we don't need to clear and repopulate the TabHost's list, we can just update the contents
            // unfortunately the only way to enumerate over the tab list is through setCurrent/getCurrent
            int savedTabIndex = tabHost.getCurrentTab();

            for (int tabIndex = 0; tabIndex < newTabs.size(); tabIndex++) {
                tabHost.setCurrentTab(tabIndex);
                BrowserActivity currentTab = (BrowserActivity)this.getCurrentTab();
                currentTab.updateHtmlContent(newTabs.get(tabIndex).getContent());
            }

            // now reset the active tab
            tabHost.setCurrentTab(savedTabIndex);
        }
        else {
            currentTagVersion++;

            // Android has a bug where it will Null-ref if you don't call setCurrentTab(0) before clearAllTabs
            // http://code.google.com/p/android/issues/detail?id=2772
            tabHost.setCurrentTab(0);

            // though there's still a race condition in onFocusChanged, so try a few times as a temporary workaround
            boolean clearTabs = true;
            int retriesRemaining = 5;
            while (clearTabs && retriesRemaining > 0) {
                try {
                    tabHost.clearAllTabs();
                    clearTabs = false;
                }
                catch (NullPointerException e) {
                    retriesRemaining--;
                }
            }

            for (TabInfo tab : newTabs) {
                addBrowserTab(tabHost, tab);
            }
        }

        // finally, cache the set of tabs
        this.currentTabs = newTabs;

        // and save them for a future session
        // TODO: can we optimize this out to onDestroy? Does it matter?
        TabInfo.save(this.currentTabs);
    }

    private void addBrowserTab(TabHost tabHost, TabInfo tab) {
        String tabName = tab.getName();

        // append currentTagVersion to our TabSpec's tag name so that Android will always redraw the contents
        TabSpec tabSpec = tabHost.newTabSpec(tabName + Integer.toString(currentTagVersion));
        tabSpec.setIndicator(tabName);
        Intent intent = new Intent(this, BrowserActivity.class);

        intent.putExtra(BrowserActivity.EXTRA_ENABLE_JAVASCRIPT_INTENT, true);
        intent.putExtra(BrowserActivity.EXTRA_HTML_INTENT, tab.getContent());
        intent.putExtra("enableJavascriptInterface", false);

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

        GpsManager.getInstance().update();

        // and call our common refresh data (which will bring down the dialog when we are complete)
        this.dataCollector.backgroundRefresh();
    }

    /**
     * Called when the device orientation changes to save our current tab data. This allows to to avoid hitting local
     * storage on orientation changes.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.currentTabs;
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
                AboutDialog.show(this, getString(R.string.app_version), getString(R.string.app_name),
                        getString(R.string.release_date), getString(R.string.info), R.drawable.icon);
                return true;
            case SETTINGS_ID:
                startActivity(new Intent(getApplicationContext(), Settings.class));
                return true;
            case EXIT_ID:
                ApplabActivity.exit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

        GpsManager.getInstance().onStart(this);
    }
}