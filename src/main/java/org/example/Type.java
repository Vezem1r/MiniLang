package org.example;

public class Type {
    public static final Type INT = new Type("int");
    public static final Type FLOAT = new Type("float");
    public static final Type BOOL = new Type("bool");
    public static final Type STRING = new Type("string");
    public static final Type FILE = new Type("file");
    public static final Type ERROR = new Type("error");

    private final String name;

    private Type(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Type fromString(String typeName) {
        return switch (typeName) {
            case "int" -> INT;
            case "float" -> FLOAT;
            case "bool" -> BOOL;
            case "string" -> STRING;
            case "file" -> FILE;
            default -> ERROR;
        };
    }

    // Can convert from int to float only
    public static boolean isConvertible(Type from, Type to) {
        return (from == INT && to == FLOAT) ||
                from == to;
    }

    public static Type resultType(Type left, Type right, String operator) {

        if (left == ERROR || right == ERROR) {
            return ERROR;
        }

        if ((left == INT && right == FLOAT) || (left == FLOAT && right == INT)) {
            if ("+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator)) {
                return FLOAT;
            }

            if ("<".equals(operator) || ">".equals(operator)) {
                return BOOL;
            }
        }

        if ((left == INT && right == INT) &&
                ("+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator))) {
            return INT;
        }

        if ((left == INT && right == INT) &&
                ("<".equals(operator) || ">".equals(operator))) {
            return BOOL;
        }

        if ((left == FLOAT && right == FLOAT) &&
                ("+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator))) {
            return FLOAT;
        }

        if ((left == FLOAT && right == FLOAT) &&
                ("<".equals(operator) || ">".equals(operator))) {
            return BOOL;
        }

        if (left == INT && right == INT && "%".equals(operator)) {
            return INT;
        }

        if (left == STRING && right == STRING && ".".equals(operator)) {
            return STRING;
        }

        if (left == STRING && right == STRING &&
                ("<".equals(operator) || ">".equals(operator))) {
            return BOOL;
        }

        if ((left == right) && ("==".equals(operator) || "!=".equals(operator)) &&
                (left == INT || left == FLOAT || left == STRING)) {
            return BOOL;
        }

        if (left == BOOL && right == BOOL &&
                ("&&".equals(operator) || "||".equals(operator))) {
            return BOOL;
        }

        return ERROR;
    }

    public static Type resultTypeUnary(Type operand, String operator) {
        if (operand == ERROR) {
            return ERROR;
        }

        if ("-".equals(operator) && (operand == INT) || operand == FLOAT) {
            return operand;
        }

        if ("!".equals(operator) && operand == BOOL) {
            return BOOL;
        }

        return ERROR;
    }

    @Override
    public String toString() {
        return name;
    }
}
