package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;

/**
 * Abstract base class representing a node in the Abstract Syntax Tree (AST) of the CNet scripting language.
 * Each node must implement methods for parsing and interpreting the node.
 */
public abstract class ASTNode {

    /**
     * Parses the node using the provided parser.
     * This method is responsible for constructing the AST node from the parsed tokens.
     *
     * @param parser the parser to use for parsing this node
     */
    public abstract void parse(CNetParser parser);

    /**
     * Interprets the node using the provided interpreter and exchange.
     * This method executes the logic represented by the AST node.
     *
     * @param interpreter the interpreter to use for interpreting this node
     * @param exchange the exchange context in which this node is interpreted
     */
    public abstract boolean interpret(CNetInterpreter interpreter, Exchange exchange) throws Exception;

}
