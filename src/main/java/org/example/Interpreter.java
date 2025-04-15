package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Interpreter {
    private final Map<String, Value> memory = new HashMap<>();
    private final Deque<Value> stack = new ArrayDeque<>();
    private final Map<String, BufferedWriter> fileHandles = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();

    public void execute(String bytecodeFile) throws IOException {
        List<String> instructions = Files.readAllLines(Paths.get(bytecodeFile));

        for (int i = 0; i < instructions.size(); i++) {
            String instruction = instructions.get(i).trim();
            if (instruction.startsWith("label ")) {
                String labelName = instruction.substring(6).trim();
                labels.put(labelName, i);
            }
        }

        for (int i = 0; i < instructions.size(); i++) {
            String instruction = instructions.get(i).trim();
            if (instruction.isEmpty()) continue;

            try {
                i = executeInstruction(instruction, i);
            } catch (Exception e) {
                System.err.println("Error executing instruction: " + instruction);
                System.err.println("Line " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        for (BufferedWriter writer : fileHandles.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private int executeInstruction(String instruction, int currentLine) throws IOException {
        String[] parts = instruction.split("\\s+", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "push" -> {
                String[] pushParts = args.split(" ", 2);
                String type = pushParts[0];
                String valueStr = pushParts.length > 1 ? pushParts[1] : "";

                switch (type) {
                    case "I" -> stack.push(new Value(Type.INT, Integer.parseInt(valueStr)));
                    case "F" -> stack.push(new Value(Type.FLOAT, Float.parseFloat(valueStr)));
                    case "B" -> stack.push(new Value(Type.BOOL, Boolean.parseBoolean(valueStr)));
                    case "S" -> {
                        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                            valueStr = valueStr.substring(1, valueStr.length() - 1);
                        }
                        stack.push(new Value(Type.STRING, valueStr));
                    }
                    default -> throw new IllegalArgumentException("Unknown type: " + type);
                }
            }
            case "pop" -> stack.pop();
            case "load" -> {
                if (!memory.containsKey(args)) {
                    throw new IllegalArgumentException("Undefined variable: " + args);
                }
                stack.push(memory.get(args));
            }
            case "save" -> {
                if (stack.isEmpty()) {
                    throw new IllegalStateException("Stack is empty");
                }
                memory.put(args, stack.peek());
            }
            case "add" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                if ("I".equals(type)) {
                    stack.push(new Value(Type.INT, ((Number) a.value).intValue() + ((Number) b.value).intValue()));
                } else if ("F".equals(type)) {
                    stack.push(new Value(Type.FLOAT, ((Number) a.value).floatValue() + ((Number) b.value).floatValue()));
                } else {
                    throw new IllegalArgumentException("Unsupported type for add: " + type);
                }
            }
            case "sub" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                if ("I".equals(type)) {
                    stack.push(new Value(Type.INT, ((Number) a.value).intValue() - ((Number) b.value).intValue()));
                } else if ("F".equals(type)) {
                    stack.push(new Value(Type.FLOAT, ((Number) a.value).floatValue() - ((Number) b.value).floatValue()));
                } else {
                    throw new IllegalArgumentException("Unsupported type for sub: " + type);
                }
            }
            case "mul" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                if ("I".equals(type)) {
                    stack.push(new Value(Type.INT, ((Number) a.value).intValue() * ((Number) b.value).intValue()));
                } else if ("F".equals(type)) {
                    stack.push(new Value(Type.FLOAT, ((Number) a.value).floatValue() * ((Number) b.value).floatValue()));
                } else {
                    throw new IllegalArgumentException("Unsupported type for mul: " + type);
                }
            }
            case "div" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                if (((Number) b.value).doubleValue() == 0) {
                    throw new ArithmeticException("Division by zero");
                }

                if ("I".equals(type)) {
                    stack.push(new Value(Type.INT, ((Number) a.value).intValue() / ((Number) b.value).intValue()));
                } else if ("F".equals(type)) {
                    stack.push(new Value(Type.FLOAT, ((Number) a.value).floatValue() / ((Number) b.value).floatValue()));
                } else {
                    throw new IllegalArgumentException("Unsupported type for div: " + type);
                }
            }
            case "mod" -> {
                Value b = stack.pop();
                Value a = stack.pop();

                if (((Number) b.value).intValue() == 0) {
                    throw new ArithmeticException("Modulo by zero");
                }

                stack.push(new Value(Type.INT, ((Number) a.value).intValue() % ((Number) b.value).intValue()));
            }
            case "uminus" -> {
                String type = args.trim();
                Value a = stack.pop();

                if ("I".equals(type)) {
                    stack.push(new Value(Type.INT, -((Number) a.value).intValue()));
                } else if ("F".equals(type)) {
                    stack.push(new Value(Type.FLOAT, -((Number) a.value).floatValue()));
                } else {
                    throw new IllegalArgumentException("Unsupported type for uminus: " + type);
                }
            }
            case "not" -> {
                Value a = stack.pop();
                if (a.type != Type.BOOL) {
                    throw new IllegalArgumentException("Expected boolean for 'not' operation");
                }
                stack.push(new Value(Type.BOOL, !(Boolean) a.value));
            }
            case "or" -> {
                Value b = stack.pop();
                Value a = stack.pop();
                if (a.type != Type.BOOL || b.type != Type.BOOL) {
                    throw new IllegalArgumentException("Expected boolean for 'or' operation");
                }
                stack.push(new Value(Type.BOOL, (Boolean) a.value || (Boolean) b.value));
            }
            case "and" -> {
                Value b = stack.pop();
                Value a = stack.pop();
                if (a.type != Type.BOOL || b.type != Type.BOOL) {
                    throw new IllegalArgumentException("Expected boolean for 'and' operation");
                }
                stack.push(new Value(Type.BOOL, (Boolean) a.value && (Boolean) b.value));
            }
            case "eq" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                boolean result = switch (type) {
                    case "I" -> ((Number) a.value).intValue() == ((Number) b.value).intValue();
                    case "F" -> ((Number) a.value).floatValue() == ((Number) b.value).floatValue();
                    case "S" -> a.value.toString().equals(b.value.toString());
                    default -> throw new IllegalArgumentException("Unsupported type for eq: " + type);
                };

                stack.push(new Value(Type.BOOL, result));
            }
            case "lt" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                boolean result = switch (type) {
                    case "I" -> ((Number) a.value).intValue() < ((Number) b.value).intValue();
                    case "F" -> ((Number) a.value).floatValue() < ((Number) b.value).floatValue();
                    case "S" -> a.value.toString().compareTo(b.value.toString()) < 0;
                    default -> throw new IllegalArgumentException("Unsupported type for lt: " + type);
                };

                stack.push(new Value(Type.BOOL, result));
            }
            case "gt" -> {
                String type = args.trim();
                Value b = stack.pop();
                Value a = stack.pop();

                boolean result = switch (type) {
                    case "I" -> ((Number) a.value).intValue() > ((Number) b.value).intValue();
                    case "F" -> ((Number) a.value).floatValue() > ((Number) b.value).floatValue();
                    case "S" -> a.value.toString().compareTo(b.value.toString()) > 0;
                    default -> throw new IllegalArgumentException("Unsupported type for gt: " + type);
                };

                stack.push(new Value(Type.BOOL, result));
            }
            case "concat" -> {
                Value b = stack.pop();
                Value a = stack.pop();
                stack.push(new Value(Type.STRING, a.value.toString() + b.value.toString()));
            }
            case "itof" -> {
                Value a = stack.pop();
                if (a.type != Type.INT) {
                    throw new IllegalArgumentException("Expected int for 'itof' operation");
                }
                stack.push(new Value(Type.FLOAT, ((Number) a.value).floatValue()));
            }
            case "print" -> {
                int count = Integer.parseInt(args);
                Object[] values = new Object[count];

                for (int i = count - 1; i >= 0; i--) {
                    values[i] = stack.pop().value;
                }

                for (Object value : values) {
                    System.out.print(value);
                }
                System.out.println();
            }
            case "read" -> {
                String type = args.trim();
                Scanner scanner = new Scanner(System.in);

                Value value;
                switch (type) {
                    case "I" -> {
                        System.out.print("Enter an integer: ");
                        value = new Value(Type.INT, scanner.nextInt());
                    }
                    case "F" -> {
                        System.out.print("Enter a float: ");
                        value = new Value(Type.FLOAT, scanner.nextFloat());
                    }
                    case "B" -> {
                        System.out.print("Enter a boolean (true/false): ");
                        value = new Value(Type.BOOL, scanner.nextBoolean());
                    }
                    case "S" -> {
                        System.out.print("Enter a string: ");
                        if (scanner.hasNextLine()) {
                            scanner.nextLine();
                        }
                        value = new Value(Type.STRING, scanner.nextLine());
                    }
                    default -> throw new IllegalArgumentException("Unknown type for read: " + type);
                }

                stack.push(value);
            }
            case "label" -> {
                // Labels are processed in the first pass
            }
            case "jmp" -> {
                String labelName = args.trim();
                if (!labels.containsKey(labelName)) {
                    throw new IllegalArgumentException("Undefined label: " + labelName);
                }
                return labels.get(labelName) - 1;
            }
            case "fjmp" -> {
                String labelName = args.trim();
                if (!labels.containsKey(labelName)) {
                    throw new IllegalArgumentException("Undefined label: " + labelName);
                }

                Value condition = stack.pop();
                if (condition.type != Type.BOOL) {
                    throw new IllegalArgumentException("Expected boolean for 'fjmp' operation");
                }

                if (!(Boolean) condition.value) {
                    return labels.get(labelName) - 1;
                }
            }
            case "swap" -> {
                if (stack.size() < 2) {
                    throw new IllegalStateException("Need at least two values on stack for swap");
                }
                Value a = stack.pop();
                Value b = stack.pop();
                stack.push(a);
                stack.push(b);
            }
            case "fopen" -> {
                Value filename = stack.pop();
                try {
                    String fileId = UUID.randomUUID().toString();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filename.value.toString()));
                    fileHandles.put(fileId, writer);
                    stack.push(new Value(Type.FILE, fileId));
                } catch (IOException e) {
                    throw new IOException("Failed to open file: " + filename.value + " - " + e.getMessage());
                }
            }
            case "fwrite" -> {
                int count = Integer.parseInt(args);
                if (count < 1) {
                    throw new IllegalArgumentException("fwrite needs at least 1 argument (file handle)");
                }

                Object[] values = new Object[count];
                for (int i = count - 1; i >= 0; i--) {
                    values[i] = stack.pop().value;
                }

                String fileId = values[0].toString();
                BufferedWriter writer = fileHandles.get(fileId);
                if (writer == null) {
                    throw new IllegalArgumentException("Invalid file handle: " + fileId);
                }

                for (int i = 1; i < count; i++) {
                    writer.write(values[i].toString());
                }
                writer.flush();
            }
            default -> throw new IllegalArgumentException("Unknown instruction: " + cmd);
        }

        return currentLine;
    }

    private record Value(Type type, Object value) {

        @Override
            public String toString() {
                return value.toString();
            }
        }

    private enum Type {
        INT, FLOAT, BOOL, STRING, FILE
    }
}