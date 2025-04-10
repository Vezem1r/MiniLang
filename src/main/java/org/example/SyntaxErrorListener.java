package org.example;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class SyntaxErrorListener extends BaseErrorListener {
    private final ErrorHandler errorHandler;

    public SyntaxErrorListener(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        if (offendingSymbol != null) {
            Token token = (Token) offendingSymbol;
            errorHandler.reportSyntaxError(token, msg);
        } else {
            String error = String.format("Syntax error at line %d:%d - %s",
                    line, charPositionInLine, msg);
            System.err.println(error);
        }
    }
}
