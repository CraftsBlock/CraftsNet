package de.craftsblock.craftsnet.api.requirements.meta;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
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
 * @version 1.0.1
 * @see Requirement
 * @since 3.5.3
 */
public record RequirementMethodLink<R extends RequireAble, T extends Requirement<R>>(T requirement, Class<R> arg, Method method) {

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
    public static <R extends RequireAble, T extends Requirement<R>> RequirementMethodLink<R, T> create(T requirement) {
        Class<T> requirementClass = (Class<T>) requirement.getClass();

        Class<R> arg = ReflectionUtils.extractGeneric(requirementClass, Requirement.class, 0);
        Method method = ReflectionUtils.findMethod(requirementClass, "applies", arg, RouteRegistry.EndpointMapping.class);

        return new RequirementMethodLink<>(requirement, arg, method);
    }

}
