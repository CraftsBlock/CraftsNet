package de.craftsblock.craftsnet.api.script.tokens;

import de.craftsblock.craftsnet.api.script.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enumeration representing the different types of tokens in the CNet scripting language.
 * Each token type is associated with a regex pattern and optionally a corresponding AST node class.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public enum CNetTokenType {

    PACKAGE("package", "package", PackageNode.class),
    RUN("run", "run", RunNode.class),
    EXEC("exec", "exec", RunNode.class),
    VARIABLE("var", "var", VariableNode.class),
    IMPORT("include", "include", IncludeNode.class),
    DEFINITION("def", "=", null),
    POINTER("pointer", "\\$[a-zA-Z0-9_.]+", null),
    IDENTIFIER("identifier", "[a-zA-Z0-9_$./]+", null),
    SEPARATION("sep", ",", null),
    SEMICOLON("semicolon", ";", null),
    LINE_BREAK("linebreak", "\n", null),
    EOF(null, null, null);

    /**
     * The current version of the CNetTokenType.
     */
    public static final String VERSION = "6";

    /**
     * List of token types that can be recognized by the lexer.
     */
    public static final List<CNetTokenType> lexerAbleTypes;
    /**
     * Regex pattern compiled from all lexable token types.
     */
    public static final Pattern lexerPattern;

    /**
     * List of token types that can be parsed into AST nodes.
     */
    public static final List<CNetTokenType> parsableTypes;

    static {
        List<CNetTokenType> parsable = new ArrayList<>();
        List<CNetTokenType> types = new ArrayList<>();

        // Populate lists for lexer and parser
        for (CNetTokenType type : CNetTokenType.values()) {
            if (type.getGroupName() != null && type.getPattern() != null)
                types.add(type);

            if (type.isNode())
                parsable.add(type);
        }

        lexerPattern = Pattern.compile(String.join("|", types.stream().map(CNetTokenType::getPattern).toList()));
        lexerAbleTypes = Collections.unmodifiableList(types);
        parsableTypes = Collections.unmodifiableList(parsable);
    }

    private final String groupName;
    private final String rawPattern;
    private final String pattern;
    private final Class<? extends ASTNode> node;

    /**
     * Constructs a CNetTokenType.
     *
     * @param groupName the name of the group for the regex pattern
     * @param pattern   the regex pattern to recognize this token type
     * @param node      the AST node class associated with this token type
     */
    CNetTokenType(String groupName, String pattern, Class<? extends ASTNode> node) {
        this.groupName = groupName;
        this.rawPattern = pattern;
        this.pattern = "(?<" + groupName + ">" + pattern + ")";
        this.node = node;
    }

    /**
     * Gets the group name of this token type.
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Gets the raw regex pattern of this token type.
     *
     * @return the raw pattern
     */
    public String getRawPattern() {
        return rawPattern;
    }

    /**
     * Gets the full regex pattern of this token type.
     *
     * @return the full pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Checks if this token type is associated with an AST node.
     *
     * @return true if associated with an AST node, false otherwise
     */
    public boolean isNode() {
        return node != null;
    }

    /**
     * Gets the AST node class associated with this token type.
     *
     * @return the AST node class
     */
    public Class<? extends ASTNode> getNode() {
        return node;
    }

    /**
     * Creates a new instance of the AST node associated with this token type.
     *
     * @return a new instance of the associated AST node
     * @throws RuntimeException if an error occurs during instantiation
     */
    public ASTNode createNode(int line) {
        try {
            return node.getDeclaredConstructor(int.class).newInstance(line);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
