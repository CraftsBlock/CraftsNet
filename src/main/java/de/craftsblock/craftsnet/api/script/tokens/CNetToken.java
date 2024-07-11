package de.craftsblock.craftsnet.api.script.tokens;

import org.jetbrains.annotations.NotNull;

/**
 * A record representing a token in the CNet scripting language.
 * Each token has a type and a value.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public record CNetToken(@NotNull CNetTokenType type, @NotNull String value) {

    /**
     * Constructs a new CNetToken.
     *
     * @param type  the type of the token, must not be null
     * @param value the value of the token, must not be null
     */
    public CNetToken {
    }

    /**
     * Gets the type of this token.
     *
     * @return the type of the token
     */
    @Override
    public @NotNull CNetTokenType type() {
        return type;
    }

    /**
     * Gets the value of this token.
     *
     * @return the value of the token
     */
    @Override
    public @NotNull String value() {
        return value;
    }

}
