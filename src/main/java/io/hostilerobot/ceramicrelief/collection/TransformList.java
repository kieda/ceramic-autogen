package io.hostilerobot.ceramicrelief.collection;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

public class TransformList<U, V> extends AbstractList<V> {
    private List<U> delegate;
    private Function<U, V> transformation;

    public TransformList(List<U> list, Function<U, V> transformation) {
        this.delegate = list;
        this.transformation = transformation;
    }

    @Override
    public V get(int index) {
        return transformation.apply(delegate.get(index));
    }

    @Override
    public int size() {
        return delegate.size();
    }
}
