package de.craftsblock.craftsnet.api.script.compiler;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.ast.ASTNode;
import de.craftsblock.craftsnet.api.script.tokens.CNetToken;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The CNetCompiler class is responsible for compiling CNetScript source code.
 * It performs lexical analysis, parsing, and interpretation of the script.
 */
public class CNetCompiler {

    /**
     * The version string of the CNetCompiler, composed of versions from CNetInterpreter, CNetParser, CNetLexer,
     * and CNetTokenType.
     */
    public static final String VERSION = String.join(".", List.of(CNetInterpreter.VERSION, CNetParser.VERSION, CNetLexer.VERSION)) + "-" + CNetTokenType.VERSION;
    private static final Pattern extractor = Pattern.compile("(?<leading>\\s*\\n\\s*)?(?<cnetscript><\\?cnetscript(?<script>[^?]*)(?:\\?>)?)(?<trailing>\\s*\\n\\s*)?|(?<remaining>[^<]+|<(?!=\\?cnetscript))");

    /**
     * Checks if the given file can be compiled.
     *
     * @param input the input file to check
     * @return true if compilation is possible, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean canCompile(File input) throws IOException {
        byte[] data;
        try (FileInputStream in = new FileInputStream(input)) {
            data = in.readAllBytes();
            if (!Utils.isEncodingValid(data, StandardCharsets.UTF_8)) return false;
        }
        return canCompile(new String(data, StandardCharsets.UTF_8));
    }

    /**
     * Checks if the given string can be compiled.
     *
     * @param input the input string to check
     * @return true if compilation is possible, false otherwise
     */
    public static boolean canCompile(String input) {
        return extractor.matcher(input).find();
    }

    /**
     * Compiles the script from the given file and executes it in the provided exchange context.
     *
     * @param input    the input file containing the script to compile
     * @param exchange the exchange context in which the script is executed
     * @throws IOException if an I/O error occurs
     */
    public static void compile(File input, Exchange exchange) throws IOException {
        try {
            byte[] data;
            try (FileInputStream in = new FileInputStream(input)) {
                data = in.readAllBytes();
                if (!Utils.isEncodingValid(data, StandardCharsets.UTF_8))
                    throw new UnsupportedEncodingException("The file must not contain none utf8 chars! (At least one byte was negativ!)");
            }
            compile(new String(data, StandardCharsets.UTF_8), exchange);
        } catch (RuntimeException e) {
            throw new RuntimeException("Unable to finish compilation of file " + input.getAbsolutePath(), e);
        }
    }

    /**
     * Compiles the script from the given string and executes it in the provided exchange context.
     *
     * @param input    the input string containing the script to compile
     * @param exchange the exchange context in which the script is executed
     */
    public static void compile(String input, Exchange exchange) {
        Matcher matcher = extractor.matcher(input);

        CNetLexer lexer = new CNetLexer();
        CNetInterpreter interpreter = new CNetInterpreter();
        int lines = 1;
        while (matcher.find()) {
            String remaining = matcher.group("remaining");
            if (remaining != null) {
                lines += countLines(remaining);
                try {
                    exchange.response().print(remaining);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            String script = matcher.group("script");
            if (script == null) continue;

            if (matcher.group("leading") != null)
                lines += countLines(matcher.group("leading"));

            Pattern removeComments = Pattern.compile("//.*|/\\*.*\\*/|#.*", Pattern.DOTALL);
            List<CNetToken> tokens = lexer.tokenize(lines, removeComments.matcher(script).replaceAll(""));

            if (matcher.group("trailing") != null)
                lines += countLines(matcher.group("trailing")) - 1;

            CNetParser parser = new CNetParser(tokens);
            List<ASTNode> nodes = parser.parse();

            try {
                if (!interpreter.interpret(nodes, exchange)) break;
            } catch (Exception e) {
                if (e instanceof RuntimeException runtimeException)
                    throw runtimeException;
                throw new RuntimeException(e);
            }
        }
        interpreter.reset();
    }

    /**
     * Counts the number of new lines (\n) in a given string.
     *
     * @param text the text in which to count new lines
     * @return the number of new line characters found in the text
     */
    private static int countLines(String text) {
        int newLines = 0;

        for (char c : text.toCharArray())
            if (c == '\n')
                newLines++;

        return newLines;
    }

}
