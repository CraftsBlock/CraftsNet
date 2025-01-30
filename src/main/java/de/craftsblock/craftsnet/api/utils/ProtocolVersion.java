package de.craftsblock.craftsnet.api.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

/**
 * Represents a specific version of a protocol, including its scheme, major version, and minor version.
 * <p>
 * This class is useful for managing different protocol versions and supports comparison,
 * parsing, and equality checks.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.2-SNAPSHOT
 */
public record ProtocolVersion(@NotNull Scheme scheme, @Range(from = 0, to = Integer.MAX_VALUE) int major,
                              @Range(from = 0, to = Integer.MAX_VALUE) int minor) implements Comparable<ProtocolVersion> {

    /**
     * Constructs a new {@link ProtocolVersion} instance.
     *
     * @param scheme The protocol scheme.
     * @param major  The major version number (must be non-negative).
     * @param minor  The minor version number (must be non-negative).
     */
    public ProtocolVersion {
    }

    /**
     * Returns the scheme associated with this protocol version.
     *
     * @return The {@link Scheme} of the protocol.
     */
    @Override
    public Scheme scheme() {
        return scheme;
    }

    /**
     * Returns the major version number.
     *
     * @return The major version.
     */
    @Override
    public int major() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return The minor version.
     */
    @Override
    public int minor() {
        return minor;
    }

    /**
     * Compares this protocol version to another.
     * <p>
     * A protocol version is considered greater if its major version is higher.
     * If major versions are equal, the minor version is compared.
     * </p>
     *
     * @param that The {@link ProtocolVersion} to compare against.
     * @return {@code -1} if this version is older, {@code 1} if it is newer, and {@code 0} if they are equal.
     * @throws IllegalStateException If the schemes are not from the same family.
     */
    @Override
    public int compareTo(@NotNull ProtocolVersion that) {
        if (!this.scheme().isSameFamily(that.scheme()))
            throw new IllegalStateException("Can not compare scheme " + this.scheme() + " to " + that.scheme());

        int majorDiff = this.major() - that.major();
        if (majorDiff != 0) return Math.min(Math.max(majorDiff, -1), 1);

        int minorDiff = this.minor() - that.minor();
        return Math.min(Math.max(minorDiff, -1), 1);
    }

    /**
     * Checks if this protocol version is equal to another object.
     *
     * @param o The object to compare against.
     * @return {@code true} if both objects represent the same protocol version, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolVersion that = (ProtocolVersion) o;
        return major() == that.major() && minor() == that.minor() && scheme() == that.scheme();
    }

    /**
     * Generates a hash code for this protocol version.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(scheme(), major(), minor());
    }

    /**
     * Parses a protocol version string using the default dot separator (".").
     *
     * @param scheme  The protocol scheme.
     * @param version The version string.
     * @return A new {@link ProtocolVersion} instance.
     * @throws RuntimeException If the version string is not a valid format.
     */
    public static ProtocolVersion parse(@NotNull Scheme scheme, @NotNull String version) {
        return parse(scheme, version, "\\.");
    }

    /**
     * Parses a protocol version string using a custom separator.
     *
     * @param scheme    The protocol scheme.
     * @param version   The version string.
     * @param separator The separator used in the version string.
     * @return A new {@link ProtocolVersion} instance.
     * @throws RuntimeException If the version string is not a valid format.
     */
    public static ProtocolVersion parse(@NotNull Scheme scheme, @NotNull String version, @NotNull String separator) {
        try {
            String[] versionParts = version.split(separator);
            int major = Integer.parseInt(versionParts[0]);
            int minor = versionParts.length >= 2 ? Integer.parseInt(versionParts[1]) : 0;
            return new ProtocolVersion(scheme, major, minor);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

}
