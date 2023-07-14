package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ASectionName;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;
import io.hostilerobot.sealedenum.SealedEnum;

import java.util.Arrays;
import java.util.EnumMap;

/*
 * format:
 *   SectionName := [Name][WS]':'
 *   WS := (whitespace|[Comment])*
 */
public class ASectionNameParser implements AParser<CharSequence> {

    //'io.hostilerobot.ceramicrelief.controller.parser.ASectionNameParser.SectionNameDAG1' is not within its bound; should extend
    // 'io.hostilerobot.sealedenum.SealedEnum<io.hostilerobot.ceramicrelief.controller.parser.ASectionNameParser.SectionNameDAG1>'
    private static sealed class SectionNameDAG1 extends SealedEnum<SectionNameDAG1> implements SealedAdvancerDAG<SectionNameMatch, SectionNameDAG1> {
        private static final SectionNameDAG1 INSTANCE = new SectionNameDAG1();
        private final SectionNameDAG1[] transitions;
        protected SectionNameDAG1(SectionNameDAG1... transitions) {
            super(SectionNameDAG1.class);
            this.transitions = transitions;
        }

        @Override
        public SectionNameDAG1[] getTransitions() {
            return transitions;
        }
    }
    private static final class COLON extends SectionNameDAG1{
        private COLON() {super();}}
    private static final SectionNameDAG1 COLON = SealedEnum.getSealedEnum(COLON.class);

    // name must be continuous, cannot have two parts broken by whitespace
    // we add this to the DAG to signify the property
    private static final class SPACE extends SectionNameDAG1{
        private SPACE() {super(COLON);}}
    private static final SectionNameDAG1 SPACE = SealedEnum.getSealedEnum(SPACE.class);
    private static final class NAME extends SectionNameDAG1{
        private NAME() {super(SPACE, COLON);}}
    private static final SectionNameDAG1 NAME = SealedEnum.getSealedEnum(NAME.class);

    // first char has different rules compared to rest of the name
    // we add this to the DAG to signify this property and transition
    private static final class FIRST_CHAR extends SectionNameDAG1{
        private FIRST_CHAR() {super(SPACE, COLON, NAME);}}
    private static final SectionNameDAG1 FIRST_CHAR = SealedEnum.getSealedEnum(FIRST_CHAR.class);
    private static final class START extends SectionNameDAG1{
        private START() {super(FIRST_CHAR, COLON);}}
    private static final SectionNameDAG1 START = SealedEnum.getSealedEnum(START.class);

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
                state.transition(COLON);
                state.stop();
            }
        },
        WHITESPACE(Character::isWhitespace) {
            @Override
            public void accept(char c, SectionNameMatch state) {
                state.runTransition(enumState -> switch(enumState) {
                        // transition to SPACE
                        // unless we're at the start, then we could have whitespace before the name and colon
                        case FIRST_CHAR fc -> SPACE;
                        case NAME n -> SPACE;
                        // otherwise do nothing
                        default -> null;
                        });
            }
        },
        OTHER(c -> true) {
            @Override
            public void accept(char c, SectionNameMatch state) {
                state.runTransition(enumState -> switch(enumState) {
                    // transition to FIRST_CHAR, then check the leading char
                    case START s -> {
                        if((c >= '0' && c <= '9')
                                || Arrays.binarySearch(AParser.RESERVED_CHARS, c) >= 0) {
                            state.stop();
                            yield null;
                        } else {
                            state.encounterValueChar(c);
                            yield FIRST_CHAR;
                        }
                    }
                    // transition to NAME, then check non-leading char
                    case FIRST_CHAR fc -> NAME;
                    // check the non-leading char
                    case NAME n -> {
                        if(Arrays.binarySearch(AParser.RESERVED_CHARS, c) >= 0) {
                            // !!! we can't use the char in the name.
                            state.stop();
                        } else {
                            state.encounterValueChar(c);
                        }
                        yield null;
                    }
                    // terminate early to signify parse didn't complete properly
                    default -> {
                        state.stop();
                        yield null;
                    }
                });
            }
        };
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
    private static class SectionNameMatch extends SealedDAGState<SectionNameMatch, SectionNameDAG1> {
        SectionNameMatch() {
            super(START);
        }

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
        }

        @Override
        protected void stop() {
            super.stop();
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
        if(matchState.getEnumState() == COLON) {
            return matchState.getPos() + 1;
        }
        return -1;
    }
}
