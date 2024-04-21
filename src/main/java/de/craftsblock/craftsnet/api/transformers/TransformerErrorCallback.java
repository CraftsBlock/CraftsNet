package de.craftsblock.craftsnet.api.transformers;

import de.craftsblock.craftsnet.api.transformers.exceptions.TransformerException;

/**
 * <p>The TransformerErrorCallback interface defines a contract for handling transformer errors within the CraftsNet API.
 * Implementations of this interface can be used to customize error handling behavior when transformer exceptions occur.</p>
 *
 * <p>Transformer errors may arise during the transformation process when data cannot be processed or converted as expected.
 * This interface allows developers to define specific error-handling logic tailored to their application requirements.</p>
 *
 * <p>Implementing classes must provide an implementation for the {@link #handleError(TransformerException)} method,
 * which receives a TransformerException object representing the error that occurred during transformation.
 * Within this method, developers can implement custom error-handling logic, such as logging, error reporting, or recovery actions.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see TransformerException
 * @since 3.0.3
 */
public interface TransformerErrorCallback {

    /**
     * Handles transformer errors by implementing custom error-handling logic.
     *
     * @param transformerException The TransformerException representing the error that occurred during transformation.
     * @throws Exception if an error occurs while handling the transformer exception.
     */
    void handleError(TransformerException transformerException) throws Exception;

}
