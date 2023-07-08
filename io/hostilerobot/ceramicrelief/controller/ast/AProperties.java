package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.List;
import java.util.Map;

public class AProperties<K, V> implements
        ANode<AList<ASection<K, V>>> {

    private final AList<ASection<K, V>> propertySections;
    public AProperties(AList<ASection<K, V>> propertySections) {
        this.propertySections = propertySections;
    }


    @Override
    public AList<ASection<K, V>> getValue() {
        return propertySections;
    }

    @Override
    public int size() {
        return propertySections.size();
    }
}
