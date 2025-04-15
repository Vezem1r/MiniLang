# PLC Interpreter

This project is a stack-based programming language interpreter built using ANTLR. It supports parsing, type checking, code generation, and execution of a custom language.


Make sure you have **Java 17+** and **Maven** installed.

### To download the project:

```bash
git clone https://github.com/Vezem1r/MiniLang.git
```

### To build the project:

```bash
mvn clean package
```

This will generate a JAR file with all dependencies included.

### To run the interpreter on an example input file: 

```bash
java -jar target/language-parser-1.0-SNAPSHOT-jar-with-dependencies.jar src/main/resources/input1.txt --run
```

or to run only TypeCheck and CodeGenerator
```bash
java -jar target/language-parser-1.0-SNAPSHOT-jar-with-dependencies.jar src/main/resources/input1.txt
```


