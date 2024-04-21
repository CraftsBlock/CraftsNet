package de.craftsblock.craftsnet.events.shares;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an event that is triggered when a share request is made.
 * This event can be canceled to prevent the share request from being processed.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.2
 * @see de.craftsblock.craftscore.event.Event
 * @see de.craftsblock.craftscore.event.Cancellable
 * @since 2.3.2
 */
public class ShareRequestEvent extends Event implements Cancellable {

    private final Headers headers = new Headers();
    private final String httpPath;
    private final Exchange exchange;
    private final RouteRegistry.ShareMapping mapping;

    private String filePath;

    private String cancelReason;
    private boolean cancelled;

    /**
     * Creates a new ShareRequestEvent with the specified path.
     *
     * @param httpPath The url used to access the share
     * @param filePath The relativ path on the file system
     * @param exchange The exchange used by the share to handle its connection
     * @param mapping  The share mapping of the request
     */
    public ShareRequestEvent(@NotNull String httpPath, @NotNull String filePath, @NotNull Exchange exchange, @NotNull RouteRegistry.ShareMapping mapping) {
        this.httpPath = httpPath;
        this.filePath = filePath;
        this.exchange = exchange;
        this.mapping = mapping;
    }

    /**
     * Gets the exchange which holds all http information about the connection with the shared location.
     *
     * @return The exchange object storing important information
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Gets the share mapping, which includes some basic information about the share.
     *
     * @return The mapping of the share
     */
    public RouteRegistry.ShareMapping getMapping() {
        return mapping;
    }

    /**
     * Gets the relativ path on the file system associated with the share request.
     *
     * @return The relativ path on file system the  for the share request.
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the relativ path on the file system associated with the share request.
     *
     * @param path The new path for the share request.
     */
    public void setFilePath(@NotNull String path) {
        this.filePath = path;
    }

    /**
     * Gets the url that is used to access the share
     *
     * @return The url which is used to access the share
     */
    @NotNull
    public String getHttpPath() {
        return httpPath;
    }

    /**
     * Gets the headers for this response.
     *
     * @return Returns the header object
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Adds a value for the specified header to the response.
     *
     * @param key   The "name" of the header
     * @param value The value of the header
     */
    public void addHeader(String key, String value) {
        headers.add(key, value);
    }

    /**
     * Set the header with the specified key and value to this Request.
     *
     * @param key   The "name" of the header
     * @param value The value which should override the value of the header if it exists already, otherwise just adds the value to the header.
     */
    public void setHeader(String key, String value) {
        headers.set(key, value);
    }

    /**
     * Get all the values from the response headers for the specified header name.
     *
     * @param key The "name" of the header used to find the header
     * @return A list of alle the values
     */
    public List<String> getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Checks if a response header was set
     *
     * @param key The "name" of the header which should be checked if it is present
     * @return true if the header is set, false otherwise
     */
    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    /**
     * Sets whether the share request is canceled or not.
     *
     * @param cancelled True if the share request is to be canceled, otherwise false.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Checks if the share request is canceled.
     *
     * @return True if the share request is canceled, otherwise false.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets a custom cancel reason which is printed to the console
     *
     * @param cancelReason The cancel reason which is printed to the console
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Gets the custom cancel reason which was set by one of the listeners.
     *
     * @return The cancel reason
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Checks and returns whether a custom cancel reason was set by one of the listeners.
     *
     * @return true if a custom cancel reason was set, false otherwise.
     */
    public boolean hasCancelReason() {
        return this.cancelReason != null;
    }

}
