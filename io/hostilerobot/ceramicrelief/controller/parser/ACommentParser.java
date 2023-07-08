package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AComment;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.AdvancerState;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CharAdvancer;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.EnumAdvancer;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;

import java.util.EnumMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ACommentParser implements AParser<CharSequence, AComment> {
    @Override
    public AComment parse(CharSequence cs) {
        Matcher matcher = COMMENT_PAT.matcher(cs);
        if(!matcher.lookingAt())
            throw new AParserException(cs + " is not in comment form");
        MatchResult result = matcher.toMatchResult();
        return new AComment(cs.subSequence(result.start(1), result.end(1)));
    }

    private static final Pattern COMMENT_PAT = Pattern.compile("^#(.*)$", Pattern.MULTILINE);
    @Override
    public int match(CharSequence cs) {
        Matcher matcher = COMMENT_PAT.matcher(cs);
        if(!matcher.lookingAt()) {
            return -1;
        }
        return matcher.toMatchResult().end();
    }

    static enum CommentCharType implements CharAdvancer<CommentAdvancerState> {
        COMMENT_BEGIN('#') {
            @Override
            public void accept(char c, CommentAdvancerState state) {
                state.startComment();
            }
        },
        NEW_LINE((c, state) -> c == '\r' || c == '\n') {
            @Override
            public void accept(char c, CommentAdvancerState state) {
                if(state.isInComment()) {
                    state.endComment();
                }
            }
        },
        IN_COMMENT((c, state) -> state.isInComment()),
        OUT_OF_COMMENT((c, state) -> !state.isInComment());

        private CommentCharType(char rep) {
            this((c, state) -> rep == c);
        }

        private CharBiPredicate<CommentAdvancerState> test;
        private CommentCharType(CharBiPredicate<CommentAdvancerState> test) {
            this.test = test;
        }

        @Override
        public void accept(char c, CommentAdvancerState state) {}

        @Override
        public boolean test(char c, CommentAdvancerState state) {
            return test.test(c, state);
        }
    }

    public static <S extends CommentAdvancerState> CharAdvancer<S> buildCommentAdvancer(
            CharAdvancer<S> whileOutOfComment) {
        return buildCommentAdvancer(whileOutOfComment, null);
    }

    public static <S extends CommentAdvancerState> CharAdvancer<S> buildCommentAdvancer(
            CharAdvancer<S> whileOutOfComment,
            CharAdvancer<S> whileInComment
    ) {
        EnumMap<CommentCharType, CharAdvancer<S>> map = new EnumMap<>(CommentCharType.class);
        map.put(CommentCharType.OUT_OF_COMMENT, whileOutOfComment);
        if(whileInComment != null)
            map.put(CommentCharType.IN_COMMENT, whileInComment);

        return new EnumAdvancer<>(CommentCharType.values(), map);
    }

    static class CommentAdvancerState extends AdvancerState {
        private boolean inComment = false;
        private int commentStart = -1;
        private int commentEnd = -1;

        /* handles */
        private void startComment() {
            inComment = true;
            commentStart = getPos();
            onStartComment();
        }

        private void endComment() {
            inComment = false;
            commentEnd = getPos();
            onEndComment();
        }

        /* events */
        protected void onStartComment(){}
        protected void onEndComment(){}

        /* accessors */
        public boolean isInComment() {
            return inComment;
        }
        public int getCommentStart() {
            return commentStart;
        }
        public int getCommentEnd() {
            return commentEnd;
        }
    }
}
