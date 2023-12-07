package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.CraftsNet;

public class DefaultPages {
    protected static String notfound(String domain, int port) {
        return "<!DOCTYPE>\n" +
                "<html><head>\n" +
                "<title>404 Share Not Found</title>\n" +
                "</head><body>\n" +
                "<h1>Share Not Found</h1>\n" +
                "<p>The requested share was not found on this instance of CraftsNet.</p>\n" +
                "<hr>\n" +
                "<address>CraftsNet/" + CraftsNet.version + " (" + System.getProperty("os.name") + ") Server at " + domain + " Port " + port + "</address>\n" +
                "</body></html>";
    }

    protected static String notallowed(String domain, int port) {
        return "<!DOCTYPE>\n" +
                "<html><head>\n" +
                "<title>403 Forbidden</title>\n" +
                "</head><body>\n" +
                "<h1>Forbidden</h1>\n" +
                "<p>You don't have permission to access this resource.</p>\n" +
                "<hr>\n" +
                "<address>CraftsNet/" + CraftsNet.version + " (" + System.getProperty("os.name") + ") Server at " + domain + " Port " + port + "</address>\n" +
                "</body></html>";
    }

}
