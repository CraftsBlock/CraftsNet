package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftsnet.api.script.ast.ASTNode;
import de.craftsblock.craftsnet.api.script.tokens.CNetToken;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

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
    public static final String VERSION = "0";

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
            throw new IllegalStateException("Wrong token type: " + token.type() + "! (Required: " + String.join(" or ", Arrays.stream(types).map(CNetTokenType::toString).toList()) + ")");

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

                consume(type);
                ASTNode node = type.createNode();
                node.parse(this);
                consume(CNetTokenType.SEMICOLON);

                nodes.add(node);
                continue parser;
            }

            throw new IllegalStateException("Found token " + token.type() + " while expecting " + String.join(" or ", CNetTokenType.parsableTypes.stream().map(CNetTokenType::toString).toList()));
        }

        reset();
        return Collections.unmodifiableList(nodes);
    }

}
