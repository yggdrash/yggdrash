package io.yggdrash.core;

public class Branch {
    private String name;
    private String owner;
    private String symbol;
    private String property;
    private String type;
    private String timestamp;
    private float tag;
    private String version;
    private String versionHistory;
    private String referenceAddress;
    private String reserveAddress;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setTag(float tag) {
        this.tag = tag;
    }

    public void setVersion_history(String versionHistory) {
        this.versionHistory = versionHistory;
    }
}
