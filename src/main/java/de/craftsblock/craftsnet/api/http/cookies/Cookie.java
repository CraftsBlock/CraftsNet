package de.craftsblock.craftsnet.api.http.cookies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
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
    private TemporalAccessor expiresAt = null;
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
     * Returns the name of the cookie.
     *
     * @return The name of the cookie
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Returns the value of the cookie.
     *
     * @return The value of the cookie, or null if not set
     */
    public @Nullable Object getValue() {
        return value;
    }

    /**
     * Sets the value of the cookie.
     *
     * @param value The new value of the cookie, can be null
     */
    public void setValue(@Nullable Object value) {
        this.value = value;
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
     */
    public void setPath(@Nullable String path) {
        this.path = path;
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
     */
    public void setDomain(@Nullable String domain) {
        this.domain = domain;
    }

    /**
     * Returns the expiry date of the cookie.
     *
     * @return The expiry date of the cookie, or null if not set
     */
    public TemporalAccessor getExpiresAt() {
        return expiresAt;
    }

    /**
     * Sets the expiry date of the cookie using a string representation.
     *
     * @param expiresAt The new expiry date of the cookie, can be null
     */
    public void setExpiresAt(@Nullable String expiresAt) {
        if (expiresAt == null) this.setExpiresAt((TemporalAccessor) null);
        else this.setExpiresAt(OffsetDateTime.parse(expiresAt));
    }

    /**
     * Sets the expiry date of the cookie using a TemporalAccessor.
     *
     * @param expiresAt The new expiry date of the cookie, can be null
     */
    public void setExpiresAt(@Nullable TemporalAccessor expiresAt) {
        this.expiresAt = expiresAt;
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
     */
    public void setSameSite(@Nullable String sameSite) {
        if (sameSite == null) this.setSameSite((SameSite) null);
        else this.setSameSite(SameSite.valueOf(sameSite.toUpperCase()));
    }

    /**
     * Sets the SameSite policy of the cookie.
     *
     * @param sameSite The new SameSite policy of the cookie, can be null
     */
    public void setSameSite(@Nullable SameSite sameSite) {
        this.sameSite = sameSite;
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
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
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
     */
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Overrides the current cookie's attributes with those of another cookie.
     *
     * @param cookie The cookie to copy attributes from
     */
    public void override(Cookie cookie) {
        assert this.name.equalsIgnoreCase(cookie.name);
        this.value = cookie.getValue();
        this.path = cookie.getPath();
        this.domain = cookie.getDomain();
        this.expiresAt = cookie.getExpiresAt();
        this.sameSite = cookie.getSameSite();
        this.secure = cookie.isSecure();
        this.httpOnly = cookie.isHttpOnly();
    }

    /**
     * Marks the cookie as deleted by setting its expiry date to a date in the past.
     */
    public void markDeleted() {
        setExpiresAt(OffsetDateTime.now().minusYears(5));
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
                (this.path != null ? "; Expires=" + DateTimeFormatter.RFC_1123_DATE_TIME.format(this.expiresAt) : "") +
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
