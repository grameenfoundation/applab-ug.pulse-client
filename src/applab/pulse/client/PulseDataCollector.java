package applab.pulse.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.util.Log;
import applab.client.Handset;

public class PulseDataCollector {
    private final String TAG = "PulseDataCollector";
    private List<TabInfo> tabs;
    private Handler handler;
    private Timer timer;
    private final int HOUR = 3600000; // TODO: move to a static class
    private String baseServerUrl;
    private boolean isRefreshing;

    /**
     * Class constructor
     * 
     * Populates the tab list (which is hard-coded for now) and creates a Timer that will refresh data in the background
     * once per hour.
     * 
     * @param handler
     *            Handler passed by the UI that should be notified when there is new content to display.
     */
    public PulseDataCollector(Handler handler) {
        this(handler, Global.server_url, Handset.getImei());
    }

    /**
     * Overload that takes a server URL and imei for test purposes
     */
    public PulseDataCollector(Handler handler, String serverUrl, String imei) {
        if (serverUrl == null) {
            throw new IllegalArgumentException("serverUrl cannot be null");
        }

        if (imei == null) {
            throw new IllegalArgumentException("imei cannot be null");
        }

        this.handler = handler;
        this.tabs = new LinkedList<TabInfo>();
        this.tabs.add(new TabInfo("messages", R.string.tab1, R.id.webkit1, true));
        this.tabs.add(new TabInfo("performance", R.string.tab2, R.id.webkit2, true));
        this.tabs.add(new TabInfo("support", R.string.tab3, 0, false));
        this.tabs.add(new TabInfo("profile", R.string.tab4, R.id.webkit3, true));

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new RefreshTask(this), this.HOUR, this.HOUR);
        this.baseServerUrl = serverUrl + "?handset_id=" + imei + "&request=";
        Log.i(TAG, "Server Base Url: " + this.baseServerUrl);
    }

    /**
     * Gets the list of tabs available to display for this user.
     * 
     * For now, the tab list is hard-coded, but we have a work item to fetch the list from the server (e.g. XML).
     * 
     * @return The list of tabs to display.
     */
    public List<TabInfo> getTabList() {
        return this.tabs;
    }

    /**
     * Fetch new data from the server to populate each tab in the tab list.
     * 
     * In the future, this method will post a hash of its stored data to the server and the server will only only send
     * down the data if it has changed.
     */
    public void backgroundRefresh() {
        // first check (under a lock), if a refresh is in progress
        synchronized (this.timer) {
            if (this.isRefreshing) {
                return;
            }
            this.isRefreshing = true;
        }
        Thread refreshThread = new Thread() {
            public void run() {
                HtmlLoader loader = new HtmlLoader();

                for (TabInfo tabInfo : tabs) {
                    if (tabInfo.supportsRefresh) {
                        try {
                            URI uri = new URI(baseServerUrl + tabInfo.tag);
                            Log.i(TAG, "Tab: " + tabInfo.tag + ", Url:" + uri.toString());
                            String newTabData = loader.fetchContent(uri);
                            if (newTabData != null) {
                                // If the data update succeeds, use the new data.
                                // Otherwise don't overwrite the cached data.
                                tabInfo.storedTabData = newTabData;
                            }
                        }
                        catch (URISyntaxException e) {
                            // catching this exception so the Java compiler won't complain
                        }
                    }
                }

                signalRefreshComplete();
            }
        };

        refreshThread.start();
    }
    
    // called when our refresh has completed
    private void signalRefreshComplete() {
        synchronized (this.timer) {
            this.isRefreshing = false;
        }

        // signal the UI that we have new data
        this.handler.sendEmptyMessage(0);
    }

    private class RefreshTask extends TimerTask {
        private PulseDataCollector dataCollector;

        public RefreshTask(PulseDataCollector dataCollector) {
            this.dataCollector = dataCollector;
        }

        public void run() {
            this.dataCollector.backgroundRefresh();
        }
    }
}