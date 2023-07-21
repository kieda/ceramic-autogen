package io.hostilerobot.ceramicrelief.controller.ast;

public class ASection<V> implements ANode<Pair<ASectionName, ANodeList<V>>> {
    private final Pair<ASectionName, ANodeList<V>> section;

    public ASection(ASectionName sectionName, ANodeList<V> sectionItems) {
        section = new Pair<>(sectionName, sectionItems);
    }

    @Override
    public Pair<ASectionName, ANodeList<V>> getValue() {
        return section;
    }

    @Override
    public int size() {
        return section.getVal().size();
    }

    @Override
    public String toString() {
        return section.getKey() + "\n" + getValue();
    }
}
