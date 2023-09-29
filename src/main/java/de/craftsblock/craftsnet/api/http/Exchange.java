package de.craftsblock.craftsnet.api.http;

/**
 * Represents the exchange of data between the client (request) and the server (response).
 * This record holds both the request and response objects for handling HTTP communication.
 * <p>
 * The Exchange object encapsulates the incoming request data (headers, parameters, cookies, body) as a {@link Request} object,
 * and the outgoing response data (headers, status code, body) as a {@link Response} object.
 *
 * @param request  The {@link Request} object containing the incoming data from the client.
 * @param response The {@link Response} object used to send data back to the client.
 * @author CraftsBlock
 * @version 1.0
 * @see Request
 * @see Response
 * @since 1.0.0
 */
public record Exchange(Request request, Response response) {

}
