package io.hostilerobot.ceramicrelief.controller.ast;

public class AName implements ANode<CharSequence> {
    private final CharSequence name;
    public AName(CharSequence name) {
        this.name = name;
    }
    @Override
    public CharSequence getValue() {
        return name;
    }
    @Override
    public int size() {
        return 1;
    }

    @Override
    public String toString() {
        return String.valueOf(name);
    }
}
