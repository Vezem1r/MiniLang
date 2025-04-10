package org.example;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage: java Main <file>");
            System.exit(1);
        }

        try {
            String inputFile = args[0];
            String outputFile = args.length > 1 ? args[1] : inputFile + ".code";
            String input = new String(Files.readAllBytes(Paths.get(inputFile)));

            LanguageLexer lexer = new LanguageLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LanguageParser parser = new LanguageParser(tokens);

            parser.removeErrorListeners();
            lexer.removeErrorListeners();

            ErrorHandler errorHandler = new ErrorHandler();
            SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener(errorHandler);
            parser.addErrorListener(syntaxErrorListener);
            lexer.addErrorListener(syntaxErrorListener);

            ParseTree tree = parser.program();

            System.out.println(tree.toStringTree(parser));

            if (errorHandler.hasErrors()) {
                errorHandler.printErrors();
                System.exit(1);
            }

            TypeChecker typeChecker = new TypeChecker(errorHandler);
            typeChecker.visit(tree);

            if (errorHandler.hasErrors()) {
                errorHandler.printErrors();
                System.exit(1);
            }

            CodeGenerator codeGenerator = new CodeGenerator(typeChecker.getTypeRegistry());
            codeGenerator.visit(tree);
            String generatedCode = codeGenerator.getCode();

            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(generatedCode);
            }

            System.out.println("Parsing, type checking, and code generation completed successfully.");
            System.out.println("Generated code saved to: " + outputFile);

        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            System.exit(1);
        }
    }
}