package org.example;

import org.antlr.v4.runtime.tree.TerminalNode;

public class CodeGenerator extends LanguageBaseVisitor<Void> {
    private final SymbolTable symbolTable = new SymbolTable();
    private final TypeRegistry typeRegistry;
    private final StringBuilder code = new StringBuilder();
    private int labelCounter = 0;

    public CodeGenerator(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public String getCode() {
        String result = code.toString();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Override
    public Void visitProgram(LanguageParser.ProgramContext ctx) {
        for (LanguageParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Void visitVariableDeclaration(LanguageParser.VariableDeclarationContext ctx) {
        String typeName = ctx.primitiveType().getText();
        Type type = Type.fromString(typeName);

        for (TerminalNode idNode : ctx.ID()) {
            String varName = idNode.getText();

            if (!symbolTable.isDefined(varName)) {
                symbolTable.define(new SymbolTable.Symbol(varName, type));

                switch (type.getName()) {
                    case "int":
                        code.append("push I 0\n");
                        code.append("save ").append(varName).append("\n");
                        break;
                    case "float":
                        code.append("push F 0.0\n");
                        code.append("save ").append(varName).append("\n");
                        break;
                    case "bool":
                        code.append("push B false\n");
                        code.append("save ").append(varName).append("\n");
                        break;
                    case "string", "file":
                        code.append("push S \"\"\n");
                        code.append("save ").append(varName).append("\n");
                        break;
                }
            }
        }
        return null;
    }

    @Override
    public Void visitRead(LanguageParser.ReadContext ctx) {
        for (TerminalNode idNode : ctx.ID()) {
            String varName = idNode.getText();
            Type type = symbolTable.resolve(varName).type();

            String typeCode = switch (type.getName()) {
                case "int" -> "I";
                case "float" -> "F";
                case "bool" -> "B";
                case "string" -> "S";
                default -> "";
            };

            code.append("read ").append(typeCode).append("\n");
            code.append("save ").append(varName).append("\n");
        }
        return null;
    }

    @Override
    public Void visitWrite(LanguageParser.WriteContext ctx) {
        int expressionCount = ctx.expression().size();

        for (LanguageParser.ExpressionContext expr : ctx.expression()) {
            visit(expr);
        }

        if (expressionCount > 0) {
            code.append("print ").append(expressionCount).append("\n");
        }
        return null;
    }

    @Override
    public Void visitFwrite(LanguageParser.FwriteContext ctx) {
        for (int i = 0; i < ctx.expression().size(); i++) {
            visit(ctx.expression(i));
        }

        code.append("fwrite ").append(ctx.expression().size()).append("\n");
        return null;
    }

    @Override
    public Void visitFopen(LanguageParser.FopenContext ctx) {
        if (ctx.expression().size() >= 2) {
            visit(ctx.expression(1));
            code.append("fopen\n");

            if (ctx.expression(0) instanceof LanguageParser.IdValueContext) {
                String varName = ((LanguageParser.IdValueContext) ctx.expression(0)).ID().getText();
                code.append("save ").append(varName).append("\n");
            }
        } else if (ctx.expression().size() == 1) {
            visit(ctx.expression(0));
            code.append("fopen\n");
        }
        return null;
    }

    @Override
    public Void visitBlock(LanguageParser.BlockContext ctx) {
        for (LanguageParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Void visitIf(LanguageParser.IfContext ctx) {
        int elseLabel = labelCounter++;
        int endLabel = labelCounter++;

        visit(ctx.condition());
        code.append("fjmp ").append(elseLabel).append("\n");
        visit(ctx.statement());
        code.append("jmp ").append(endLabel).append("\n");
        code.append("label ").append(elseLabel).append("\n");

        if (ctx.else_() != null) {
            visit(ctx.else_().statement());
        }

        code.append("label ").append(endLabel).append("\n");
        return null;
    }

    @Override
    public Void visitWhile(LanguageParser.WhileContext ctx) {
        int startLabel = labelCounter++;
        int endLabel = labelCounter++;

        code.append("label ").append(startLabel).append("\n");
        visit(ctx.condition());
        code.append("fjmp ").append(endLabel).append("\n");
        visit(ctx.statement());
        code.append("jmp ").append(startLabel).append("\n");
        code.append("label ").append(endLabel).append("\n");
        return null;
    }

    @Override
    public Void visitExpressionEval(LanguageParser.ExpressionEvalContext ctx) {
        visit(ctx.expression());
        code.append("pop\n");
        return null;
    }

    @Override
    public Void visitNop(LanguageParser.NopContext ctx) {
        return null;
    }

    @Override
    public Void visitAssignment(LanguageParser.AssignmentContext ctx) {
        String varName = ctx.ID().getText();
        Type targetType = symbolTable.resolve(varName).type();
        visit(ctx.expression());

        Type expressionType = typeRegistry.getType(ctx.expression());
        if (targetType == Type.FLOAT && expressionType == Type.INT) {
            code.append("itof\n");
        }

        code.append("save ").append(varName).append("\n");
        code.append("load ").append(varName).append("\n");
        return null;
    }

    @Override
    public Void visitOr(LanguageParser.OrContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        code.append("or\n");
        return null;
    }

    @Override
    public Void visitAnd(LanguageParser.AndContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        code.append("and\n");
        return null;
    }

    @Override
    public Void visitComparison(LanguageParser.ComparisonContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));

        String operator = ctx.op.getText();
        Type leftType = typeRegistry.getType(ctx.expression(0));
        typeRegistry.getType(ctx.expression(1));

        if ("!=".equals(operator)) {
            String opCode = "eq";

            if (leftType == Type.INT) {
                opCode += " I";
            } else if (leftType == Type.FLOAT) {
                opCode += " F";
            } else if (leftType == Type.STRING) {
                opCode += " S";
            }
            code.append(opCode).append("\n");
            code.append("not\n");
        } else {
            String opCode = "eq";

            if (leftType == Type.INT) {
                opCode += " I";
            } else if (leftType == Type.FLOAT) {
                opCode += " F";
            } else if (leftType == Type.STRING) {
                opCode += " S";
            }
            code.append(opCode).append("\n");
        }
        return null;
    }

    @Override
    public Void visitRelation(LanguageParser.RelationContext ctx) {
        visit(ctx.expression(0));

        Type leftType = typeRegistry.getType(ctx.expression(0));
        Type rightType = typeRegistry.getType(ctx.expression(1));

        if (leftType == Type.INT && rightType == Type.FLOAT) {
            code.append("itof\n");
            leftType = Type.FLOAT;
        }

        visit(ctx.expression(1));

        if (rightType == Type.INT && leftType == Type.FLOAT) {
            code.append("itof\n");
        }
        String operator = ctx.op.getText();

        String typeCode = "";
        if (leftType == Type.INT || (leftType == Type.FLOAT && rightType == Type.INT)) {
            typeCode = " I";
        } else if (leftType == Type.FLOAT) {
            typeCode = " F";
        } else if (leftType == Type.STRING) {
            typeCode = " S";
        }

        String opCode = switch (operator) {
            case "<" -> "lt";
            case ">" -> "gt";
            default -> "";
        };

        code.append(opCode).append(typeCode).append("\n");
        return null;
    }

    @Override
    public Void visitAdd(LanguageParser.AddContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));

        String operator = ctx.op.getText();
        Type leftType = typeRegistry.getType(ctx.expression(0));
        Type rightType = typeRegistry.getType(ctx.expression(1));

        if (operator.equals("+") || operator.equals("-")) {
            if (leftType == Type.INT && rightType == Type.FLOAT) {
                code.append("swap\n");
                code.append("itof\n");
                code.append("swap\n");
            } else if (leftType == Type.FLOAT && rightType == Type.INT) {
                code.append("itof\n");
            }

            if (operator.equals("+")) {
                if (leftType == Type.FLOAT || rightType == Type.FLOAT) {
                    code.append("add F\n");
                } else {
                    code.append("add I\n");
                }
            } else {
                if (leftType == Type.FLOAT || rightType == Type.FLOAT) {
                    code.append("sub F\n");
                } else {
                    code.append("sub I\n");
                }
            }
        } else if (operator.equals(".")) {
            code.append("concat\n");
        }

        return null;
    }

    @Override
    public Void visitMul(LanguageParser.MulContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));

        String operator = ctx.op.getText();
        Type leftType = typeRegistry.getType(ctx.expression(0));
        Type rightType = typeRegistry.getType(ctx.expression(1));

        if (leftType == Type.INT && rightType == Type.FLOAT) {
            code.append("swap\n");
            code.append("itof\n");
            code.append("swap\n");
        } else if (leftType == Type.FLOAT && rightType == Type.INT) {
            code.append("itof\n");
        }

        switch (operator) {
            case "*":
                if (leftType == Type.FLOAT || rightType == Type.FLOAT) {
                    code.append("mul F\n");
                } else {
                    code.append("mul I\n");
                }
                break;
            case "/":
                if (leftType == Type.FLOAT || rightType == Type.FLOAT) {
                    code.append("div F\n");
                } else {
                    code.append("div I\n");
                }
                break;
            case "%":
                code.append("mod\n");
                break;
        }

        return null;
    }

    @Override
    public Void visitUnary(LanguageParser.UnaryContext ctx) {
        visit(ctx.expression());

        Type type = typeRegistry.getType(ctx.expression());
        if (type == Type.INT) {
            code.append("uminus I\n");
        } else if (type == Type.FLOAT) {
            code.append("uminus F\n");
        }

        return null;
    }

    @Override
    public Void visitNot(LanguageParser.NotContext ctx) {
        visit(ctx.expression());
        code.append("not\n");
        return null;
    }

    @Override
    public Void visitParentheses(LanguageParser.ParenthesesContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Void visitDecValue(LanguageParser.DecValueContext ctx) {
        int value = Integer.parseInt(ctx.DEC().getText());
        code.append("push I ").append(value).append("\n");
        return null;
    }

    @Override
    public Void visitOctValue(LanguageParser.OctValueContext ctx) {
        String octStr = ctx.OCT().getText().substring(1);
        int value = 0;
        if (!octStr.isEmpty()) {
            value = Integer.parseInt(octStr, 8);
        }
        code.append("push I ").append(value).append("\n");
        return null;
    }

    @Override
    public Void visitHexValue(LanguageParser.HexValueContext ctx) {
        String hexStr = ctx.HEXA().getText().substring(2);
        int value = 0;
        if (!hexStr.isEmpty()) {
            value = Integer.parseInt(hexStr, 16);
        }
        code.append("push I ").append(value).append("\n");
        return null;
    }

    @Override
    public Void visitFloatValue(LanguageParser.FloatValueContext ctx) {
        float value = Float.parseFloat(ctx.FLOAT_VALUE().getText());
        code.append("push F ").append(value).append("\n");
        return null;
    }

    @Override
    public Void visitBoolValue(LanguageParser.BoolValueContext ctx) {
        boolean value = ctx.boolValues().TRUE() != null;
        code.append("push B ").append(value).append("\n");
        return null;
    }

    @Override
    public Void visitStringValue(LanguageParser.StringValueContext ctx) {
        String value = ctx.STRING_VALUE().getText();
        value = value.substring(1, value.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        code.append("push S \"").append(value).append("\"\n");
        return null;
    }

    @Override
    public Void visitIdValue(LanguageParser.IdValueContext ctx) {
        String varName = ctx.ID().getText();
        code.append("load ").append(varName).append("\n");
        return null;
    }
}