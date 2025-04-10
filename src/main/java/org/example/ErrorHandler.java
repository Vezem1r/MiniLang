package org.example;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private final List<String> errors = new ArrayList<>();

    public void reportSyntaxError(Token token, String message) {
        String error = String.format("Syntax error at line %d:%d - %s",
                token.getLine(), token.getCharPositionInLine(), message);
        errors.add(error);
    }

    public void reportTypeError(Token token, String message) {
        String error = String.format("Type error at line %d:%d - %s",
                token.getLine(), token.getCharPositionInLine(), message);
        errors.add(error);
    }

    public void reportVariableError(Token token, String message) {
        String error = String.format("Variable error at line %d:%d - %s",
                token.getLine(), token.getCharPositionInLine(), message);
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String error : errors) {
            System.err.println(error);
        }
    }
}
