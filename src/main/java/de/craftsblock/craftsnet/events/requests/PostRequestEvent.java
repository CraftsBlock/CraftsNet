package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftscore.annotations.Experimental;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.http.Exchange;

/**
 * This event is triggered after an HTTP request has been processed.
 * It contains information about the request, whether the requested resource was found,
 * and whether the resource was shared.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.1.0-SNAPSHOT
 */
@Experimental
public class PostRequestEvent extends Event {

    private final Exchange exchange;
    private final boolean found, shared;

    /**
     * Constructs a new {@code PostRequestEvent}.
     *
     * @param exchange The {@link Exchange} object containing the details of the HTTP request and response.
     * @param found    A boolean flag indicating whether the requested resource was found.
     * @param shared   A boolean flag indicating whether the resource was shared successfully.
     */
    public PostRequestEvent(Exchange exchange, boolean found, boolean shared) {
        this.exchange = exchange;
        this.found = found;
        this.shared = shared;
    }

    /**
     * Gets the {@link Exchange} object associated with the request event.
     *
     * @return The {@link Exchange} object representing the request and response.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Checks whether the requested resource was found.
     *
     * @return {@code true} if the resource was found, {@code false} otherwise.
     */
    public boolean wasFound() {
        return found;
    }

    /**
     * Checks whether the requested resource was shared successfully.
     * This method returns {@code true} only if the resource was found and shared.
     *
     * @return {@code true} if the resource was found and shared, {@code false} otherwise.
     */
    public boolean wasShared() {
        return found && shared;
    }

}
