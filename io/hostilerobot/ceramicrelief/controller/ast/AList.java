package io.hostilerobot.ceramicrelief.controller.ast;


// X is resulting value of Y
// list of items are nodes of type Y which result in X
// Y[] getItems() : <Y extends ANode<X>>[] getValue()
public class AList<X, Y extends ANode<X>> implements ANode<Y[]> {
    private final Y[] items;
    public AList(Y[] items) {
        this.items = items;
    }

    @Override
    public Y[] getValue() {
        return items;
    }
    public Y get(int index) {
        return items[index];
    }
    @Override
    public int size() {
        return items.length;
    }
}
