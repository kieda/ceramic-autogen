package io.hostilerobot.ceramicrelief.controller.ast;

public class ASectionList<K, V> extends AList<NodePair<K, V>> {
    public ASectionList(ANode<NodePair<K, V>>[] items) {
        super(items);
    }
}
