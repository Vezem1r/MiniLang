package org.example;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashMap;
import java.util.Map;

public class TypeRegistry {
    private final Map<ParseTree, Type> expressionTypes = new HashMap<>();

    public void registerType(ParseTree ctx, Type type) {
        expressionTypes.put(ctx, type);
    }

    public Type getType(ParseTree ctx) {
        return expressionTypes.getOrDefault(ctx, Type.ERROR);
    }
}