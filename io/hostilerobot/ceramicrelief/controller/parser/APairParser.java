package io.hostilerobot.ceramicrelief.controller.parser;


import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.ast.APair;

import java.util.Map;

public class APairParser<K, V, PK extends ANode<K>, PV extends ANode<V>>
    implements AParser<Map.Entry<PK, PV>, APair<K, V, PK, PV>> {

    @Override
    public APair<K, V, PK, PV> parse(CharSequence cs) {
        return null;
    }

    @Override
    public int match(CharSequence cs) {
        return 0;
    }
}
