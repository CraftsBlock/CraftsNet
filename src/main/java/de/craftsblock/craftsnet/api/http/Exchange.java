package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.api.utils.SessionStorage;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the exchange of data between the client (request) and the server (response).
 * This record holds both the request and response objects for handling HTTP communication.
 * <p>
 * The Exchange object encapsulates the incoming request data (headers, parameters, cookies, body) as a {@link Request} object,
 * and the outgoing response data (headers, status code, body) as a {@link Response} object.
 *
 * @param path     The path the client connected to.
 * @param request  The {@link Request} object containing the incoming data from the client.
 * @param response The {@link Response} object used to send data back to the client.
 * @param storage  A storage for this request designed for storing request specific data.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @see Request
 * @see Response
 * @since 1.0.0-SNAPSHOT
 */
public record Exchange(@NotNull String path, @NotNull Request request, @NotNull Response response, @NotNull SessionStorage storage) {

    public Exchange {
        request.setExchange(this);
        response.setExchange(this);
    }

}
