package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a variable declaration and definition node in the AST of the CNet scripting language.
 * This node is responsible for storing and managing variables.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class VariableNode extends ASTNode {

    private String name;
    private String value;

    /**
     * Parses the variable node, extracting the variable name and value.
     *
     * @param parser the parser used to parse the node
     */
    @Override
    public void parse(CNetParser parser) {
        name = parser.consume(CNetTokenType.POINTER).value().substring(1);
        parser.consume(CNetTokenType.DEFINITION);
        value = parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value();
        parser.consume(CNetTokenType.SEMICOLON);
    }

    /**
     * Interprets the variable node, storing the variable in the interpreter's storage.
     *
     * @param interpreter the interpreter used to execute the node
     * @param exchange    the exchange context in which the node is interpreted
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
     * Gets the name of the variable.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value of the variable.
     *
     * @return the variable value
     */
    public String getValue() {
        return value;
    }

    /**
     * Retrieves the variables map from the interpreter's storage, creating it if it does not exist.
     *
     * @param interpreter the interpreter whose storage is queried
     * @return the map of variables
     */
    protected static ConcurrentHashMap<String, String> getVariables(CNetInterpreter interpreter) {
        if (!interpreter.isInStorage(VariableNode.class)) interpreter.putToStorage(VariableNode.class, new ConcurrentHashMap<>());
        return interpreter.getFromStorage(VariableNode.class);
    }

    /**
     * Gets the value of a specific variable from the interpreter's storage.
     *
     * @param interpreter the interpreter whose storage is queried
     * @param name        the name of the variable to retrieve
     * @return the value of the variable
     * @throws RuntimeException if the variable is not found
     */
    protected static String getVariableValue(CNetInterpreter interpreter, String name) {
        ConcurrentHashMap<String, String> variables = getVariables(interpreter);
        if (!variables.containsKey(name))
            throw new RuntimeException("No variable named $" + name + " found!");
        return variables.get(name);
    }

    /**
     * Builds the full target string for a given value, resolving variables and package context as needed.
     *
     * @param interpreter the interpreter used to resolve the target
     * @param value       the value to resolve
     * @param type        whether the value is a type
     * @return the resolved target string
     */
    protected static String buildTarget(CNetInterpreter interpreter, String value, boolean type) {
        if (value.startsWith("$")) return getVariableValue(interpreter, value.substring(1));
        String currentPackage = PackageNode.getCurrentPackage(interpreter);
        if (currentPackage.isEmpty()) return value;

        return value.contains(".") || !type ? value : currentPackage.concat(currentPackage.endsWith(".") ? "" : ".").concat(value);
    }

}
