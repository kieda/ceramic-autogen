package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AName aName = (AName) o;
        return Objects.equals(name, aName.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
