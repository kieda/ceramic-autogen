package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.AbstractMap;
import java.util.Map;

public class ASection<K, V, PK extends ANode<K>, PV extends ANode<V>> implements ANode<Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>>> {

    private final Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>> section;
    public ASection(String sectionName,
                    AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>> sectionItems) {
        section = new AbstractMap.SimpleImmutableEntry<>(sectionName, sectionItems);
    }

    @Override
    public Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>> getValue() {
        return section;
    }

    @Override
    public int size() {
        return section.getValue().size();
    }
}
