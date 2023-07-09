package io.hostilerobot.ceramicrelief.controller.ast;

public class ASection<K, V> implements ANode<Pair<ASectionName, ASectionList<K, V>>> {
    private final Pair<ASectionName, ASectionList<K, V>> section;
    public ASection(ASectionName sectionName, ASectionList<K, V> sectionItems) {
        section = new Pair<>(sectionName, sectionItems);
    }

    @Override
    public Pair<ASectionName, ASectionList<K, V>> getValue() {
        return section;
    }

    @Override
    public int size() {
        return section.getVal().size();
    }
}
