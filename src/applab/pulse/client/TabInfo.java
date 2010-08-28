package applab.pulse.client;

public class TabInfo {
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
}
