package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftsnet.api.script.ast.ASTNode;
import de.craftsblock.craftsnet.api.script.tokens.CNetToken;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The parser for the CNet scripting language.
 * It converts a list of tokens into an abstract syntax tree (AST).
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class CNetParser {

    /**
     * The current version of the CNetParser.
     */
    public static final String VERSION = "1";

    private final List<CNetToken> tokens;
    private int position;

    /**
     * Constructs a new CNetParser.
     *
     * @param tokens the list of tokens to parse
     */
    public CNetParser(List<CNetToken> tokens) {
        this.tokens = tokens;
        reset();
    }

    /**
     * Resets the parser position to the beginning.
     */
    public void reset() {
        this.position = 0;
    }

    /**
     * Advances the parser position by one token.
     */
    public void move() {
        this.position += 1;
    }

    /**
     * Gets the current token.
     *
     * @return the current token
     */
    public CNetToken getToken() {
        return this.tokens.get(position);
    }

    /**
     * Returns a sublist of tokens starting from a given position and attempts to return the specified number of tokens.
     * If there are not enough tokens from the position to the end of the list, it includes preceding tokens to ensure
     * the total number of tokens is returned, unless there are fewer tokens in total than requested.
     *
     * @param tokens the number of tokens to return.
     * @return a list containing the specified number of tokens from the current position or fewer if the list
     * does not have enough elements.
     * @throws IndexOutOfBoundsException if the position is out of bounds or tokens is negative.
     */
    private List<CNetToken> getNTokens(@Range(from = 0, to = Integer.MAX_VALUE) int tokens) {
        int size = this.tokens.size();
        if (position < 0 || position >= size)
            throw new IndexOutOfBoundsException("Invalid position or tokens count");

        int available = size - position;

        int start = Math.max(0, position - (tokens - available));
        int end = Math.min(position + tokens, size);

        return this.tokens.subList(start, end);
    }

    /**
     * Checks if the current token matches any of the specified types.
     *
     * @param types the token types to check against
     * @return true if the current token matches one of the specified types, false otherwise
     */
    public boolean canConsume(CNetTokenType... types) {
        if (types.length == 0) return false;

        CNetToken token = getToken();
        for (CNetTokenType type : types)
            if (token.type() == type)
                return true;

        return false;
    }

    /**
     * Consumes the current token if it matches one of the specified types.
     *
     * @param types the token types to check against
     * @return the consumed token
     * @throws IllegalStateException if the current token does not match any of the specified types
     */
    public CNetToken consume(CNetTokenType... types) {
        CNetToken token = getToken();

        if (!canConsume(types))
            throwExceptionForToken(token, types);

        move();
        return token;
    }

    /**
     * Parses the list of tokens into a list of AST nodes.
     *
     * @return an unmodifiable list of AST nodes
     * @throws IllegalStateException if an unexpected token is encountered
     */
    public List<ASTNode> parse() {
        List<ASTNode> nodes = new ArrayList<>();

        CNetToken token;
        parser:
        while ((token = getToken()).type() != CNetTokenType.EOF) {
            for (CNetTokenType type : CNetTokenType.parsableTypes) {
                if (token.type() != type) continue;

                ASTNode node = type.createNode(consume(type).line());
                node.parse(this);
                consume(CNetTokenType.SEMICOLON);

                nodes.add(node);
                continue parser;
            }

            throwExceptionForToken(token, CNetTokenType.parsableTypes.toArray(CNetTokenType[]::new));
        }

        reset();
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Throws an {@link IllegalStateException} in case of a token mismatch.
     *
     * @param token The {@link CNetToken} object representing the token that caused the error.
     * @param valid An array of {@link CNetTokenType}, representing the valid token types expected in this context.
     * @throws IllegalStateException Always throws this exception, with a message describing the token error and its context.
     */
    private void throwExceptionForToken(CNetToken token, CNetTokenType... valid) {
        throw new IllegalStateException(
                "Found token " + token.type() + " while expecting " +
                        String.join(" or ", Arrays.stream(valid).map(CNetTokenType::toString).toList()) +
                        "\nThe exception is near '" +
                        String.join(" ", getNTokens(3).stream().map(CNetToken::value).toList()).replaceAll("\\s+;", ";") +
                        "' at line " + token.line()
        );
    }

}
