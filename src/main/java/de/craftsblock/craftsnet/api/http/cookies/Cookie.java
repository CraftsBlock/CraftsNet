package de.craftsblock.craftsnet.api.http.cookies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a http cookie with various attributes such as name, value, path, domain, expiry date,
 * same-site policy, security, and HttpOnly flag.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6
 */
public class Cookie {

    private final String name;
    private Object value;

    private String path = null;
    private String domain = null;
    private OffsetDateTime expiresAt = null;
    private long maxAge = -2;
    private SameSite sameSite = null;
    private boolean secure = true;
    private boolean httpOnly = true;

    /**
     * Constructs a new Cookie with the specified name and no value.
     *
     * @param name The name of the cookie, cannot be null
     */
    public Cookie(@NotNull String name) {
        this(name, null);
    }

    /**
     * Constructs a new Cookie with the specified name and value.
     *
     * @param name  The name of the cookie, cannot be null
     * @param value The value of the cookie, can be null
     */
    public Cookie(@NotNull String name, @Nullable Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Sets a specified attribute of the cookie based on the flag provided.
     * <p>
     * This method allows setting various cookie attributes using a flag and its corresponding argument.
     * Recognized flags are "Path", "Domain", "Expires", "SameSite", "Secure", and "HttpOnly".
     *
     * @param flag The attribute to be set, cannot be null
     * @param arg  The value for the attribute, can be null for attributes that accept null values
     * @return The current Cookie object, for method chaining
     */
    public Cookie setFlag(@NotNull String flag, @Nullable String arg) {
        switch (flag.toLowerCase()) {
            case "path" -> this.setPath(arg);
            case "domain" -> this.setDomain(arg);
            case "expires" -> this.setExpiresAt(arg);
            case "max-age", "maxage" -> {
                if (arg != null) this.setMaxAge(Long.parseLong(arg));
            }
            case "samesite" -> this.setSameSite(arg);
            case "secure" -> {
                if (arg != null) this.setSecure(Boolean.parseBoolean(arg));
                else this.setSecure(true);
            }
            case "httponly" -> {
                if (arg != null) this.setHttpOnly(Boolean.parseBoolean(arg));
                else this.setHttpOnly(true);
            }
        }

        return this;
    }

    /**
     * Returns the name of the cookie.
     *
     * @return The name of the cookie
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Retrieves the value of the cookie, cast to the specified type.
     * <p>
     * This method returns the value of the cookie if it is set, casting it to the type specified by the caller.
     * If the value is null, it returns null. The caller must ensure that the type cast is correct.
     *
     * @param <T> The type to which the cookie value should be cast
     * @return The value of the cookie cast to the specified type, or null if the value is not set
     * @throws ClassCastException If the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getValue() {
        if (this.value == null) return null;
        return (T) this.value;
    }

    /**
     * Sets the value of the cookie.
     *
     * @param value The new value of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setValue(@Nullable Object value) {
        this.value = value;
        return this;
    }

    /**
     * Returns the path of the cookie.
     *
     * @return The path of the cookie, or null if not set
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path of the cookie.
     *
     * @param path The new path of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setPath(@Nullable String path) {
        this.path = path;
        return this;
    }

    /**
     * Returns the domain of the cookie.
     *
     * @return The domain of the cookie, or null if not set
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain of the cookie.
     *
     * @param domain The new domain of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setDomain(@Nullable String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Returns the expiry date of the cookie.
     *
     * @return The expiry date of the cookie, or null if not set
     */
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Sets the expiry date of the cookie using a string representation.
     *
     * @param expiresAt The new expiry date of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setExpiresAt(@Nullable String expiresAt) {
        if (expiresAt == null) return this.setExpiresAt((OffsetDateTime) null);
        else return this.setExpiresAt(OffsetDateTime.parse(expiresAt));
    }

    /**
     * Sets the expiry date of the cookie using a TemporalAccessor.
     *
     * @param expiresAt The new expiry date of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setExpiresAt(@Nullable OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Returns the maximum age of the cookie.
     *
     * @return The maximum age of the cookie, if disabled -1 will be returned.
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Sets the maximum age of this cookie. Set to -2 to disable this flag.
     *
     * @param maxAge The number in seconds the cookie is valid.
     */
    public Cookie setMaxAge(long maxAge) {
        if (maxAge < -1) return this;
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Returns the SameSite policy of the cookie.
     *
     * @return The SameSite policy of the cookie, or null if not set
     */
    public SameSite getSameSite() {
        return sameSite;
    }

    /**
     * Sets the SameSite policy of the cookie using a string representation.
     *
     * @param sameSite The new SameSite policy of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setSameSite(@Nullable String sameSite) {
        if (sameSite == null) return this.setSameSite((SameSite) null);
        else return this.setSameSite(SameSite.valueOf(sameSite.toUpperCase()));
    }

    /**
     * Sets the SameSite policy of the cookie.
     *
     * @param sameSite The new SameSite policy of the cookie, can be null
     * @return The current Cookie object, for method chaining
     */
    public Cookie setSameSite(@Nullable SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    /**
     * Returns whether the cookie is secure.
     *
     * @return true if the cookie is secure, false otherwise
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Sets whether the cookie is secure.
     *
     * @param secure true to make the cookie secure, false otherwise
     * @return The current Cookie object, for method chaining
     */
    public Cookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Returns whether the cookie is HttpOnly.
     *
     * @return true if the cookie is HttpOnly, false otherwise
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Sets whether the cookie is HttpOnly.
     *
     * @param httpOnly true to make the cookie HttpOnly, false otherwise
     * @return The current Cookie object, for method chaining
     */
    public Cookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    /**
     * Overrides the current cookie's attributes with those of another cookie.
     *
     * @param cookie The cookie to copy attributes from
     * @return The current Cookie object, for method chaining
     */
    public Cookie override(Cookie cookie) {
        assert this.name.equalsIgnoreCase(cookie.name);
        this.value = cookie.getValue();
        this.path = cookie.getPath();
        this.domain = cookie.getDomain();
        this.expiresAt = cookie.getExpiresAt();
        this.sameSite = cookie.getSameSite();
        this.secure = cookie.isSecure();
        this.httpOnly = cookie.isHttpOnly();
        return this;
    }

    /**
     * Marks the cookie as deleted by setting its expiry date to a date in the past.
     *
     * @return The current Cookie object, for method chaining
     */
    public Cookie markDeleted() {
        setExpiresAt(OffsetDateTime.now().minusYears(5));
        setMaxAge(-1);
        return this;
    }

    /**
     * Returns a string representation of the cookie.
     *
     * @return The string representation of the cookie
     */
    @Override
    public String toString() {
        return this.name + "=" + (this.value != null ? this.value.toString() : "") +
                (this.path != null ? "; Path=" + this.path : "") +
                (this.domain != null ? "; Domain=" + this.domain : "") +
                (this.expiresAt != null ? "; Expires=" + DateTimeFormatter.RFC_1123_DATE_TIME.format(expiresAt) : "") +
                (this.maxAge >= -1 ? "; Max-Age=" + maxAge : "") +
                (this.sameSite != null ? "; SameSite=" + this.sameSite : "") +
                (this.secure || (this.sameSite != null && this.sameSite.equals(SameSite.NONE)) ? "; Secure" : "") +
                (this.httpOnly ? "; HttpOnly" : "");
    }

    /**
     * Compares this cookie to the specified object for equality.
     *
     * @param o The object to compare this cookie against
     * @return true if the specified object is equal to this cookie, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cookie cookie = (Cookie) o;
        return isSecure() == cookie.isSecure() && isHttpOnly() == cookie.isHttpOnly() && Objects.equals(getName(), cookie.getName()) && Objects.equals(getValue(), cookie.getValue()) && Objects.equals(getPath(), cookie.getPath()) && Objects.equals(getDomain(), cookie.getDomain()) && Objects.equals(getExpiresAt(), cookie.getExpiresAt()) && getSameSite() == cookie.getSameSite();
    }

    /**
     * Returns the hash code value for this cookie.
     *
     * @return The hash code value for this cookie
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue(), getPath(), getDomain(), getExpiresAt(), getSameSite(), isSecure(), isHttpOnly());
    }

}
