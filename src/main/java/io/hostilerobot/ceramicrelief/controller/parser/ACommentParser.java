package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AComment;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;

import java.util.EnumMap;

public class ACommentParser implements AParser<CharSequence> {
    private static final CharAdvancer<CommentState> COMMENT_ADVANCER = new CompositeAdvancer<>(CommentCharType.values());

    @Override
    public AComment parse(CharSequence cs) {
        CommentState cas = new CommentState(true);
        CharAdvancer.runAdvancer(cs, cas, COMMENT_ADVANCER);
        CharSequence comment = cs.subSequence(cas.getCommentStart(), cas.getCommentEnd());
        return new AComment(comment);
    }
    @Override
    public int match(CharSequence cs) {
        CommentState cas = new CommentState(true);
        if(!cs.isEmpty() && CommentCharType.COMMENT_BEGIN.test(cs.charAt(0), cas)) {
            CharAdvancer.runAdvancer(cs, cas, COMMENT_ADVANCER);
            return cas.getCommentEnd();
        }
        return -1;
    }

    enum CommentCharType implements CharAdvancer<CommentState> {
        COMMENT_BEGIN('#') {
            @Override
            public void accept(char c, CommentState state) {
                state.startComment();
            }
        },
        NEW_LINE((c, state) -> c == '\r' || c == '\n') {
            @Override
            public void accept(char c, CommentState state) {
                if(state.isInComment()) {
                    state.endComment();
                    if(state.shouldStopOnCommentEnd())
                        state.stop();
                }
            }
        },
        IN_COMMENT((c, state) -> state.isInComment()),
        OUT_OF_COMMENT((c, state) -> !state.isInComment());

        private CharBiPredicate<CommentState> test;
        CommentCharType(char rep) { this(CharBiPredicate.from(rep)); }
        CommentCharType(CharBiPredicate<CommentState> test) {
            this.test = test;
        }

        @Override
        public void accept(char c, CommentState state) {}

        @Override
        public boolean test(char c, CommentState state) {
            return test.test(c, state);
        }
    }

    public static <S extends AdvancerState> CharAdvancer<ChainedAdvancerState<CommentState, S>> buildCommentAdvancer(
            CharAdvancer<S> whileOutOfComment) {
        return buildCommentAdvancer(whileOutOfComment, null);
    }

    public static <S extends AdvancerState> CharAdvancer<ChainedAdvancerState<CommentState, S>> buildCommentAdvancer(
            CharAdvancer<S> whileOutOfComment,
            CharAdvancer<S> whileInComment) {
        EnumMap<CommentCharType, CharAdvancer<S>> map = new EnumMap<>(CommentCharType.class);
        map.put(CommentCharType.OUT_OF_COMMENT, whileOutOfComment);
        map.put(CommentCharType.NEW_LINE, whileOutOfComment);
            // consider new line to be out of the comment
            // so we don't consume them and they are treated as whitespace; can separate values

        if(whileInComment != null)
            map.put(CommentCharType.IN_COMMENT, whileInComment);

        return new ChainedEnumAdvancer<>(CommentCharType.values(), map);
    }

    static class CommentState extends AdvancerState {
        private boolean inComment = false;
        private int commentStart = -1;
        private int commentEnd = -1;

        // private method to allow us to stop parsing when the comment ends
        // only used internally
        private final boolean stopOnCommentEnd;
        private CommentState(boolean stopOnCommentEnd) {
            this.stopOnCommentEnd = stopOnCommentEnd;
        }
        // by default we continue parsing through the whole string
        // to allow other classes to compose with this one
        public CommentState() {
            this.stopOnCommentEnd = false;
        }

        private boolean shouldStopOnCommentEnd() {
            return stopOnCommentEnd;
        }

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

        @Override
        protected void stop() {
            super.stop();
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
