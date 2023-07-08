package io.hostilerobot.ceramicrelief.controller.parser;


import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.ast.APair;

import java.util.Map;

public class APairParser<K, V>
    implements AParser<Map.Entry<ANode<K>, ANode<V>>> {

    @Override
    public APair<K, V> parse(CharSequence cs) {
        return null;
    }

    @Override
    public int match(CharSequence cs) {
        return 0;
    }
}
