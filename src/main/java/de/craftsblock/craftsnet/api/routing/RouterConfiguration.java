package de.craftsblock.craftsnet.api.routing;

public class RouterConfiguration {

    private boolean cacheEnabled = false;
    private int cacheSize = 256;

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

}
