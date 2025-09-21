package de.craftsblock.craftsnet.api.requirements.meta;

import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Represents a link between a {@link Requirement} implementation and its
 * reflective {@link Method} reference to the {@code applies(...)} method.
 *
 * @param requirement The requirement instance.
 * @param method      The reflective method reference to {@code applies(...)}.
 * @param <T>         The requirement type.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Requirement
 * @since 3.5.3
 */
public record RequirementMethodLink<T extends Requirement<?>>(T requirement, Method method) {

    /**
     * Creates a new {@link RequirementMethodLink} for the given requirement by
     * resolving its generic type argument and locating the {@code applies(...)} method
     * using reflection.
     *
     * @param requirement the requirement instance.
     * @param <T>         the requirement type.
     * @return a new {@link RequirementMethodLink} pointing to the requirements
     * {@code applies(...)} method.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Requirement<?>> RequirementMethodLink<T> create(T requirement) {
        Class<T> requirementClass = (Class<T>) requirement.getClass();
        Class<?> args = ReflectionUtils.extractGeneric(requirementClass, Requirement.class, 0);
        Method method = ReflectionUtils.findMethod(requirementClass, "applies", args);

        return new RequirementMethodLink<>(requirement, method);
    }

}
