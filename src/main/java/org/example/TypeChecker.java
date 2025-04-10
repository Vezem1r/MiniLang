package org.example;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TypeChecker extends LanguageBaseVisitor<Type> {
    private final SymbolTable symbolTable = new SymbolTable();
    private final ErrorHandler errorHandler;
    private final TypeRegistry typeRegistry = new TypeRegistry();

    public TypeChecker(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @Override
    public Type visitProgram(LanguageParser.ProgramContext ctx) {
        for (LanguageParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Type visitVariableDeclaration(LanguageParser.VariableDeclarationContext ctx) {
        String typeName = ctx.primitiveType().getText();
        Type type = Type.fromString(typeName);

        for (TerminalNode idNode : ctx.ID()) {
            String varName = idNode.getText();
            Token token = idNode.getSymbol();

            if (symbolTable.isDefined(varName)) {
                errorHandler.reportVariableError(token, "Variable '" + varName + "' is already defined");
            } else {
                symbolTable.define(new SymbolTable.Symbol(varName, type));
            }
        }
        return null;
    }

    @Override
    public Type visitRead(LanguageParser.ReadContext ctx) {
        for (TerminalNode idNode : ctx.ID()) {
            String varName = idNode.getText();
            Token token = idNode.getSymbol();

            if (!symbolTable.isDefined(varName)) {
                errorHandler.reportVariableError(token, "Undefined variable '" + varName + "' in read statement");
            }
        }

        return null;
    }

    @Override
    public Type visitFwrite(LanguageParser.FwriteContext ctx) {
        Type fileType = visit(ctx.expression(0));
        typeRegistry.registerType(ctx.expression(0), fileType);
        if (fileType != Type.FILE && fileType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.expression(0).getStart(),
                    "First argument of fwrite must be of type file, got " + fileType);
        }

        for (int i = 1; i < ctx.expression().size(); i++) {
            Type exprType = visit(ctx.expression(i));
            typeRegistry.registerType(ctx.expression(i), exprType);
        }

        return null;
    }

    @Override
    public Type visitWrite(LanguageParser.WriteContext ctx) {
        for (LanguageParser.ExpressionContext expression : ctx.expression()) {
            Type exprType = visit(expression);
            typeRegistry.registerType(expression, exprType);
        }
        return null;
    }

    @Override
    public Type visitBlock(LanguageParser.BlockContext ctx) {
        for (LanguageParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Type visitIf(LanguageParser.IfContext ctx) {
        Type conditionType = visit(ctx.condition());
        typeRegistry.registerType(ctx.condition().expression(), conditionType);
        if (conditionType != Type.BOOL && conditionType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.condition().getStart(),
                    "Condition in if statement must be of type bool, got " + conditionType);
        }

        visit(ctx.statement());
        if (ctx.else_() != null) {
            visit(ctx.else_().statement());
        }

        return null;
    }

    @Override
    public Type visitWhile(LanguageParser.WhileContext ctx) {
        Type conditionType = visit(ctx.condition());
        typeRegistry.registerType(ctx.condition().expression(), conditionType);
        if (conditionType != Type.BOOL && conditionType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.condition().getStart(),
                    "Condition in while statement must be of type bool, got " + conditionType);
        }

        visit(ctx.statement());

        return null;
    }

    @Override
    public Type visitExpressionEval(LanguageParser.ExpressionEvalContext ctx) {
        Type exprType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), exprType);
        return exprType;
    }

    @Override
    public Type visitNop(LanguageParser.NopContext ctx) {
        return null;
    }

    @Override
    public Type visitAssignment(LanguageParser.AssignmentContext ctx) {
        String varName = ctx.ID().getText();
        Token varToken = ctx.ID().getSymbol();

        if (!symbolTable.isDefined(varName)) {
            errorHandler.reportVariableError(varToken, "Undefined variable '" + varName + "' in assignment");
            return Type.ERROR;
        }

        Type leftType = symbolTable.resolve(varName).type();
        Type rightType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), rightType);
        typeRegistry.registerType(ctx, leftType);

        if (!Type.isConvertible(rightType, leftType) && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.getStart(),
                    "Cannot assign value of type " + rightType + " to variable of type " + leftType);
            return Type.ERROR;
        }

        return leftType;
    }

    @Override
    public Type visitOr(LanguageParser.OrContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        Type resultType = Type.resultType(leftType, rightType, "||");
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.getStart(),
                    "Incompatible types for operator '||'");
        }

        return resultType;
    }

    @Override
    public Type visitAnd(LanguageParser.AndContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        Type resultType = Type.resultType(leftType, rightType, "&&");
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.getStart(),
                    "Incompatible types for operator '&&'");
        }

        return resultType;
    }

    @Override
    public Type visitComparison(LanguageParser.ComparisonContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        String operator = ctx.op.getText();
        Type resultType = Type.resultType(leftType, rightType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.op,
                    "Incompatible types for operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitRelation(LanguageParser.RelationContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        String operator = ctx.op.getText();
        Type resultType = Type.resultType(leftType, rightType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.op,
                    "Incompatible types for operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitAdd(LanguageParser.AddContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        String operator = ctx.op.getText();
        Type resultType = Type.resultType(leftType, rightType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.op,
                    "Incompatible types for operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitMul(LanguageParser.MulContext ctx) {
        Type leftType = visit(ctx.expression(0));
        Type rightType = visit(ctx.expression(1));
        typeRegistry.registerType(ctx.expression(0), leftType);
        typeRegistry.registerType(ctx.expression(1), rightType);

        String operator = ctx.op.getText();
        Type resultType = Type.resultType(leftType, rightType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && leftType != Type.ERROR && rightType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.op,
                    "Incompatible types for operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitUnary(LanguageParser.UnaryContext ctx) {
        Type operandType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), operandType);
        String operator = "-";

        Type resultType = Type.resultTypeUnary(operandType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && operandType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.getStart(),
                    "Incompatible type for unary operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitNot(LanguageParser.NotContext ctx) {
        Type operandType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), operandType);
        String operator = "!";

        Type resultType = Type.resultTypeUnary(operandType, operator);
        typeRegistry.registerType(ctx, resultType);

        if (resultType == Type.ERROR && operandType != Type.ERROR) {
            errorHandler.reportTypeError(ctx.getStart(),
                    "Incompatible type for unary operator '" + operator + "'");
        }

        return resultType;
    }

    @Override
    public Type visitParentheses(LanguageParser.ParenthesesContext ctx) {
        Type exprType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), exprType);
        typeRegistry.registerType(ctx, exprType);
        return exprType;
    }

    @Override
    public Type visitDecValue(LanguageParser.DecValueContext ctx) {
        Type type = Type.INT;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitOctValue(LanguageParser.OctValueContext ctx) {
        Type type = Type.INT;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitHexValue(LanguageParser.HexValueContext ctx) {
        Type type = Type.INT;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitFloatValue(LanguageParser.FloatValueContext ctx) {
        Type type = Type.FLOAT;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitBoolValue(LanguageParser.BoolValueContext ctx) {
        Type type = Type.BOOL;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitStringValue(LanguageParser.StringValueContext ctx) {
        Type type = Type.STRING;
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitIdValue(LanguageParser.IdValueContext ctx) {
        String varName = ctx.ID().getText();
        Token token = ctx.ID().getSymbol();

        if (!symbolTable.isDefined(varName)) {
            errorHandler.reportVariableError(token, "Undefined variable '" + varName + "'");
            typeRegistry.registerType(ctx, Type.ERROR);
            return Type.ERROR;
        }

        Type type = symbolTable.resolve(varName).type();
        typeRegistry.registerType(ctx, type);
        return type;
    }

    @Override
    public Type visitCondition(LanguageParser.ConditionContext ctx) {
        Type exprType = visit(ctx.expression());
        typeRegistry.registerType(ctx.expression(), exprType);
        return exprType;
    }
}