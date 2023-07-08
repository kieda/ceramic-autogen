package io.hostilerobot.ceramicrelief.controller.ast;

public class NodePair<K, V> {
    private final ANode<K> key;
    private final ANode<V> val;
    public NodePair(ANode<K> key, ANode<V> val) {
        this.key = key;
        this.val = val;
    }

    public ANode<K> getKey() {
        return key;
    }

    public ANode<V> getVal() {
        return val;
    }
}
