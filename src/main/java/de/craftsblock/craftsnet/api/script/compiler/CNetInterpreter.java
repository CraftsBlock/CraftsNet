package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.ast.ASTNode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The interpreter for the CNet scripting language.
 * It executes a list of AST nodes, managing state through a storage map.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class CNetInterpreter {

    /**
     * The current version of the CNetInterpreter.
     */
    public static final String VERSION = "1";

    private final ConcurrentHashMap<Class<? extends ASTNode>, Object> storage = new ConcurrentHashMap<>();

    /**
     * Resets the interpreter by clearing its storage.
     */
    public void reset() {
        this.storage.clear();
    }

    /**
     * Interprets a list of AST nodes within the context of an exchange.
     *
     * @param nodes    the list of AST nodes to interpret
     * @param exchange the exchange context in which to interpret the nodes
     */
    public void interpret(List<ASTNode> nodes, Exchange exchange) {
        for (ASTNode node : nodes)
            if (!node.interpret(this, exchange))
                break;
    }

    /**
     * Retrieves an object from the storage associated with a specific AST node class.
     *
     * @param <T>  the type of the object to retrieve
     * @param node the class of the AST node
     * @return the object associated with the given AST node class, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromStorage(Class<? extends ASTNode> node) {
        if (!this.isInStorage(node)) return null;
        return (T) this.storage.get(node);
    }

    /**
     * Checks if there is an object in the storage associated with a specific AST node class.
     *
     * @param node the class of the AST node
     * @return true if an object is associated with the given AST node class, false otherwise
     */
    public boolean isInStorage(Class<? extends ASTNode> node) {
        return this.storage.containsKey(node);
    }

    /**
     * Puts an object into the storage associated with a specific AST node class.
     *
     * @param node  the class of the AST node
     * @param value the object to store
     */
    public void putToStorage(Class<? extends ASTNode> node, Object value) {
        this.storage.put(node, value);
    }

}
