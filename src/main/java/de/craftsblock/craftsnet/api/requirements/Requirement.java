package de.craftsblock.craftsnet.api.requirements;

import de.craftsblock.craftsnet.api.RouteRegistry;

import java.lang.annotation.Annotation;

/**
 * Represents an abstract requirement that can be used in the requirement system.
 *
 * @param <T> The type of the storage class that this requirement gets its data from.
 * @param <R> The type of the route mapping class that this requirement uses.
 *
 * @since 3.0.5-SNAPSHOT
 * @version 1.0.0
 * @author CraftsBlock
 * @author Philipp Maywald
 */
public abstract class Requirement<T extends RequireAble, R extends RouteRegistry.EndpointMapping> {

    private final Class<? extends Annotation> annotation;

    /**
     * Constructs a new requirement with the specified annotation class. The annotation will
     * be used to determine the requirement on the methods of endpoints.
     *
     * @param annotation the annotation class that this requirement is associated with.
     */
    public Requirement(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    /**
     * Determines if this requirement applies to the specified mapping and content.
     *
     * @param t the storage instance
     * @param r the route mapping instance
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    public abstract boolean applies(T t, R r);

    /**
     * Gets the annotation class that this requirement is associated with.
     *
     * @return the annotation class
     */
    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

}
