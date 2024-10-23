package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a variable declaration and assignment node in the Abstract Syntax Tree (AST) of the CNet scripting language.
 * <p>
 * The {@code VariableNode} handles the parsing and interpretation of variable declarations and assignments. It supports
 * resolving variables within the current package context and allows variables to be stored and retrieved dynamically during
 * script execution.
 * </p>
 * <p>
 * This node parses variable declarations using the format {@code $variable = value} and stores them in the interpreter's
 * internal storage. The value can either be a direct identifier or a pointer, with support for package resolution.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see ASTNode
 * @see CNetInterpreter
 * @see CNetParser
 * @see PackageNode
 * @since 3.0.7-SNAPSHOT
 */
public class VariableNode extends ASTNode {

    private String name;
    private String value;

    /**
     * Constructs a {@link VariableNode} with the specified line number.
     *
     * @param line the line number in the source code where the variable declaration appears
     */
    public VariableNode(int line) {
        super(line);
    }

    /**
     * Parses a variable declaration and assignment statement from the given parser.
     * <p>
     * This method consumes tokens representing a variable name (prefixed with '$'), an assignment operator ('='), and a value.
     * The value can be either an identifier or a pointer, and is stored for later interpretation.
     * </p>
     *
     * @param parser the {@link CNetParser} used to parse the variable declaration
     */
    @Override
    public void parse(CNetParser parser) {
        name = parser.consume(CNetTokenType.POINTER).value().substring(1);
        parser.consume(CNetTokenType.DEFINITION);
        value = parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value();
    }

    /**
     * Interprets the variable declaration by storing the variable and its value in the interpreter's storage.
     * <p>
     * This method ensures that if the value does not contain a period ('.'), it is automatically prefixed with the current package
     * context (if applicable). This allows for proper namespacing of variables.
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} used to store the variable and its value
     * @param exchange    the {@link Exchange} context (not used in this specific node but required for the signature)
     * @return true always, indicating successful interpretation of the variable assignment
     */
    @Override
    public boolean interpret(CNetInterpreter interpreter, Exchange exchange) {
        String value = this.getValue();
        getVariables(interpreter).put(
                this.getName(),
                value.contains(".") ? value : PackageNode.getCurrentPackage(interpreter).concat(".").concat(value)
        );
        return true;
    }

    /**
     * Returns the name of the variable (without the leading '$').
     *
     * @return the variable name as a {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value assigned to the variable.
     *
     * @return the variable value as a {@link String}
     */
    public String getValue() {
        return value;
    }

    /**
     * Retrieves the map of all stored variables for the current interpreter.
     * <p>
     * If no variable map exists in the interpreter's storage, a new {@link ConcurrentHashMap} is created and stored.
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} in which the variables are stored
     * @return the map of variables as a {@link ConcurrentHashMap}
     */
    protected static ConcurrentHashMap<String, String> getVariables(CNetInterpreter interpreter) {
        if (!interpreter.isInStorage(VariableNode.class)) interpreter.putToStorage(VariableNode.class, new ConcurrentHashMap<>());
        return interpreter.getFromStorage(VariableNode.class);
    }

    /**
     * Retrieves the value of a variable by its name from the interpreter's storage.
     * <p>
     * If the variable does not exist, a runtime exception is thrown.
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} containing the variable
     * @param name        the name of the variable (without the leading '$')
     * @return the value of the variable as a {@link String}
     * @throws RuntimeException if the variable is not found in the interpreter's storage
     */
    protected static String getVariableValue(CNetInterpreter interpreter, String name) {
        ConcurrentHashMap<String, String> variables = getVariables(interpreter);
        if (!variables.containsKey(name))
            throw new RuntimeException("No variable named $" + name + " found!");
        return variables.get(name);
    }

    /**
     * Builds the target value for a variable, resolving it within the current package context if necessary.
     * <p>
     * If the value starts with a '$', it is resolved as a variable reference. Otherwise, it checks if the value
     * needs to be prefixed with the current package name (if not already fully qualified).
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} to use for resolving variables and package context
     * @param value       the value to resolve (may be a variable reference or a plain value)
     * @param type        a boolean indicating whether the value is a type (for determining if package resolution is required)
     * @return the resolved target value as a {@link String}
     */
    protected static String buildTarget(CNetInterpreter interpreter, String value, boolean type) {
        if (value.startsWith("$")) return getVariableValue(interpreter, value.substring(1));
        String currentPackage = PackageNode.getCurrentPackage(interpreter);
        if (currentPackage.isEmpty()) return value;

        return value.contains(".") || !type ? value : currentPackage.concat(currentPackage.endsWith(".") ? "" : ".").concat(value);
    }

}
