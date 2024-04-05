package de.craftsblock.craftsnet.events.shares;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an event that is triggered when a share request is made.
 * This event can be canceled to prevent the share request from being processed.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see de.craftsblock.craftscore.event.Event
 * @see de.craftsblock.craftscore.event.Cancelable
 * @since 2.3.2
 */
public class ShareRequestEvent extends Event implements Cancelable {

    private final Headers headers = new Headers();
    private final String httpPath;
    private String filePath;
    private boolean cancelled;

    /**
     * Creates a new ShareRequestEvent with the specified path.
     *
     * @param httpPath The url used to access the share
     * @param filePath The relativ path on the file system
     */
    public ShareRequestEvent(@NotNull String httpPath, @NotNull String filePath) {
        this.httpPath = httpPath;
        this.filePath = filePath;
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
     */
    public void addHeader(String key, String value) {
        headers.add(key, value);
    }

    /**
     * Set the header with the specified key and value to this Request.
     */
    public void setHeader(String key, String value) {
        headers.set(key, value);
    }

    /**
     * Get all the values from the response headers for the specified header name.
     *
     * @return A list of alle the values
     */
    public List<String> getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Checks if a response header was set
     *
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

}
