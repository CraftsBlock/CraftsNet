package de.craftsblock.craftsnet.api.requirements.meta;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

/**
 * Represents a link between a {@link Requirement} implementation and its
 * require-able type.
 *
 * @param requirement The requirement instance.
 * @param arg         The exact type the {@link Requirement#applies(RequireAble, RouteRegistry.EndpointMapping)}
 *                    method takes as the require-able.
 * @param <R>         The require-able type of the requirement.
 * @param <T>         The requirement type.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @see Requirement
 * @since 3.5.3
 */
public record RequirementMethodLink<R extends RequireAble, T extends Requirement<R>>(T requirement, Class<R> arg) {

    /**
     * Creates a new {@link RequirementMethodLink} for the given requirement by
     * resolving its generic type argument.
     *
     * @param requirement The requirement instance.
     * @param <T>         The requirement type.
     * @param <R>         The require-able type of the requirement.
     * @return a new {@link RequirementMethodLink} pointing to the requirements
     * {@code applies(...)} method.
     */
    @SuppressWarnings("unchecked")
    public static <R extends RequireAble, T extends Requirement<R>> RequirementMethodLink<R, T> create(T requirement) {
        Class<T> requirementClass = (Class<T>) requirement.getClass();
        Class<R> arg = ReflectionUtils.extractGeneric(requirementClass, Requirement.class, 0);

        return new RequirementMethodLink<>(requirement, arg);
    }

}
