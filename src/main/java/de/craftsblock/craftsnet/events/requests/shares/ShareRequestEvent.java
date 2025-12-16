package de.craftsblock.craftsnet.events.requests.shares;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.events.EventWithCancelReason;
import de.craftsblock.craftsnet.events.requests.GenericRequestEventBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an event that is triggered when a share request is made.
 * This event can be canceled to prevent the share request from being processed.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @see EventWithCancelReason
 * @see GenericRequestEventBase
 * @since 2.3.2-SNAPSHOT
 */
public class ShareRequestEvent extends EventWithCancelReason implements GenericRequestEventBase {

    private final String httpPath;
    private final Exchange exchange;
    private final RouteRegistry.ShareMapping mapping;

    private String filePath;

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
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull Exchange getExchange() {
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
     * @deprecated Use {@link Response#getHeaders()} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public Headers getHeaders() {
        return getResponse().getHeaders();
    }

    /**
     * Adds a value for the specified header to the response.
     *
     * @param key   The "name" of the header
     * @param value The value of the header
     * @deprecated Use {@link Response#addHeader(String, String)} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public void addHeader(String key, String value) {
        getResponse().addHeader(key, value);
    }

    /**
     * Set the header with the specified key and value to this Request.
     *
     * @param key   The "name" of the header
     * @param value The value which should override the value of the header if it exists already, otherwise just adds the value to the header.
     * @deprecated Use {@link Response#setHeader(String, String)} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public void setHeader(String key, String value) {
        getResponse().setHeader(key, value);
    }

    /**
     * Get all the values from the response headers for the specified header name.
     *
     * @param key The "name" of the header used to find the header
     * @return A list of alle the values
     * @deprecated Use {@link Response#getHeaders(String)} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public List<String> getHeader(String key) {
        return getResponse().getHeaders(key);
    }

    /**
     * Checks if a response header was set
     *
     * @param key The "name" of the header which should be checked if it is present
     * @return true if the header is set, false otherwise
     * @deprecated Use {@link Response#hasHeader(String)} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public boolean hasHeader(String key) {
        return getResponse().hasHeader(key);
    }

}
