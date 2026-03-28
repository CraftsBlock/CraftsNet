package de.craftsblock.craftsnet.api.http.body;

/**
 * This interface defines constants for commonly used Content-Type headers in HTTP requests and responses.
 * These constants can be used to specify the type of content being received in an HTTP request.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.4-SNAPSHOT
 */
public interface ContentType {

    String APPLICATION_JSON = "application/json";
    String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    String MULTIPART_FORM_DATA = "multipart/form-data";

}
