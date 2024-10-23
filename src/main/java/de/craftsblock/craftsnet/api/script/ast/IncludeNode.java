package de.craftsblock.craftsnet.api.script.ast;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.script.compiler.CNetCompiler;
import de.craftsblock.craftsnet.api.script.compiler.CNetInterpreter;
import de.craftsblock.craftsnet.api.script.compiler.CNetParser;
import de.craftsblock.craftsnet.api.script.tokens.CNetTokenType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Represents an 'include' statement node in the Abstract Syntax Tree (AST) of the CNet scripting language.
 * <p>
 * The 'include' statement is responsible for including and interpreting external files
 * during script execution. The {@link IncludeNode} holds a reference to the target file
 * and ensures that it can be compiled and included properly.
 * </p>
 * <p>
 * The parsing phase extracts the file name or pointer from the tokens, and the interpretation
 * phase validates the file's existence and compatibility before including it.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see ASTNode
 * @see CNetCompiler
 * @see CNetParser
 * @see CNetInterpreter
 * @see VariableNode
 * @since 3.0.7-SNAPSHOT
 */
public class IncludeNode extends ASTNode {

    private String target;

    /**
     * Constructs an {@link IncludeNode} with the specified line number.
     *
     * @param line the line number in the source code where the 'include' statement appears
     */
    public IncludeNode(int line) {
        super(line);
    }

    /**
     * Parses the 'include' statement and extracts the target file or pointer.
     * <p>
     * This method consumes an identifier or a pointer token from the {@link CNetParser}
     * and assigns it to the {@code target} field, which represents the file to be included.
     * </p>
     *
     * @param parser the {@link CNetParser} used for extracting the file name or pointer
     */
    @Override
    public void parse(CNetParser parser) {
        target = parser.consume(CNetTokenType.IDENTIFIER, CNetTokenType.POINTER).value();
    }

    /**
     * Interprets the 'include' statement by attempting to include the target file.
     * <p>
     * This method:
     * <ul>
     *     <li>Resolves the target file using the {@link VariableNode#buildTarget} method.</li>
     *     <li>Checks if the target file exists. If not, throws a {@link FileNotFoundException}.</li>
     *     <li>Checks if the target file can be compiled. If not, throws an {@link UnsupportedEncodingException}.</li>
     *     <li>Compiles the target file using the {@link CNetCompiler} and returns the compilation result.</li>
     * </ul>
     * </p>
     *
     * @param interpreter the {@link CNetInterpreter} used for resolving the target file path
     * @param exchange    the {@link Exchange} context in which the included file is interpreted
     * @return true if the included file is successfully compiled; false otherwise
     * @throws IOException if an I/O error occurs during file operations, such as file not found or unsupported encoding
     */
    @Override
    public boolean interpret(CNetInterpreter interpreter, Exchange exchange) throws IOException {
        File target = new File(VariableNode.buildTarget(interpreter, this.target, false));
        if (!target.exists())
            throw new FileNotFoundException("The included file (" + target.getAbsolutePath() + ") does not exists!");
        if (!CNetCompiler.canCompile(target))
            throw new UnsupportedEncodingException("The included file (" + target.getAbsolutePath() + ") can not be compiled!");

        return CNetCompiler.compile(target, exchange);
    }

}
