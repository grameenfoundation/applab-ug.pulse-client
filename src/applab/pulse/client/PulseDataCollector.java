package applab.pulse.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Handler;
import applab.client.HttpHelpers;
import applab.client.XmlEntityBuilder;

public class PulseDataCollector {
    // constants used for signaling our handler
    public final static int NO_UPDATES_DETECTED = 0;
    public final static int UPDATES_DETECTED = 1;
    public final static int NO_SERVER_CONNECTION = 2;

    private final String TAG = "PulseDataCollector";
    private List<TabInfo> tabs;
    private Handler handler;
    private Timer timer;
    private final int HOUR = 3600000; // TODO: move to a static class
    private String serverUrlOverride;
    private boolean isRefreshing;

    private SAXParser xmlParser;

    private final static String NAMESPACE = "http://schemas.applab.org/2010/08/pulse";
    private final static String TAB_ELEMENT_NAME = "Tab";
    private final static String NAME_ATTRIBUTE = "name";
    private final static String HASH_ATTRIBUTE = "hash";

    /**
     * Class constructor
     * 
     * Creates a Timer that will refresh the tab list and contents in the background (once per hour).
     * 
     * @param handler
     *            Handler passed by the UI that should be notified when there is new content to display.
     */
    public PulseDataCollector(Handler handler) {
        this(handler, null);
    }

    /**
     * Overload that takes a server URL and imei for test purposes
     */
    public PulseDataCollector(Handler handler, String serverUrlOverride) {
        this.handler = handler;
        this.serverUrlOverride = serverUrlOverride;
        this.tabs = new ArrayList<TabInfo>();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new RefreshTask(this), this.HOUR, this.HOUR);

        try {
            this.xmlParser = SAXParserFactory.newInstance().newSAXParser();
        }
        catch (ParserConfigurationException e) {
            // Needed to avoid java warnings, newSAXParser will never throw on Android
        }
        catch (SAXException e) {
            // Needed to avoid java warnings, newSAXParser will never throw on Android
        }
        catch (FactoryConfigurationError e) {
            // Needed to avoid java warnings, newSAXParser will never throw on Android
        }
    }

    /**
     * Gets the list of tabs to display for this user.
     * 
     * @return The list of tabs to display.
     */
    public List<TabInfo> getTabList() {
        return this.tabs;
    }

    /**
     * Create a POST request with our existing list of tabs and their hashes, and retrieve updated values. See Pulse
     * spec for the details of the GetTabs protocol.
     * 
     * @return The handler signal value (i.e. whether we have updated tabs or not)
     */
    private int downloadTabUpdates() {
        int downloadResponse = NO_UPDATES_DETECTED;

        String baseServerUrl = this.serverUrlOverride;
        if (baseServerUrl == null) {
            baseServerUrl = Settings.getServerUrl();
        }
        if (baseServerUrl.endsWith("/")) {
            baseServerUrl = baseServerUrl.substring(0, baseServerUrl.length() - 1);
        }

        XmlEntityBuilder postBody = new XmlEntityBuilder();
        postBody.writeStartElement("GetTabsRequest", NAMESPACE);
        for (TabInfo currentTab : this.tabs) {
            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(NAME_ATTRIBUTE, currentTab.getName());
            attributes.put(HASH_ATTRIBUTE, currentTab.getContentHash());
            postBody.writeStartElement(TAB_ELEMENT_NAME, attributes);
            postBody.writeEndElement();
        }
        postBody.writeEndElement();
        GetTabsResponseHandler handler = new GetTabsResponseHandler(this.tabs);
        
        try {

            // This line was causing problems on android 2.2 (IDEOS)
            // this.xmlParser.reset();         
        	
        	// Salesforce doesn't currently support raw http post data, except via it's rest service api, but the rest service api doesn't provide the ability to set the language
        	// So we'll send a name value pair instead
        	/*InputStream response = HttpHelpers.postXmlRequestAndGetStream(baseServerUrl + "/pulse/getTabs",
                    (StringEntity)postBody.getEntity());*/
        	
        	
        	//InputStream response = HttpHelpers.postData("data=" + postBody.toString(), new URL(baseServerUrl + "/pulse/getTabs")); 
        	
        	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        	nameValuePairs.add(new BasicNameValuePair("data", postBody.toString()));
        	
        	InputStream response = HttpHelpers.postFormRequestAndGetStream(baseServerUrl + "/getTabs",
        			new UrlEncodedFormEntity(nameValuePairs));
            this.xmlParser.parse(response, handler);
            if (handler.getHasUpdatedTabs()) {
                this.tabs = handler.getUpdatedTabs();
                downloadResponse = UPDATES_DETECTED;
            }
        }

        catch (java.net.SocketException exception) {
            downloadResponse = NO_SERVER_CONNECTION;
        }

        catch (java.net.UnknownHostException exception) {
            downloadResponse = NO_SERVER_CONNECTION;
        }

        catch (Exception e) {
            // if we fail in the download process, log and notify that nothing has been updated
            android.util.Log.e(TAG, "Exception:" + e);
        }

        synchronized (this.timer) {
            this.isRefreshing = false;
        }

        return downloadResponse;
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
                int handlerSignal = downloadTabUpdates();
                signalRefreshComplete(handlerSignal);
            }
        };

        refreshThread.start();
    }

    // called when our refresh has completed
    private void signalRefreshComplete(int handlerSignal) {
        synchronized (this.timer) {
            this.isRefreshing = false;
        }
        // signal the UI that we have new data
        this.handler.sendEmptyMessage(handlerSignal);
    }

    /**
     * SAX parser handler that processes the GetTabsResponse message
     * 
     */
    private class GetTabsResponseHandler extends DefaultHandler {
        private boolean hasUpdatedTabs;
        private static final String HAS_CHANGED_ATTRIBUTE = "hasChanged";
        private static final String RESPONSE_ELEMENT = "GetTabsResponse";

        // current set of tabs, indexed by name
        private HashMap<String, TabInfo> currentTabMapping;
        private ArrayList<TabInfo> updatedTabs;
        private TabInfo newTab;

        public GetTabsResponseHandler(List<TabInfo> currentTabs) {
            // assume tabs are updating unless told otherwise
            this.hasUpdatedTabs = true;
            this.updatedTabs = new ArrayList<TabInfo>();
            if (currentTabs != null) {
                this.currentTabMapping = new HashMap<String, TabInfo>();
                for (TabInfo currentTab : currentTabs) {
                    this.currentTabMapping.put(currentTab.getName(), currentTab);
                }
            }
        }

        public boolean getHasUpdatedTabs() {
            return this.hasUpdatedTabs;
        }

        public List<TabInfo> getUpdatedTabs() {
            return this.updatedTabs;
        }

        // <?xml version="1.0"?>
        // <GetTabsResponse xmlns="http://schemas.applab.org/2010/08/pulse" hasChanged="true">
        // <Tab name="Messages" hash="updated_hash">updated tab content</Tab>
        // <Tab name="Performance" hasChanged="false" />
        // ...
        // </GetTabsResponse>
        @Override
        public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
            if (NAMESPACE.equals(namespaceUri)) {
                if (RESPONSE_ELEMENT.equals(localName)) {
                    // see if hasChanged="false"
                    String hasChanged = attributes.getValue(HAS_CHANGED_ATTRIBUTE);
                    if (hasChanged != null) {
                        this.hasUpdatedTabs = Boolean.parseBoolean(hasChanged);
                    }
                }
                else if (TAB_ELEMENT_NAME.equals(localName)) {
                    String name = attributes.getValue(NAME_ATTRIBUTE);
                    String contentHash = attributes.getValue(HASH_ATTRIBUTE);
                    String hasChangedValue = attributes.getValue(HAS_CHANGED_ATTRIBUTE);
                    boolean hasTabChanged = true;
                    if (hasChangedValue != null) {
                        hasTabChanged = Boolean.parseBoolean(hasChangedValue);
                    }
                    if (!hasTabChanged) {
                        this.newTab = this.currentTabMapping.get(name);
                    }
                    else {
                        this.newTab = new TabInfo(name, contentHash);
                    }
                }
            }
        }

        @Override
        public void endElement(String namespaceUri, String localName, String qName) throws SAXException {
            if (NAMESPACE.equals(namespaceUri)) {
                if (TAB_ELEMENT_NAME.equals(localName)) {
                    if (this.newTab != null) {
                        this.updatedTabs.add(this.newTab);
                        this.newTab = null;
                    }
                }
            }
        }

        @Override
        public void characters(char[] data, int start, int length) throws SAXException {
            if (this.newTab != null) {
                this.newTab.appendContent(String.copyValueOf(data, start, length));
            }
        }
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