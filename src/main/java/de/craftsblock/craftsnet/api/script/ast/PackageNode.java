package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

/**
 * Represents a package declaration node in the AST of the CNet scripting language.
 * This node sets the current package context for subsequent operations.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class PackageNode extends ASTNode {

    private String target;

    /**
     * Parses the package node, extracting the target package identifier or pointer.
     *
     * @param parser the parser used to parse the node
     */
    @Override
    public void parse(CNetParser parser) {
        target = parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value();
        parser.consume(CNetTokenType.SEMICOLON);
    }

    /**
     * Interprets the package node, setting the target package in the interpreter's storage.
     *
     * @param interpreter the interpreter used to execute the node
     * @param exchange    the exchange context in which the node is interpreted
     */
    @Override
    public void interpret(CNetInterpreter interpreter, Exchange exchange) {
        String target = VariableNode.buildTarget(interpreter, this.getTarget(), false);
        interpreter.putToStorage(getClass(), target.equals(".") ? "" : target);
    }

    /**
     * Gets the target package identifier or pointer.
     *
     * @return the target package
     */
    public String getTarget() {
        return target;
    }

    /**
     * Retrieves the current package context from the interpreter's storage.
     *
     * @param interpreter the interpreter whose storage is queried
     * @return the current package, or an empty string if no package is set
     */
    protected static String getCurrentPackage(CNetInterpreter interpreter) {
        if (!interpreter.isInStorage(PackageNode.class)) return "";
        return interpreter.getFromStorage(PackageNode.class);
    }

}
