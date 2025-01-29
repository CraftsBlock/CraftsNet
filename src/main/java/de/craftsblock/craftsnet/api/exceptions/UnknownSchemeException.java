package de.craftsblock.craftsnet.api.exceptions;

import de.craftsblock.craftsnet.api.utils.Scheme;

/**
 * Exception thrown when an unknown or unsupported scheme is encountered.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.2-SNAPSHOT
 */
public class UnknownSchemeException extends RuntimeException {

    /**
     * Constructs a new {@link UnknownSchemeException} with a message derived from the provided scheme.
     *
     * @param scheme The {@link Scheme} that is unknown or unsupported.
     */
    public UnknownSchemeException(Scheme scheme) {
        this("Unknown scheme: " + scheme);
    }

    /**
     * Constructs a new {@link UnknownSchemeException} with the specified detail message.
     *
     * @param message The detail message for this exception.
     */
    public UnknownSchemeException(String message) {
        super(message);
    }

}
