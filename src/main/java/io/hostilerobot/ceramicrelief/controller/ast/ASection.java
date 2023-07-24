package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.Objects;

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
        return section.getKey() + "\n" + section.getVal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ASection<?> aSection = (ASection<?>) o;
        return Objects.equals(section, aSection.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(section);
    }
}
