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
}
