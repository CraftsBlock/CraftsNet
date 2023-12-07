package de.craftsblock.craftsnet.events.shares;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an event that is triggered when a share request is made.
 * This event can be canceled to prevent the share request from being processed.
 *
 * @author CraftsBlock
 * @version 1.0
 * @see de.craftsblock.craftscore.event.Event
 * @see de.craftsblock.craftscore.event.Cancelable
 * @since 2.3.2
 */
public class ShareRequestEvent extends Event implements Cancelable {

    private Headers headers = new Headers();
    private String path;
    private boolean cancelled;

    /**
     * Creates a new ShareRequestEvent with the specified path.
     *
     * @param path The path for the share request.
     */
    public ShareRequestEvent(@NotNull String path) {
        this.path = path;
    }

    /**
     * Gets the path associated with the share request.
     *
     * @return The path for the share request.
     */
    @NotNull
    public String getPath() {
        return path;
    }

    /**
     * Sets the path associated with the share request.
     *
     * @param path The new path for the share request.
     */
    public void setPath(@NotNull String path) {
        this.path = path;
    }

    /**
     * Gets the headers for this Request.
     *
     * @return Returns the header object
     */
    public Headers getHeaders() {
        return headers;
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
