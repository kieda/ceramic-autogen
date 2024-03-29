package io.hostilerobot.ceramicrelief.controller.ast;


import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

// X is resulting value of Y
// list of items are nodes of type Y which result in X
// Y[] getItems() : <Y extends ANode<X>>[] getValue()
public class AList<X> implements ANode<ANode<X>[]> {
    private final ANode<X>[] items;
    public AList(ANode<X>[] items) {
        this.items = items;
    }

    @Override
    public ANode<X>[] getValue() {
        return items;
    }
    public ANode<X> get(int index) {
        return items[index];
    }
    @Override
    public int size() {
        return items.length;
    }

    @Override
    public String toString() {
        return Arrays.stream(items).map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AList<?> aList = (AList<?>) o;
        return Arrays.equals(items, aList.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }
}
