package applab.pulse.client;

import applab.client.*;

import java.util.*;

import javax.xml.parsers.*;

import org.apache.http.client.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Handler;
import android.text.Html;

public class PulseDataCollector {
    // constants used for signaling our handler
    public final static int NO_UPDATES_DETECTED = 0;
    public final static int UPDATES_DETECTED = 1;

    private final String TAG = "PulseDataCollector";
    private List<TabInfo> tabs;
    private Handler handler;
    private Timer timer;
    private final int HOUR = 3600000; // TODO: move to a static class
    private String serverUrlOverride;
    private String imei;
    private boolean isRefreshing;

    // TODO: should we factor out the HTTP code here into a separate class?
    private HttpClient httpClient;
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
        this(handler, null, Handset.getImei());
    }

    /**
     * Overload that takes a server URL and imei for test purposes
     */
    public PulseDataCollector(Handler handler, String serverUrlOverride, String imei) {
        if (imei == null) {
            throw new IllegalArgumentException("imei cannot be null");
        }

        this.handler = handler;
        this.serverUrlOverride = serverUrlOverride;
        this.imei = imei;
        this.tabs = new ArrayList<TabInfo>();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new RefreshTask(this), this.HOUR, this.HOUR);

        // setup our HTTP client
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(httpParameters, Global.TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(httpParameters, Global.TIMEOUT);
        this.httpClient = new DefaultHttpClient(httpParameters);

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
        HttpPost httpPost = new HttpPost(baseServerUrl + "/pulse/getTabs");
        httpPost.addHeader("Content-Type", "text/xml");
        httpPost.addHeader("x-Imei", imei);

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

        try {
            httpPost.setEntity(postBody.getEntity());
            BasicHttpResponse httpResponse = (BasicHttpResponse)httpClient.execute(httpPost);
            this.xmlParser.reset();
            GetTabsResponseHandler handler = new GetTabsResponseHandler(this.tabs);
            this.xmlParser.parse(httpResponse.getEntity().getContent(), handler);
            if (handler.getHasUpdatedTabs()) {
                this.tabs = handler.getUpdatedTabs();
                downloadResponse = UPDATES_DETECTED;
            }
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