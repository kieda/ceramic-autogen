package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ANodeList<V> implements ANode<List<ANode<V>>> {
    private final List<ANode<V>> nodes;
    public ANodeList() {
        this.nodes = new ArrayList<>();
    }

    public void add(ANode<V> node) {
        nodes.add(node);
    }

    public ANode<V> get(int index) {
        return nodes.get(index);
    }

    @Override
    public List<ANode<V>> getValue() {
        return nodes;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public String toString() {
        return nodes.stream().map(String::valueOf).collect(Collectors.joining("\n")).indent(4).stripTrailing();
    }
}
