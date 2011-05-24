package applab.pulse.client;

import java.util.ArrayList;
import java.util.List;

import applab.client.PropertyStorage;
import applab.client.pulse.R;

public class TabInfo {
    // keys used for property storage
    private static final String KEY_TAB_COUNT = "TabCount";
    private static final String KEY_TAB_NAME_BASE = "TabName";
    private static final String KEY_TAB_HASH_BASE = "TabHash";
    private static final String KEY_TAB_CONTENT_BASE = "TabHash";

    private String content;
    private String contentHash;
    private String name;

    public TabInfo(String name, String contentHash) {
        this.name = name;
        this.contentHash = contentHash;
        this.content = "";
    }

    /**
     * Get the name of the tab (to show as the "indicator")
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the hash of the tab content (used for GetTabs protocol)
     */
    public String getContentHash() {
        return this.contentHash;
    }

    /**
     * return the HTML content associated with this tab
     */
    public String getContent() {
        return this.content;
    }

    public void appendContent(String additionalContent) {
        this.content += additionalContent;
    }

    /**
     * Load tab information from storage.
     */
    public static ArrayList<TabInfo> load() {
        ArrayList<TabInfo> tabList = new ArrayList<TabInfo>();
        PropertyStorage localStorage = PropertyStorage.getLocal();
        String tabCountValue = localStorage.getValue(KEY_TAB_COUNT);
        if (tabCountValue != null) {
            int tabCount = Integer.parseInt(tabCountValue);
            if (tabCount > 0) {
                // We have cached tabs, fetch their data. Each stored tab spans three properties grouped by index.
                for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                    TabInfo cachedTab = new TabInfo(
                            localStorage.getValue(KEY_TAB_NAME_BASE + tabIndex),
                            localStorage.getValue(KEY_TAB_HASH_BASE + tabIndex));
                    cachedTab.appendContent(localStorage.getValue(KEY_TAB_CONTENT_BASE + tabIndex));
                    tabList.add(cachedTab);
                }
            }
        }

        return tabList;
    }

    /**
     * Save this list of tabs so that it can be restored in a later session
     */
    public static void save(List<TabInfo> tabList) {
        PropertyStorage localStorage = PropertyStorage.getLocal();
        localStorage.setValue(KEY_TAB_COUNT, Integer.toString(tabList.size()));

        for (int tabIndex = 0; tabIndex < tabList.size(); tabIndex++) {
            TabInfo tab = tabList.get(tabIndex);
            localStorage.setValue(KEY_TAB_NAME_BASE + tabIndex, tab.getName());
            localStorage.setValue(KEY_TAB_HASH_BASE + tabIndex, tab.getContentHash());
            localStorage.setValue(KEY_TAB_CONTENT_BASE + tabIndex, tab.getContent());
        }
    }
}
