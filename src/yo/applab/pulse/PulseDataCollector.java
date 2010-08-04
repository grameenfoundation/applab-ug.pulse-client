package yo.applab.pulse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.util.Log;

public class PulseDataCollector {
	private final String TAG = "PulseDataCollector";
	private List<TabInfo> tabs;
	private Handler handler;
	private Timer timer;
	private final int HOUR = 3600000; // TODO: move to a static class

	/**
	 * Class constructor 
	 * 
	 * Populates the tab list (which is hard-coded for now) and creates a Timer that
	 * will refresh data in the background once per hour.
	 * 
	 * @param handler Handler passed by the UI that should be notified when there is 
	 * new content to display.
	 */
	public PulseDataCollector(Handler handler) {
		this.handler = handler;
		this.tabs = new LinkedList<TabInfo>();
		this.tabs.add(new TabInfo("messages", R.string.tab1, R.id.webkit1, true));
		this.tabs.add(new TabInfo("performance", R.string.tab2, R.id.webkit2, true));
		this.tabs.add(new TabInfo("support", R.string.tab3, 0, false));
		this.tabs.add(new TabInfo("profile", R.string.tab4, R.id.webkit3, true));

		this.timer = new Timer();
		this.timer.scheduleAtFixedRate(new RefreshTask(this), this.HOUR, this.HOUR);
	}

	/**
	 * Gets the list of tabs available to display for this user. 
	 * 
	 * For now, the tab list is hard-coded, but we have a work item to fetch the 
	 * list from the server (e.g. XML).
	 * 
	 * @return The list of tabs to display.
	 */
	public List<TabInfo> getTabList() {
		return this.tabs;
	}

	/**
	 * Fetch new data from the server to populate each tab in the tab list. 
	 * 
	 * In the future, this method will post a hash of its stored data to the server
	 * and the server will only only send down the data if it has changed.
	 */
	public void backgroundRefresh() {
		Thread refreshThread = new Thread() {
			public void run() {
				String base = Global.server_url + "?handset_id=" + Global.getImei(null) + "&request=";
				Log.i(TAG, "Server Base Url: " + base);
				HtmlLoader loader = new HtmlLoader();

				for (int i = 0; i < tabs.size(); i++) {
					TabInfo tabInfo = tabs.get(i);

					if (tabInfo.supportsRefresh) { 
						try {
							URI uri = new URI(base + tabInfo.tag);
							Log.i(TAG, "Tab: " + tabInfo.tag + ", Url:" + uri.toString());
							String newTabData = loader.fetchContent(uri);
							if (newTabData != null) {
								// If the data update succeeds, use the new data.
								// Otherwise don't overwrite the cached data.
								tabInfo.storedTabData = newTabData;
							}
						} catch (URISyntaxException e) {
							// catching this exception so the Java compiler won't complain
						}
					}
				}
				
				// signal the UI that we have new data
				handler.sendEmptyMessage(0);
			}
		};
		
		refreshThread.start();
	}

	class RefreshTask extends TimerTask {
		
		private PulseDataCollector dataCollector;
		
		public RefreshTask(PulseDataCollector dataCollector) {
			this.dataCollector = dataCollector;
		}
		
		public void run() {
			this.dataCollector.backgroundRefresh();
		}
	}

}