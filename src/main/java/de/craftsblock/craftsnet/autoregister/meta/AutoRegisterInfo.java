package de.craftsblock.craftsnet.autoregister.meta;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

/**
 * The {@link AutoRegisterInfo} holds information about a class to be processed in an auto registration
 * process. It contains the class name, the annotation associated with the class, the class loader,
 * and a list of parent types (superclasses and interfaces) of the class.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.2.0-SNAPSHOT
 */
public record AutoRegisterInfo(String className, Annotation annotation, ClassLoader loader, List<String> parentTypes) {

    /**
     * Creates a new instance of {@link AutoRegisterInfo}.
     *
     * @param className   The name of the class.
     * @param annotation  The annotation associated with the class.
     * @param loader      The class loader that was used to load the class.
     * @param parentTypes A list of the names of the parent types (superclasses and interfaces) of the class.
     * @return A new instance of {@link AutoRegisterInfo}.
     */
    public static AutoRegisterInfo of(String className, Annotation annotation, ClassLoader loader, List<String> parentTypes) {
        return new AutoRegisterInfo(className, annotation, loader, parentTypes);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares two {@code AutoRegisterInfo} objects for equality based on their class name and annotation.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoRegisterInfo that = (AutoRegisterInfo) o;
        return Objects.equals(className, that.className) && Objects.equals(annotation, that.annotation);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calculates the hash code based on the class name and annotation.</p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(className, annotation);
    }

}
