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

    // comment isn't a value, so its size is 0
    @Override
    public int size() {
        return 0;
    }
}