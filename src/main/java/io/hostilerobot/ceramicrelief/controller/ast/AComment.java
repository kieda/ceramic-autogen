package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.Objects;

public class AComment implements ANode<CharSequence> {
    private final CharSequence comment;
    public AComment(CharSequence comment) {
        this.comment = comment;
    }
    @Override
    public CharSequence getValue() {
        return comment;
    }

    // comment isn't a value, so its size is 0
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean ignore() {
        return true;
    }

    @Override
    public String toString() {
        return "#" + comment + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AComment aComment = (AComment) o;
        return Objects.equals(comment, aComment.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment);
    }
}
