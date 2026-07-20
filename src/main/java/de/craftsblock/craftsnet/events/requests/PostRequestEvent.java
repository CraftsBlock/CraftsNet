package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered after an HTTP request has been processed.
 * It contains information about the request, whether the requested resource was found,
 * and whether the resource was shared.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see GenericRequestEventBase
 * @since 3.1.0-SNAPSHOT
 */
public class PostRequestEvent extends Event implements GenericRequestEventBase {

    private final HttpExchange httpExchange;
    private final boolean found, shared;

    /**
     * Constructs a new {@code PostRequestEvent}.
     *
     * @param httpExchange The {@link HttpExchange} object containing the details of the HTTP request and response.
     * @param found    A boolean flag indicating whether the requested resource was found.
     * @param shared   A boolean flag indicating whether the resource was shared successfully.
     */
    public PostRequestEvent(HttpExchange httpExchange, boolean found, boolean shared) {
        this.httpExchange = httpExchange;
        this.found = found;
        this.shared = shared;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isAsyncAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull HttpExchange getExchange() {
        return httpExchange;
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
