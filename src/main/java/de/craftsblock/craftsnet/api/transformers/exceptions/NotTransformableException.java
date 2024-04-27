package de.craftsblock.craftsnet.api.transformers.exceptions;

/**
 * This exception is thrown when a parameter cannot be transformed to a specified target type.
 * It extends the TransformerException class, indicating a specific type of transformation failure.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see TransformerException
 * @since CraftsNet-3.0.2
 */
public class NotTransformableException extends TransformerException {

    /**
     * Construct a new NotTransformableException which indicates that something bad happened while transforming the value.
     *
     * @param parameter  The dynamic URL parameter value that failed transforming.
     * @param targetType The class to which the value should be transformed, but isn't suitable for the value.
     */
    public NotTransformableException(String parameter, Class<?> targetType) {
        super(
                "\"" + parameter + "\" is not transformable to type " +
                        targetType.getSimpleName().split("\\.")[targetType.getSimpleName().split("\\.").length - 1]
        );
    }

}
