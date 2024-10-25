package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftsnet.api.script.tokens.CNetToken;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final String VERSION = "1";
    private static final String[] COMMENT_GROUPS = new String[]{"single", "block", "line"};

    private final HashMap<Integer, Integer> skipLinesOnIndex = new HashMap<>();

    /**
     * Tokenizes the given script into a list of {@link CNetToken} objects.
     *
     * @param script the script to tokenize
     * @return a list of tokens representing the script
     */
    public List<CNetToken> tokenize(int start, String script) {
        int line = start;
        Pattern codeCheck = Pattern.compile("(?<single>//[^\\n]*\\n?)|(?<block>/\\*.*\\*/)|(?<line>#[^\\n]*\\n?)|(?<code>(?:[^/#]|/(?![*/])|#(?!.*))+.*?)", Pattern.DOTALL);
        Matcher matcher = codeCheck.matcher(script);

        StringBuilder sanitized = new StringBuilder();
        check:
        while (matcher.find()) {
            for (String group : COMMENT_GROUPS) {
                String comment = matcher.group(group);
                if (comment != null) {
                    int counted = CNetCompiler.countLines(comment);
                    skipLinesOnIndex.put(line, counted);
                    line += counted;
                    continue check;
                }
            }

            String code = matcher.group("code");
            if (code == null) continue;
            sanitized.append(code);
            line += CNetCompiler.countLines(code);
        }

        line = start;

        List<CNetToken> tokens = new ArrayList<>();
        matcher.usePattern(CNetTokenType.lexerPattern);
        matcher.reset(sanitized.toString());

        lexer:
        while (matcher.find()) {
            for (CNetTokenType type : CNetTokenType.lexerAbleTypes) {
                String value = matcher.group(type.getGroupName());
                if (value == null) continue;

                line += skipLinesOnIndex.getOrDefault(line, 0);

                if (type.equals(CNetTokenType.LINE_BREAK)) {
                    line++;
                    continue;
                }

                tokens.add(new CNetToken(line, type, value));
                continue lexer;
            }
        }

        tokens.add(new CNetToken(line, CNetTokenType.EOF, ""));
        return tokens;
    }

}
