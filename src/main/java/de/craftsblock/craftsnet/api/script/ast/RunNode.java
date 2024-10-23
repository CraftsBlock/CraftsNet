package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.addon.AddonClassLoader;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.CNetScript;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a 'run' statement node in the Abstract Syntax Tree (AST) of the CNet scripting language.
 * <p>
 * The 'run' statement is used to dynamically execute one or more scripts or classes within the context of the script.
 * It resolves class names and ensures that they implement {@link CNetScript}, then invokes the {@link CNetScript#execute(Exchange)} method
 * on them, passing the current {@link Exchange} context.
 * </p>
 * <p>
 * The node stores multiple targets, which represent the scripts or classes to be run, and handles
 * reflection to load and execute the corresponding methods.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see ASTNode
 * @see CNetScript
 * @see CNetInterpreter
 * @see CNetParser
 * @see VariableNode
 * @since 3.0.7-SNAPSHOT
 */
public class RunNode extends ASTNode {

    private final List<String> targets = new ArrayList<>();

    /**
     * Constructs a {@link RunNode} with the specified line number.
     *
     * @param line the line number in the source code where the 'run' statement appears
     */
    public RunNode(int line) {
        super(line);
    }

    /**
     * Parses the 'run' statement and extracts the target classes or scripts.
     * <p>
     * This method consumes a list of identifiers or pointers from the {@link CNetParser},
     * separated by commas, and adds them to the {@code targets} list.
     * </p>
     *
     * @param parser the {@link CNetParser} used to extract the target class names or scripts
     */
    @Override
    public void parse(CNetParser parser) {
        do {
            if (parser.canConsume(CNetTokenType.SEPARATION)) parser.consume(CNetTokenType.SEPARATION);
            targets.add(parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value());
        }
        while (parser.canConsume(CNetTokenType.SEPARATION));
    }

    /**
     * Interprets the 'run' statement and executes the target scripts or classes.
     * <p>
     * This method:
     * <ul>
     *     <li>Resolves the target class or script using {@link VariableNode#buildTarget}.</li>
     *     <li>Attempts to load the class via reflection, first checking the standard class loader,
     *         then trying addon class loaders via {@link AddonClassLoader}.</li>
     *     <li>Verifies that the class implements {@link CNetScript} and invokes its {@code execute} method.</li>
     * </ul>
     * If the class cannot be found or does not conform to the expected {@code CNetScript} interface,
     * a runtime exception is thrown.
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} used to resolve variables and target paths
     * @param exchange    the {@link Exchange} context passed to the script's {@code execute} method
     * @return true if the execution is successful, false otherwise
     * @throws RuntimeException if the class cannot be found, or if the reflection process encounters errors
     */
    @Override
    public boolean interpret(CNetInterpreter interpreter, Exchange exchange) {
        for (String value : this.getTargets()) {
            String rawTarget = VariableNode.buildTarget(interpreter, value, true);

            try {
                String name = rawTarget.startsWith(".") ? rawTarget.substring(1) : rawTarget;
                Class<?> type = null;

                try {
                    type = Class.forName(name);
                } catch (ClassNotFoundException ignored) {
                }

                if (type == null)
                    for (ClassLoader loader : AddonClassLoader.getAddonLoaders()) {
                        try {
                            type = loader.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            continue;
                        }
                        break;
                    }

                if (type == null)
                    throw new RuntimeException("The class " + name + " could not be found!");

                if (!CNetScript.class.isAssignableFrom(type))
                    throw new RuntimeException("The script (" + type.getName() + ") you tried to run is not an instance of " + CNetScript.class.getName() + "!");

                Object instance = type.getDeclaredConstructor().newInstance();
                Method method = type.getDeclaredMethod("execute", Exchange.class);

                return !((boolean) method.invoke(instance, exchange));
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * Returns the list of target class names or scripts defined by this node.
     *
     * @return a list of target names as {@link String}s
     */
    public List<String> getTargets() {
        return targets;
    }

}
