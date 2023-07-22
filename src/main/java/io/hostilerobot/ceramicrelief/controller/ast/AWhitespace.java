package io.hostilerobot.ceramicrelief.controller.ast;


public class AWhitespace implements ANode<Void>{
    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean ignore() {
        return true;
    }

    private AWhitespace() {}
    private static final AWhitespace INSTANCE = new AWhitespace();
    public static AWhitespace getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return " ";
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}
