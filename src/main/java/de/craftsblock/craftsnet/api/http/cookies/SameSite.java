package de.craftsblock.craftsnet.api.http.cookies;

/**
 * Enumeration representing the SameSite attribute of a cookie.
 * <p>
 * The SameSite attribute allows you to declare if your cookie should be restricted to a first-party or same-site context.
 * This attribute helps to protect against Cross-Site Request Forgery (CSRF) attacks.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6
 */
public enum SameSite {

    /**
     * The "None" value allows the cookie to be sent with both cross-site and same-site requests.
     * Cookies with the SameSite=None attribute must also be marked as Secure.
     */
    NONE,

    /**
     * The "Lax" value allows the cookie to be sent with same-site requests and with top-level cross-site navigation.
     * It provides a reasonable balance between security and usability.
     */
    LAX,

    /**
     * The "Strict" value ensures that the cookie is sent only in a first-party context (same-site requests).
     * This provides the highest level of protection against CSRF attacks but can affect the usability of some applications.
     */
    STRICT,

}
