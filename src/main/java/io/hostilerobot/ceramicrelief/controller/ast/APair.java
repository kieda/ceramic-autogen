package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.AbstractMap;
import java.util.Map;

public class APair<K, V> implements ANode<NodePair<K, V>>{
    private final NodePair<K, V> pair;
    public APair(ANode<K> key, ANode<V> val) {
        pair = new NodePair<>(key, val);
    }

    @Override
    public NodePair<K, V> getValue() {
        return pair;
    }

    @Override
    public int size() {
        return 2;
    }

    private boolean isPrimitive(ANode<?> node) {
        return node instanceof ADecimal
                || node instanceof AName
                || node instanceof AQuotient
                || node instanceof AWhitespace
                || node instanceof AComment
                || node instanceof AList;
    }
    @Override
    public String toString() {
        return (isPrimitive(pair.getKey()) && isPrimitive(pair.getVal())) ?
                // both primitives: use a = b
                // otherwise use {a = b} syntax
                (pair.getKey() + " = " + pair.getVal()) : ("{" + pair.getKey() + " = " + pair.getVal() + "}");
    }
}
