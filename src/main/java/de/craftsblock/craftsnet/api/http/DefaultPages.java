package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.CraftsNet;

/**
 * Default pages for HTTP error responses.
 * This class provides static methods to generate HTML content for common HTTP error responses such as 404 Not Found and 403 Forbidden.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-2.3.3
 */
class DefaultPages {

    /**
     * Generates HTML content for a 404 Not Found response.
     *
     * @param domain The domain where the request was made.
     * @param port   The port where the request was made.
     * @return HTML content for the 404 Not Found response.
     */
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

    /**
     * Generates HTML content for a 403 Forbidden response.
     *
     * @param domain The domain where the request was made.
     * @param port   The port where the request was made.
     * @return HTML content for the 403 Forbidden response.
     */
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
