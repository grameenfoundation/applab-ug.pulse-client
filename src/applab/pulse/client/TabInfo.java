package applab.pulse.client;

public class TabInfo {

	public String tag;
	public int indicator;
	public int contentID;
	public String storedTabData; 
	public boolean supportsRefresh;

	public TabInfo(String tag, int indicator, int contentID) {
		new TabInfo(tag, indicator, contentID, true);
	}
	
	public TabInfo(String tag, int indicator, int contentID, boolean supportsRefresh) {
		this.tag = tag;
		this.indicator = indicator;
		this.contentID = contentID;
		this.storedTabData = null;
		this.supportsRefresh = supportsRefresh;
	}
	
}
