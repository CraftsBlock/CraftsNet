package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

/**
 * Represents a 'package' statement node in the Abstract Syntax Tree (AST) of the CNet scripting language.
 * <p>
 * The 'package' statement is used to define or set the current package or namespace in which the script operates.
 * </p>
 * The {@link PackageNode} extracts the package or target namespace during parsing, and during interpretation,
 * it sets the package into the interpreter's storage for later use.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see ASTNode
 * @see CNetInterpreter
 * @see CNetParser
 * @see VariableNode
 * @since 3.0.7-SNAPSHOT
 */
public class PackageNode extends ASTNode {

    private String target;

    /**
     * Constructs a {@link PackageNode} with the specified line number.
     *
     * @param line the line number in the source code where the 'package' statement appears
     */
    public PackageNode(int line) {
        super(line);
    }

    /**
     * Parses the 'package' statement and extracts the target package or pointer.
     * <p>
     * This method consumes an identifier or pointer token from the {@link CNetParser}
     * and assigns it to the {@code target} field, which represents the package or namespace.
     * </p>
     *
     * @param parser the {@link CNetParser} used to extract the package name or pointer
     */
    @Override
    public void parse(CNetParser parser) {
        target = parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value();
    }

    /**
     * Interprets the 'package' statement and sets the package name in the interpreter's storage.
     * <p>
     * This method:
     * <ul>
     *     <li>Resolves the target package using {@link VariableNode#buildTarget}.</li>
     *     <li>Stores the resolved package name in the interpreter for use by other nodes during script execution.</li>
     *     <li>If the target is ".", an empty string is stored instead, indicating the root package.</li>
     * </ul>
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} used to store the current package
     * @param exchange    the {@link Exchange} context (not used in this specific node but required for the signature)
     * @return true always, indicating successful interpretation of the 'package' statement
     */
    @Override
    public boolean interpret(CNetInterpreter interpreter, Exchange exchange) {
        String target = VariableNode.buildTarget(interpreter, this.getTarget(), false);
        interpreter.putToStorage(getClass(), target.equals(".") ? "" : target);
        return true;
    }

    /**
     * Returns the target package or namespace defined by this node.
     *
     * @return the target package as a {@link String}
     */
    public String getTarget() {
        return target;
    }

    /**
     * Retrieves the current package stored in the interpreter's storage.
     * <p>
     * This method checks the interpreter's storage to see if a package has been defined.
     * If none exists, it returns an empty string, representing the root package.
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} from which to retrieve the current package
     * @return the current package name or an empty string if none is set
     */
    protected static String getCurrentPackage(CNetInterpreter interpreter) {
        if (!interpreter.isInStorage(PackageNode.class)) return "";
        return interpreter.getFromStorage(PackageNode.class);
    }

}
