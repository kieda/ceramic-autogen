package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ASectionName;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.util.Arrays;
import java.util.EnumMap;

/*
 * format:
 *   SectionName := [Name][WS]':'
 *   WS := (whitespace|[Comment])*
 */
public class ASectionNameParser implements AParser<CharSequence> {
    private enum SectionNameDAG implements AdvancerDAG<SectionNameMatch, SectionNameDAG> {
        COLON(),
        SPACE(COLON), // name must be continuous, cannot have two parts broken by whitespace
                      // we add this to the DAG to signify the property
        NAME(SPACE, COLON),
        FIRST_CHAR(SPACE, COLON, NAME),
            // first char has different rules compared to rest of the name
            // we add this to the DAG to signify this property and transition
        START(FIRST_CHAR, COLON);

        private final SectionNameDAG[] transition;
        SectionNameDAG(SectionNameDAG... transition) {
            this.transition = transition;
        }

        @Override
        public SectionNameDAG[] getTransitions() {
            return transition;
        }
    }

    /* two ways of parsing the following:
     * abc : def : ghi :
     *
     * (abc :) (def :) (ghi :)          -- section name is parsed first
     * (abc) (:) (def) (:) (ghi) (:)    -- value is parsed first, then section name is empty string
     *
     * we want parse section name first. Permit empty string for section names.
     *  abc: 3 = 1 : asf : 4=5
     *  (abc: 3 = 1) (:) (asf : 4=5)
     *  abc: 3 = 1 : 123 : 4=5
     *  (abc: 3 = 1) (: 123) (: 4 = 5)
     */
    private enum SectionNameCharType implements CharAdvancer<SectionNameMatch> {
        END_NAME(':') {
            @Override
            public void accept(char c, SectionNameMatch state) {
                // transition to COLON and stop
                state.transition(SectionNameDAG.COLON);
                state.stop();
            }
        },
        WHITESPACE(Character::isWhitespace) {
            @Override
            public void accept(char c, SectionNameMatch state) {
                switch(state.getDAG()) {
                    case FIRST_CHAR, NAME ->
                            // transition to SPACE
                            // unless we're at the start, then we could have whitespace before the name and colon
                            state.transition(SectionNameDAG.SPACE);
                    // otherwise do nothing
                }
            }
        },
        OTHER(c -> true) {
            @Override
            public void accept(char c, SectionNameMatch state) {
                switch(state.getDAG()) {
                    case START:
                            // transition to FIRST_CHAR, then check the leading char
                            state.transition(SectionNameDAG.FIRST_CHAR);
                            if((c >= '0' && c <= '9')
                                || Arrays.binarySearch(AParser.RESERVED_CHARS, c) >= 0) {
                                state.stop();
                            } else {
                                state.encounterValueChar(c);
                            }
                            break;
                    case FIRST_CHAR:
                            // transition to NAME, then check non-leading char
                            state.transition(SectionNameDAG.NAME);
                    case NAME:
                            // check the non-leading char
                            if(Arrays.binarySearch(AParser.RESERVED_CHARS, c) >= 0) {
                                // !!! we can't use the char in the name.
                                state.stop();
                            } else {
                                state.encounterValueChar(c);
                            }
                            break;
                    default:
                        // terminate early to signify parse didn't complete properly
                        state.stop();
                }
            }
        }
        ;
        private final CharBiPredicate<SectionNameMatch> match;
        SectionNameCharType(char match) {
            this(CharBiPredicate.from(match));
        }
        SectionNameCharType(CharPredicate match) {
            this(CharBiPredicate.from(match));
        }
        SectionNameCharType(CharBiPredicate<SectionNameMatch> match) {
            this.match = match;
        }

        @Override
        public boolean test(char c, SectionNameMatch state) {
            return match.test(c, state);
        }
    }
    private static class SectionNameMatch extends AdvancerState {
        private SectionNameDAG dag;
        SectionNameMatch() {
            dag = SectionNameDAG.START;
        }
        public SectionNameDAG getDAG() {
            return dag;
        }

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
        }

        @Override
        protected void stop() {
            super.stop();
        }

        private void transition(SectionNameDAG next) {
            if(!dag.isValidTransition(next))
                throw new AParserException();
            next.onTransition(this);
            dag = next;
        }
    }
    private static class SectionNameParse extends SectionNameMatch {
        private int startSectionName = -1;
        private int endSectionName = -1;

        private final CharSequence base;
        public SectionNameParse(CharSequence base) {
            this.base = base;
        }

        private ASectionName parsed = null;
        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
            if(startSectionName >= 0) {
                endSectionName = getPos();
            } else{
                startSectionName = endSectionName = getPos();
            }
        }

        protected ASectionName getParsed() {
            return parsed;
        }

        protected void onNameEnd() {
            // might both be -1 if we didn't encounter any name values
            CharSequence name = startSectionName == endSectionName ? SmallCharSequence.make()
                : base.subSequence(startSectionName, endSectionName);
            parsed = new ASectionName(name);
        }
    }
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, SectionNameMatch>>
        SECTION_NAME_MATCH = ACommentParser.buildCommentAdvancer(new CompositeAdvancer<>(SectionNameCharType.values()));
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, SectionNameParse>>
        SECTION_NAME_PARSE;
    static {
        EnumMap<SectionNameCharType, CharAdvancer<SectionNameParse>> map = new EnumMap<>(SectionNameCharType.class);
        map.put(SectionNameCharType.END_NAME, new CharAdvancer<>() {
            @Override
            public void accept(char c, SectionNameParse state) {
                // when we end the section name we want to obtain it
                state.onNameEnd();
            }

            @Override
            public boolean test(char c, SectionNameParse state) {
                return true;
            }
        });
        // compose the parser so comments are ignored and we parse the name
        SECTION_NAME_PARSE = ACommentParser.buildCommentAdvancer(
                new SubclassEnumAdvancer<>(SectionNameCharType.values(), map));
    }

    @Override
    public ASectionName parse(CharSequence cs) {
        SectionNameParse matchState = new SectionNameParse(cs);
        CharAdvancer.runAdvancer(cs,
            ChainedAdvancerState.chain(new ACommentParser.CommentState(), matchState),
            SECTION_NAME_PARSE
        );

        return matchState.getParsed();
    }

    @Override
    public int match(CharSequence cs) {
        SectionNameMatch matchState = new SectionNameMatch();

        CharAdvancer.runAdvancer(cs,
                ChainedAdvancerState.chain(new ACommentParser.CommentState(), matchState),
                SECTION_NAME_MATCH);
        if(matchState.getDAG() == SectionNameDAG.COLON) {
            return matchState.getPos() + 1;
        }
        return -1;
    }
}
