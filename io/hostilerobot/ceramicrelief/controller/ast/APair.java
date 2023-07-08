package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.AbstractMap;
import java.util.Map;

public class APair<K, V, PK extends ANode<K>, PV extends ANode<V>> implements ANode<Map.Entry<PK, PV>>{
    private final Map.Entry<PK, PV> pair;
    public APair(PK key, PV val) {
        pair = new AbstractMap.SimpleImmutableEntry<>(key, val);
    }

    @Override
    public Map.Entry<PK, PV> getValue() {
        return pair;
    }

    @Override
    public int size() {
        return 2;
    }
}
