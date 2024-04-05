package de.craftsblock.craftsnet.api.exceptions;

/**
 * This class represents an exception that occurs during data transformation operations.
 * It extends the RuntimeException class, making it an unchecked exception.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.2
 */
public class TransformerException extends RuntimeException {

    /**
     * Constructs a new TransformerException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     *                describing the cause of the exception.
     */
    public TransformerException(String message) {
        super(message);
    }

}
