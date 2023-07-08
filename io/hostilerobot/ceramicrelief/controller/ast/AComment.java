package io.hostilerobot.ceramicrelief.controller.ast;

public class AComment implements ANode<CharSequence> {
    private final CharSequence comment;
    public AComment(CharSequence comment) {
        this.comment = comment;
    }
    @Override
    public CharSequence getValue() {
        return comment;
    }

    @Override
    public int size() {
        return 0;
    }
}
