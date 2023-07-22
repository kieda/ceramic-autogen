package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AProperties<V> implements ANode<List<ASection<V>>> {
    private final List<ASection<V>> propertySections;
    public AProperties() {
        this.propertySections = new ArrayList<>();
    }

    public void add(ASection<V> node) {
        propertySections.add(node);
    }

    public ASection<V> get(int index) {
        return propertySections.get(index);
    }

    @Override
    public List<ASection<V>> getValue() {
        return propertySections;
    }

    @Override
    public int size() {
        return propertySections.size();
    }

    @Override
    public String toString() {
        return propertySections.stream().map(ASection::toString).collect(Collectors.joining("\n"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AProperties<?> that = (AProperties<?>) o;
        return Objects.equals(propertySections, that.propertySections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertySections);
    }
}
