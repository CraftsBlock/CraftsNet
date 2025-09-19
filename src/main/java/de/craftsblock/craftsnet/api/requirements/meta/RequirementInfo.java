package de.craftsblock.craftsnet.api.requirements.meta;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents detailed information about a specific requirement annotation.
 *
 * <p>This class processes and stores metadata, methods, and values associated with the annotation.</p>
 *
 * @param annotation The class of the annotation.
 * @param meta       The {@link RequirementMeta} annotation metadata.
 * @param values     A map of key-value pairs representing the annotation's values.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see RequirementMeta
 * @see RequirementStore
 * @since 3.1.0-SNAPSHOT
 */
public record RequirementInfo(Class<? extends Annotation> annotation, RequirementMeta meta, Map<String, Object> values) {

    /**
     * Constructs a {@link RequirementInfo} from a given annotation.
     *
     * @param annotation The annotation instance.
     */
    public RequirementInfo(Annotation annotation) {
        this(annotation.annotationType(), loadMeta(annotation.annotationType()), storeValues(annotation));
    }

    /**
     * Checks whether the given key exists in the annotation's values.
     *
     * @param key The key to check.
     * @return {@code true} if the key exists, {@code false} otherwise.
     */
    public boolean hasValue(@NotNull String key) {
        return values.containsKey(key);
    }

    /**
     * Retrieves the value associated with a given key from the annotation's values.
     *
     * @param key The key to retrieve the value for.
     * @param <T> The expected return value type.
     * @return The value associated with the key, or {@code null} if not present.
     */
    @SuppressWarnings("unchecked")
    public <T> @Unmodifiable T getValue(@NotNull String key) {
        if (!values.containsKey(key)) return null;
        return (T) values.get(key);
    }

    /**
     * Extracts the values of an annotation by invoking its methods.
     *
     * @param annotation The annotation to extract values from.
     * @return A map of key-value pairs representing the annotation's data.
     */
    private static Map<String, Object> storeValues(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();

        RequirementMeta meta = loadMeta(type);
        if (meta.type().equals(RequirementType.FLAG)) return Map.of();

        // Collect methods explicitly defined in the metadata
        List<Method> methods = new ArrayList<>();
        methods.addAll(Arrays.stream(meta.methods())
                .map(name -> Utils.getMethod(type, name))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        // Collect methods annotated with @RequirementStore
        methods.addAll(Utils.getMethodsByAnnotation(type, RequirementStore.class));

        // Invoke and process methods
        return methods.stream()
                .distinct()
                .filter(method -> !method.getReturnType().equals(Void.TYPE))
                .map(method -> {
                    Object value = ReflectionUtils.invokeMethod(annotation, method);
                    if (value == null) return null; // Skip null values

                    // Convert arrays to lists for better compatibility
                    return Map.entry(method.getName(), value.getClass().isArray() ? List.of((Object[]) value) : value);
                }).filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, o2) -> o));
    }

    /**
     * Loads the {@link RequirementMeta} from a given annotation type.
     *
     * @param annotation The annotation class to load metadata from.
     * @return The loaded {@link RequirementMeta}.
     */
    private static RequirementMeta loadMeta(Class<? extends Annotation> annotation) {
        // Return default meta if none is defined
        if (!annotation.isAnnotationPresent(RequirementMeta.class))
            return RequirementMeta.class.getDeclaredAnnotation(RequirementMeta.class);

        return annotation.getDeclaredAnnotation(RequirementMeta.class);
    }

    /**
     * Merges two {@link RequirementInfo} instances into a single one.
     *
     * <p>Conflicts between values are resolved as follows:</p>
     * <ul>
     *     <li>If the values are arrays, they are concatenated.</li>
     *     <li>Otherwise, the value from the second instance overrides the first.</li>
     * </ul>
     *
     * @param first  the first {@code RequirementInfo}.
     * @param second the second {@code RequirementInfo}.
     * @return a merged {@code RequirementInfo}.
     * @throws IllegalStateException if the two instances refer to different annotations.
     */
    public static RequirementInfo merge(RequirementInfo first, RequirementInfo second) {
        if (!first.annotation().equals(second.annotation))
            throw new IllegalStateException("");

        Map<String, Object> mergedValues = new HashMap<>(first.values());

        for (Map.Entry<String, Object> entry : second.values().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (mergedValues.containsKey(key)) {
                if (value.getClass().isArray()) {
                    // Merge arrays into a single list
                    Object merged = Stream.concat(
                            Arrays.stream((Object[]) Objects.requireNonNull(first.getValue(key))),
                            Arrays.stream((Object[]) value)
                    ).toList();

                    mergedValues.put(key, merged);
                } else
                    // Override value with the second's
                    mergedValues.put(key, value);
                continue;
            }

            mergedValues.put(key, value); // Add new value
        }

        return new RequirementInfo(first.annotation(), first.meta(), mergedValues);
    }

}
