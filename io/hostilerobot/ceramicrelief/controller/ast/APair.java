package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.AbstractMap;
import java.util.Map;

public class APair<K, V> implements ANode<Map.Entry<ANode<K>, ANode<V>>>{
    private final Map.Entry<ANode<K>, ANode<V>> pair;
    public APair(ANode<K> key, ANode<V> val) {
        pair = new AbstractMap.SimpleImmutableEntry<>(key, val);
    }

    @Override
    public Map.Entry<ANode<K>, ANode<V>> getValue() {
        return pair;
    }

    @Override
    public int size() {
        return 2;
    }
}
