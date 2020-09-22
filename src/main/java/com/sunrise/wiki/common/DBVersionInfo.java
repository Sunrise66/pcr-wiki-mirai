package com.sunrise.wiki.common;

public class DBVersionInfo {
    private long TruthVersion;
    private String hash;

    public long getTruthVersion() {
        return TruthVersion;
    }

    public void setTruthVersion(long truthVersion) {
        TruthVersion = truthVersion;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
