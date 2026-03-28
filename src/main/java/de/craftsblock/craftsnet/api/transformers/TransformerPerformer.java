package de.craftsblock.craftsnet.api.transformers;

import de.craftsblock.craftscore.cache.DoubleKeyedCache;
import de.craftsblock.craftscore.cache.DoubleKeyedLruCache;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.transformers.annotations.Transformer;
import de.craftsblock.craftsnet.api.transformers.annotations.TransformerCollection;
import de.craftsblock.craftsnet.api.transformers.exceptions.TransformerException;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
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
 * @see Transformer
 * @see TransformerCollection
 * @see Transformable
 * @since 3.0.3-SNAPSHOT
 */
public class TransformerPerformer {

    private final Logger logger;

    private final DoubleKeyedLruCache<Class<? extends Transformable<?, ?>>, Object, Object> transformerCache = new DoubleKeyedLruCache<>(10);
    private final List<String> groupNames = new ArrayList<>();

    private final int argsOffset;
    private final TransformerErrorCallback callback;

    private Pattern validator;

    /**
     * Constructor for TransformerPerformer.
     *
     * @param craftsNet  The CraftsNet instance which instantiates this
     * @param argsOffset The offset for arguments.
     * @param callback   The callback responsible for handling transformer exception.
     */
    public TransformerPerformer(@NotNull CraftsNet craftsNet, int argsOffset, @Nullable TransformerErrorCallback callback) {
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
        if (validator == null) {
            return;
        }

        if (this.validator != null && this.validator.pattern().equals(validator.pattern())) {
            return;
        }

        this.validator = validator;
        if (!this.groupNames.isEmpty()) {
            this.groupNames.clear();
        }

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
    public boolean perform(Handler handler, Method method, Object[] args) throws Exception {
        if (hasNoTransformers(handler) && hasNoTransformers(method))
            return true;

        applyTransformers(args, handler);
        applyTransformers(args, method);

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != args.length) {
            return true;
        }

        for (int i = this.argsOffset; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            Object value = args[i];

            if (value instanceof TransformerException e) {
                if (callback != null) {
                    callback.handleError(e);
                }

                return false;
            }

            if (!type.isAssignableFrom(value.getClass())) {
                String name = type.getSimpleName();

                Method converter = ReflectionUtils.findMethod(value.getClass(), name + "Value");
                if (converter == null) {
                    continue;
                }

                args[i] = ReflectionUtils.invokeMethod(value, converter);
            }
        }

        return true;
    }

    /**
     * Checks whether transformers are present in a method or class.
     *
     * @param obj The method or the handler which contains the information about the transformers.
     * @return {@code true} if there are transformers present, {@code false} otherwise
     */
    public boolean hasNoTransformers(Object obj) {
        return (obj instanceof Method method ? method : obj.getClass()).getAnnotation(TransformerCollection.class) == null &&
                (obj instanceof Method method ? method : obj.getClass()).getAnnotation(Transformer.class) == null;
    }

    /**
     * Applies the transformation process to the obj.
     *
     * @param args The args which should be transformed.
     * @param obj  The method or the handler which contains the information about the transformers.
     * @throws NoSuchMethodException  if the transformer method could not be found.
     * @throws InstantiationException if no new instance of the Transformable can be created.
     * @throws IllegalAccessException if access to the constructor of Transformable or
     *                                access to the method Transformable.transform(String) is restricted.
     */
    private void applyTransformers(Object[] args, Object obj) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        TransformerCollection transformers = (obj instanceof Method method ? method : obj.getClass()).getAnnotation(TransformerCollection.class);
        if (transformers != null) {
            for (Transformer transformer : transformers.value()) {
                transform(groupNames, args, transformer);
            }

            return;
        }

        Transformer standaloneTransformer = (obj instanceof Method method ? method : obj.getClass()).getAnnotation(Transformer.class);
        if (standaloneTransformer != null) {
            transform(groupNames, args, standaloneTransformer);
        }

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
        if (!groupNames.contains(parameter)) {
            logger.warning("Parameter %s has a transformer but is not used!", parameter);
            return;
        }

        int groupIndex = groupNames.indexOf(parameter) + this.argsOffset;
        String value = (String) args[groupIndex];
        try {
            Class<? extends Transformable<?, ?>> transformable = transformer.transformer();
            args[groupIndex] = transform(value, transformer, transformable);
        } catch (RuntimeException | InvocationTargetException parent) {
            if (parent.getCause() == null) {
                return;
            }

            args[groupIndex] = getTransformerException(parent);
        }
    }

    /**
     * Wraps a given exception into a {@link TransformerException}.
     *
     * @param parent The exception that should be wrapped.
     * @return The wrapped exception.
     */
    private static @NotNull TransformerException getTransformerException(Exception parent) {
        Throwable cause = parent.getCause();
        TransformerException exception;
        if (cause instanceof TransformerException e) {
            exception = e;
        } else if (cause.getCause() != null && cause.getCause() instanceof TransformerException e) {
            exception = e;
        } else {
            exception = null;
        }

        if (exception == null) {
            throw (parent instanceof RuntimeException re ? re : new RuntimeException(parent));
        }
        return exception;
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

        Object value;
        if (transformable.getParent() != null) {
            value = transform(parameter, transformer, transformable.getParent());
        } else {
            value = parameter;
        }

        if (transformer.cacheable() && transformable.isCacheable() && transformerCache.containsPair(type, value)) {
            return transformerCache.get(type, value);
        }

        Method transformerMethod = ReflectionUtils.findMethod(type, "transform", Object.class);
        if (transformerMethod == null) {
            throw new IllegalStateException("Transformer " + type.getName() + " does not have a transformer method!");
        }

        Object transformed = ReflectionUtils.invokeMethod(transformable, transformerMethod, value);
        if (transformer.cacheable() && transformable.isCacheable()) {
            transformerCache.put(type, value, transformed);
        }

        return transformed;
    }

}
