package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftsnet.api.script.tokens.CNetToken;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The lexer for the CNet scripting language.
 * It converts a script into a list of tokens for further processing by the parser.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class CNetLexer {

    /**
     * The current version of the CNetLexer.
     */
    public static final String VERSION = "0";

    /**
     * Tokenizes the given script into a list of {@link CNetToken} objects.
     *
     * @param script the script to tokenize
     * @return a list of tokens representing the script
     */
    public List<CNetToken> tokenize(String script) {
        List<CNetToken> tokens = new ArrayList<>();
        Matcher matcher = CNetTokenType.lexerPattern.matcher(script);

        lexer:
        while (matcher.find()) {
            for (CNetTokenType type : CNetTokenType.lexerAbleTypes) {
                String value = matcher.group(type.getGroupName());
                if (value == null) continue;

                tokens.add(new CNetToken(type, value));
                continue lexer;
            }
        }
        tokens.add(new CNetToken(CNetTokenType.EOF, ""));

        return tokens;
    }

}
