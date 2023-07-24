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
    private static sealed class SectionNameDAG extends SealedEnum<SectionNameDAG> implements DAGAdvancer<SectionNameMatch, SectionNameDAG> {
        private final SectionNameDAG[] transitions;
        protected SectionNameDAG(SectionNameDAG... transitions) {
            super(SectionNameDAG.class);
            this.transitions = transitions;
        }

        @Override
        public SectionNameDAG[] getTransitions() {
            return transitions;
        }
    }
    private static final class COLON extends SectionNameDAG {
        private COLON() {super();}}
    // name must be continuous, cannot have two parts broken by whitespace
    // we add this to the DAG to signify the property
    private static final class SPACE extends SectionNameDAG {
        private SPACE() {super(SealedEnum.getSealedEnum(COLON.class));}}
    private static final class NAME extends SectionNameDAG {
        private NAME() {super(SealedEnum.getSealedEnum(SPACE.class), SealedEnum.getSealedEnum(COLON.class));}}

    // first char has different rules compared to rest of the name
    // we add this to the DAG to signify this property and transition
    private static final class FIRST_CHAR extends SectionNameDAG {
        private FIRST_CHAR() {super(SealedEnum.getSealedEnum(SPACE.class), SealedEnum.getSealedEnum(COLON.class), SealedEnum.getSealedEnum(NAME.class));}}
    private static final class START extends SectionNameDAG {
        private START() {super(SealedEnum.getSealedEnum(FIRST_CHAR.class), SealedEnum.getSealedEnum(COLON.class));}}

    private static final SectionNameDAG INSTANCE = new SectionNameDAG();
    private static final SectionNameDAG COLON = SealedEnum.getSealedEnum(COLON.class);
    private static final SectionNameDAG SPACE = SealedEnum.getSealedEnum(SPACE.class);
    private static final SectionNameDAG NAME = SealedEnum.getSealedEnum(NAME.class);
    private static final SectionNameDAG FIRST_CHAR = SealedEnum.getSealedEnum(FIRST_CHAR.class);
    private static final SectionNameDAG START = SealedEnum.getSealedEnum(START.class);

    private static final boolean isValidFirstChar(char c) {
        return !(c >= '0' && c <= '9') && !Character.isWhitespace(c)
                && Arrays.binarySearch(AParser.RESERVED_CHARS, c) < 0;
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
                final SectionNameDAG startEnumState = state.getEnumState();
                state.runTransition(enumState -> switch(enumState) {
                    // transition to FIRST_CHAR, then check the leading char
                    case START s -> {
                        if(!isValidFirstChar(c)) {
                            state.stop();
                        } else {
                            state.encounterValueChar(c);
                            state.transition(FIRST_CHAR);
                            // transition to FIRST_CHAR then end this transition
                        }
                        yield null;
                    }
                    // transition to NAME, then check non-leading char
                    case FIRST_CHAR fc -> NAME;
                    // check the non-leading char
                    case NAME n -> {
                        // todo - change to RESERVED_SEPARATORS, but we also don't want the name to end in a reserved char
                        //    like  "com.foo.MyClass."
                        //    or    "my-phrase-"         -- what if we have my-phrase- 123.5, would it be "my-phrase-", "123.5" or "my-phrase" "- 123.5"
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

    private static class SectionNameMatch extends DAGState<SectionNameMatch, SectionNameDAG> {
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
            CharSequence name = startSectionName < 0 && endSectionName < 0 ? SmallCharSequence.make()
                : base.subSequence(startSectionName, endSectionName + 1);
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
        // we must be directly looking at the first char of the name in order for this to be valid.
        if(cs.isEmpty() || !(isValidFirstChar(cs.charAt(0)) || SectionNameCharType.END_NAME.test(cs.charAt(0), matchState))) {
            return -1;
        }

        CharAdvancer.runAdvancer(cs,
                ChainedAdvancerState.chain(new ACommentParser.CommentState(), matchState),
                SECTION_NAME_MATCH);
        if(matchState.getEnumState() == COLON) {
            return matchState.getPos();
        }
        return -1;
    }
}
