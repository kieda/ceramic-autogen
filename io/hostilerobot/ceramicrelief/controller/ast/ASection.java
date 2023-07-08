package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.AbstractMap;
import java.util.Map;

public class ASection<K, V> implements ANode<Map.Entry<String, AList<APair<K, V>>>> {

    private final Map.Entry<String, AList<APair<K, V>>> section;
    public ASection(String sectionName,
                    AList<APair<K, V>> sectionItems) {
        section = new AbstractMap.SimpleImmutableEntry<>(sectionName, sectionItems);
    }

    @Override
    public Map.Entry<String, AList<APair<K, V>>> getValue() {
        return section;
    }

    @Override
    public int size() {
        return section.getValue().size();
    }
}
