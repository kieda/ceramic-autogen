package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.List;
import java.util.Map;

public class AProperties<K, V, PK extends ANode<K>, PV extends ANode<V>> implements
        // yuck! :p
        // todo - do more research on recent java features and see if there's anything that can clean up this mess
        ANode<
            AList<
                Map.Entry<String,
                    AList<
                        Map.Entry<PK, PV>,
                        APair<K, V, PK, PV> > >,
                ASection<K, V, PK, PV> > > {

    private final AList<Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>>, ASection<K, V, PK, PV>> propertySections;
    public AProperties(AList<Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>>, ASection<K, V, PK, PV>> propertySections) {
        this.propertySections = propertySections;
    }


    @Override
    public AList<Map.Entry<String, AList<Map.Entry<PK, PV>, APair<K, V, PK, PV>>>, ASection<K, V, PK, PV>> getValue() {
        return propertySections;
    }

    @Override
    public int size() {
        return propertySections.size();
    }
}
