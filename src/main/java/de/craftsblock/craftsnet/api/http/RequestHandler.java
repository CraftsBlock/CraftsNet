package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.api.Handler;

/**
 * The RequestHandler interface serves as a marker interface for classes that are intended to handle incoming HTTP requests.
 * Implementing this interface indicates that a class has the capability to handle HTTP requests, but it doesn't define any
 * specific methods or behavior for handling the requests.
 * <p>
 * Marker interfaces, like RequestHandler, are used to categorize or tag classes that share a common characteristic or role.
 * Classes that implement RequestHandler can be identified as request handlers without enforcing any specific contract or
 * implementation.
 * <p>
 * Developers can implement this interface in classes that are designed to handle incoming requests and use it to easily
 * identify request handling components within a system or framework.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since CraftsNet-1.0.0
 */
public interface RequestHandler extends Handler {
    // No methods are declared in this interface since it serves as a marker interface.
    // Classes implementing this interface are expected to provide their own request handling logic.
}
