package de.craftsblock.craftsnet.api.exceptions;

/**
 * This exception is thrown when a parameter cannot be transformed to a specified target type.
 * It extends the TransformerException class, indicating a specific type of transformation failure.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see TransformerException
 * @since 3.0.2
 */
public class NotTransformableException extends TransformerException {

    public NotTransformableException(String parameter, Class<?> targetType) {
        super(
                "\"" + parameter + "\" is not transformable to type " +
                        targetType.getSimpleName().split("\\.")[targetType.getSimpleName().split("\\.").length - 1]
        );
    }

}
