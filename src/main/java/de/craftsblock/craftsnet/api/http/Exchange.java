package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.utils.Scheme;
import org.apache.http.HttpVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the exchange of data between the client (request) and the server (response).
 * This record holds both the request and response objects for handling HTTP communication.
 * <p>
 * The Exchange object encapsulates the incoming request data (headers, parameters, cookies, body) as a {@link Request} object,
 * and the outgoing response data (headers, status code, body) as a {@link Response} object.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.2.1
 * @see BaseExchange
 * @see Request
 * @see Response
 * @see Session
 * @since 1.0.0-SNAPSHOT
 */
public record Exchange(@NotNull Scheme scheme, @NotNull String path, @NotNull Request request, @NotNull Response response,
                       @NotNull Session session) implements BaseExchange {

    /**
     * @param path     The path the client connected to.
     * @param request  The {@link Request} object containing the incoming data from the client.
     * @param response The {@link Response} object used to send data back to the client.
     * @param session  The {@link Session} object used to store session related things.
     */
    public Exchange {
        request.setExchange(this);
        response.setExchange(this);
    }

    /**
     * Gets the {@link Request} object associated with this exchange holding
     * information about the request.
     *
     * @return The {@link Request} object.
     */
    @Override
    public Request request() {
        return request;
    }

    /**
     * Gets the {@link Response} object associated with this exchange used
     * for managing the response to the request.
     *
     * @return The {@link Response} object.
     */
    @Override
    public Response response() {
        return response;
    }

    /**
     * <p>Get the {@link Session} object associated with this exchange holding
     * information from the session.</p>
     *
     * <p>
     * Can be made persistent by using {@link Session#startSession()}. Must
     * be called prior to the first call of {@link Response#print(Object)}.
     * </p>
     *
     * <p>If the session is not persistent the data stored will be discarded
     * after the request was handled or the websocket was closed.</p>
     *
     * @return The session object.
     */
    @Override
    public Session session() {
        return session;
    }

    /**
     * Performs last actions before the exchange is closed.
     */
    @Override
    public void close() throws Exception {
    }

    /**
     * A wrapper method for {@link Request#getRawUrl()} retrieved from the {@link Exchange#request()}.
     * <p>This method is used for backwards compatibility with older versions of CraftsNet.</p>
     *
     * @return The raw url.
     * @since 3.3.2-SNAPSHOT
     * @deprecated in favor of {@link Request#getRawUrl()}.
     */
    @Deprecated(since = "3.3.2-SNAPSHOT", forRemoval = true)
    public String path() {
        return request().getRawUrl();
    }

}
