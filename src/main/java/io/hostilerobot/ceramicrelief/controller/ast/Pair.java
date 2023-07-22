package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.Objects;

public class Pair<K, V> {
    private final K key;
    private final V val;
    public Pair(K key, V val) {
        this.key = key;
        this.val = val;
    }

    public K getKey() {
        return key;
    }
    public V getVal() {
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(key, pair.key) && Objects.equals(val, pair.val);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, val);
    }
}
