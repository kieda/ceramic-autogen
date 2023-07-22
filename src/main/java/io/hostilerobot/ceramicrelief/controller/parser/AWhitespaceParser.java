package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AWhitespace;

public class AWhitespaceParser implements AParser<Void>{
    private static final AWhitespaceParser INSTANCE = new AWhitespaceParser();
    // since this is a void type and the parse results are ignored, we may use getInstance() to combine with parsers of any type
    public static <T> AParser<T> getInstance() {
        return (AParser<T>) INSTANCE;
    }
    AWhitespaceParser() {}

    @Override
    public AWhitespace parse(CharSequence cs) {
        return AWhitespace.getInstance();
    }

    @Override
    public int match(CharSequence cs) {
        // "" -> 0
        // "a" -> -1
        // " a" -> 1
        // " " -> 1

        if(cs.isEmpty())
            return 0;

        int pos = 0;

        while(pos < cs.length() && Character.isWhitespace(cs.charAt(pos))) {
            pos++;
        }

        return pos == 0 ? -1 : pos;
    }

    @Override
    public boolean ignore() {
        // we ignore whitespace and don't add it to the AST
        return true;
    }
}
