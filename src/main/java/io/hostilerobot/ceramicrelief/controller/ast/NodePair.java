package io.hostilerobot.ceramicrelief.controller.ast;

public class NodePair<K, V> extends Pair<ANode<K>, ANode<V>>{
    public NodePair(ANode<K> key, ANode<V> val) {
        super(key, val);
    }
}
