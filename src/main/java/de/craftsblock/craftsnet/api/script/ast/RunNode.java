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
 * Represents a run node in the AST of the CNet scripting language.
 * This node is responsible for executing specified targets.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class RunNode extends ASTNode {

    private final List<String> targets = new ArrayList<>();

    /**
     * Parses the run node, extracting the list of target identifiers or pointers.
     *
     * @param parser the parser used to parse the node
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
     * Interprets the run node, executing each target in the context of an exchange.
     *
     * @param interpreter the interpreter used to execute the node
     * @param exchange    the exchange context in which the node is interpreted
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
     * Gets the list of target identifiers or pointers.
     *
     * @return the list of targets
     */
    public List<String> getTargets() {
        return targets;
    }

}
