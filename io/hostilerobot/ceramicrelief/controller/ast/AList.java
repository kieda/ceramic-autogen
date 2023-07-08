package io.hostilerobot.ceramicrelief.controller.ast;


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
}
