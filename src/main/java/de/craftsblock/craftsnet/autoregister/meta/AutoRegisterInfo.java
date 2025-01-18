package de.craftsblock.craftsnet.autoregister.meta;

import de.craftsblock.craftsnet.addon.Addon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

/**
 * The {@link AutoRegisterInfo} holds information about a class to be processed in an auto registration
 * process. It contains the class name, the annotation associated with the class, the class loader,
 * and a list of parent types (superclasses and interfaces) of the class.
 * Optional it contains the addon that the source is located in. If no addons was set, the source is
 * not located inside an addon.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @since 3.2.0-SNAPSHOT
 */
public record AutoRegisterInfo(@NotNull String className, @Nullable Addon bounding, @NotNull Annotation annotation, @NotNull ClassLoader loader,
                               @NotNull List<String> parentTypes) {

    /**
     * Creates a new instance of {@link AutoRegisterInfo}.
     *
     * @param className   The name of the class.
     * @param bounding    The addon that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param annotation  The annotation associated with the class.
     * @param loader      The class loader that was used to load the class.
     * @param parentTypes A list of the names of the parent types (superclasses and interfaces) of the class.
     * @return A new instance of {@link AutoRegisterInfo}.
     */
    public static AutoRegisterInfo of(@NotNull String className, @Nullable Addon bounding, @NotNull Annotation annotation,
                                      @NotNull ClassLoader loader, @NotNull List<String> parentTypes) {
        return new AutoRegisterInfo(className, bounding, annotation, loader, parentTypes);
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
