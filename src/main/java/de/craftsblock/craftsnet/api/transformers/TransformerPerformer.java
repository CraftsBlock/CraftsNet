package de.craftsblock.craftsnet.api.transformers;

import de.craftsblock.craftscore.cache.DoubleKeyedCache;
import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.transformers.annotations.Transformer;
import de.craftsblock.craftsnet.api.transformers.annotations.TransformerCollection;
import de.craftsblock.craftsnet.api.transformers.exceptions.TransformerException;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static de.craftsblock.craftsnet.utils.Utils.getGroupNames;

/**
 * <p>The TransformerPerformer class is responsible for performing transformations on method arguments based on provided annotations.
 * It serves as a central component in the transformation process within the CraftsNet API.</p>
 *
 * <p>Transformations are essential for converting and validating input data before it is processed by various methods.
 * This class facilitates the application of transformers defined by annotations such as {@link Transformer} and {@link TransformerCollection},
 * allowing for dynamic and customizable data processing.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.1
 * @see Transformer
 * @see TransformerCollection
 * @see Transformable
 * @since 3.0.3-SNAPSHOT
 */
public class TransformerPerformer {

    private final Logger logger;

    private final DoubleKeyedCache<Class<? extends Transformable<?, ?>>, Object, Object> transformerCache = new DoubleKeyedCache<>(10);
    private final List<String> groupNames = new ArrayList<>();

    private final int argsOffset;
    private final TransformerErrorCallback callback;

    /**
     * Constructor for TransformerPerformer.
     *
     * @param craftsNet  The CraftsNet instance which instantiates this
     * @param validator  The pattern used for validation.
     * @param argsOffset The offset for arguments.
     */
    public TransformerPerformer(@NotNull CraftsNet craftsNet, @Nullable Pattern validator, int argsOffset) {
        this(craftsNet, validator, argsOffset, null);
    }

    /**
     * Constructor for TransformerPerformer.
     *
     * @param craftsNet  The CraftsNet instance which instantiates this
     * @param validator  The pattern used for validation.
     * @param argsOffset The offset for arguments.
     * @param callback   The callback responsible for handling transformer exception.
     */
    public TransformerPerformer(@NotNull CraftsNet craftsNet, @Nullable Pattern validator, int argsOffset, @Nullable TransformerErrorCallback callback) {
        setValidator(validator);
        this.argsOffset = argsOffset;
        this.callback = callback;
        this.logger = craftsNet.getLogger();
    }

    /**
     * Updates the validator which is used to load the group names from.
     *
     * @param validator The new validator.
     */
    public void setValidator(@Nullable Pattern validator) {
        if (validator == null) return;
        if (!this.groupNames.isEmpty()) this.groupNames.clear();
        this.groupNames.addAll(getGroupNames(validator.pattern()));
    }

    /**
     * Performs transformations on method arguments.
     *
     * @param handler The handler on which the performer should be performed
     * @param method  The method to perform transformations on.
     * @param args    The arguments containing the transform targets.
     * @return An array of transformed arguments.
     * @throws NoSuchMethodException     if the transformer method could not be found.
     * @throws InstantiationException    if no new instance of the Transformable can be created.
     * @throws IllegalAccessException    if access to the constructor of Transformable or
     *                                   access to the method Transformable.transform(String) is restricted.
     * @throws IOException               if an I/O error occurs.
     * @throws InvocationTargetException if the underlying method throws an exception.
     */
    public Object[] perform(Handler handler, Method method, Object[] args) throws Exception {
        // Return when no transformer is applied
        if (!hasTransformers(handler) && !hasTransformers(method))
            return args;

        // Copy args into a new array
        Object[] copiedArgs = new Object[args.length];
        System.arraycopy(args, 0, copiedArgs, 0, args.length);

        // Apply all the transformers
        applyTransformers(copiedArgs, handler);
        applyTransformers(copiedArgs, method);

        // Loop through all parameters of the method and checks the parameter type
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == copiedArgs.length)
            for (int i = this.argsOffset; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                Object value = copiedArgs[i];

                // Check if the value is an TransformerException and call the callback if present.
                if (value instanceof TransformerException e) {
                    if (callback != null) callback.handleError(e);
                    // Continue to the next route
                    return null;
                }

                // Check if the parameter type is not the value type
                if (!type.isAssignableFrom(value.getClass())) {
                    String name = type.getSimpleName();

                    // Gets and checks if a method for an alternative transformation is present.
                    // This allows for example the use of both Integer and int
                    Method converter = Utils.getMethod(value.getClass(), name + "Value");
                    if (converter == null) continue;

                    // Set the value of the argument to the return of the method for alternativ transformation.
                    copiedArgs[i] = converter.invoke(value);
                }
            }

        return copiedArgs;
    }

    /**
     * Checks whether transformers are present in a method or class.
     *
     * @param obj The method or the handler which contains the information about the transformers.
     * @return {@code true} if there are transformers present, {@code false} otherwise
     */
    public boolean hasTransformers(Object obj) {
        return (obj instanceof Method method ? method : obj.getClass()).getAnnotation(TransformerCollection.class) != null ||
                (obj instanceof Method method ? method : obj.getClass()).getAnnotation(Transformer.class) != null;
    }

    /**
     * Applies the transformation process to the obj.
     *
     * @param args The args which should be transformed.
     * @param obj  The method or the handler which contains the information about the transformers.
     * @return {@code true} if a transformer was applied, {@code false} otherwise.
     * @throws NoSuchMethodException  if the transformer method could not be found.
     * @throws InstantiationException if no new instance of the Transformable can be created.
     * @throws IllegalAccessException if access to the constructor of Transformable or
     *                                access to the method Transformable.transform(String) is restricted.
     */
    private boolean applyTransformers(Object[] args, Object obj) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        TransformerCollection transformers = (obj instanceof Method method ? method : obj.getClass()).getAnnotation(TransformerCollection.class);
        if (transformers != null) {
            for (Transformer transformer : transformers.value())
                transform(groupNames, args, transformer);
            return true;
        }

        Transformer standaloneTransformer = (obj instanceof Method method ? method : obj.getClass()).getAnnotation(Transformer.class);
        if (standaloneTransformer != null) {
            transform(groupNames, args, standaloneTransformer);
            return true;
        }

        return false;
    }

    /**
     * Clears the transformer cache.
     */
    public void clearCache() {
        transformerCache.clear();
    }

    /**
     * Performs a transformation with an {@link Transformer}.
     *
     * @param groupNames  A {@link List <String>} with all the named groups of the url validator.
     * @param args        A {@link Object} array with all the dynamic url parameter values.
     * @param transformer The current {@link Transformer} used to transform an argument.
     * @throws NoSuchMethodException  if the transformer method could not be not found.
     * @throws InstantiationException if no new instance of the {@link Transformable <?>} can be created.
     * @throws IllegalAccessException if the access to the constructor of the {@link Transformable} or
     *                                if the access to the method {@link Transformable#transform(Object)} is restricted.
     */
    private void transform(List<String> groupNames, Object[] args, Transformer transformer) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        String parameter = transformer.parameter();
        // Abort if the dynamic parameter is not present in the named groups
        if (!groupNames.contains(parameter)) {
            logger.warning("Parameter %s has a transformer but is not used!", parameter);
            return;
        }

        int groupIndex = groupNames.indexOf(parameter) + this.argsOffset;
        String value = (String) args[groupIndex];
        try {
            // Load all important variables
            Class<? extends Transformable<?, ?>> transformable = transformer.transformer();
            args[groupIndex] = transform(value, transformer, transformable);
        } catch (InvocationTargetException parent) {
            // Check if the cause of the InvocationTargetException is an TransformerException
            if (parent.getCause() instanceof TransformerException e)
                // Parse up the TransformerException to the route handler
                args[groupIndex] = e;
        }
    }

    /**
     * Performs the actual transformation with an {@link Transformer}.
     *
     * @param parameter   The parameter which should be transformed.
     * @param transformer The current {@link Transformer} used to transform an argument.
     * @param type        The class type of {@link Transformer#transformer()}.
     * @return The transformed value.
     * @throws NoSuchMethodException     if the transformer method could not be not found.
     * @throws InvocationTargetException if no new instance of the {@link Transformable <?>} can be created.
     * @throws InstantiationException    if the performed {@link Transformable#transform(Object)} method throws an exception.
     * @throws IllegalAccessException    if the access to the constructor of the {@link Transformable} or
     *                                   if the access to the method {@link Transformable#transform(Object)} is restricted.
     * @since 3.4.0-SNAPSHOT
     */
    private Object transform(String parameter, Transformer transformer, Class<? extends Transformable<?, ?>> type) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Transformable<?, ?> transformable = type.cast(type.getDeclaredConstructor().newInstance());

        // Check if the transformable has a parent and invoke it if one is present
        Object value;
        if (transformable.getParent() != null)
            value = transform(parameter, transformer, transformable.getParent());
        else value = parameter;

        // Check if the transformer is cacheable and the transformer cache contains this specific transformer
        if (transformer.cacheable() && transformable.isCacheable() && transformerCache.containsKeyPair(type, value))
            // Override the args with the cache value from the transformer cache
            return transformerCache.get(type, value);

        // Search for the transform method on the transformable
        Method transformerMethod = Utils.getMethod(type, "transform", Object.class);
        if (transformerMethod == null)
            throw new IllegalStateException("Transformer " + type.getName() + " does not have a transformer method!");

        try {
            // Execute the transform method on the transformable
            transformerMethod.setAccessible(true);
            Object transformed = transformerMethod.invoke(transformable, value);

            // Put the transformed value into the cache, if the transformer is cacheable
            if (transformer.cacheable() && transformable.isCacheable()) transformerCache.put(type, value, transformed);

            return transformed;
        } finally {
            transformerMethod.setAccessible(false);
        }
    }

    /**
     * Gets the arguments offset.
     *
     * @return The arguments offset.
     */
    public int getArgsOffset() {
        return argsOffset;
    }

}
