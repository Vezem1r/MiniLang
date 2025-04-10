package org.example;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Map<String, Symbol> symbols = new HashMap<>();

    public void define(Symbol symbol) {
        if (symbols.containsKey(symbol.name())) return;
        symbols.put(symbol.name(), symbol);
    }

    public boolean isDefined(String name) {
        return symbols.containsKey(name);
    }

    public Symbol resolve(String name) {
        return symbols.get(name);
    }

    public record Symbol(String name, Type type) {
    }
}
