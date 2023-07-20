package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ADecimal;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.sealedenum.SealedEnum;

import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADecimalParser implements AParser<Double> {
    private static sealed class DecimalDAG extends SealedEnum<DecimalDAG> implements DAGAdvancer<DecimalState, DecimalDAG> {
        private final DecimalDAG[] transitions;
        protected DecimalDAG(DecimalDAG... transitions) {
            super(DecimalDAG.class);
            this.transitions = transitions;
        }

        @Override
        public DecimalDAG[] getTransitions() {
            return transitions;
        }

        public boolean isEndState() {
            return false;
        }
    }

    private final static class ERROR extends DecimalDAG{
        private ERROR() {super();}

        @Override public boolean isEndState() {
            return true;
        }}
    private final static class END extends DecimalDAG{
        private END() {super();}
        @Override public boolean isEndState() {
            return true;
        }}
    private final static class DECIMAL extends DecimalDAG{
        private DECIMAL() {super(getSealedEnum(END.class));}}
    // is in the form .123
    private final static class DECIMAL_SEP extends DecimalDAG{
        private DECIMAL_SEP() {super(getSealedEnum(DECIMAL.class), getSealedEnum(END.class));}}
    // is in the form 1.123 or 1.
    private static final class INT_SEP extends DecimalDAG {
        private INT_SEP() {super(getSealedEnum(DECIMAL.class), getSealedEnum(END.class));}}
    private static final class INT extends DecimalDAG{
        private INT() {super(getSealedEnum(INT_SEP.class), getSealedEnum(END.class), getSealedEnum(ERROR.class));}}
    private static final class SIGN extends DecimalDAG{
        private SIGN() {super(getSealedEnum(DECIMAL_SEP.class), getSealedEnum(INT.class), getSealedEnum(ERROR.class));}}
    private static final class START extends DecimalDAG{
        private START() {super(getSealedEnum(SIGN.class), getSealedEnum(DECIMAL_SEP.class), getSealedEnum(INT.class), getSealedEnum(ERROR.class));}}

    private static final DecimalDAG INSTANCE = new DecimalDAG();
    private static final ERROR VERROR = SealedEnum.getSealedEnum(ERROR.class);
    private static final END VEND = SealedEnum.getSealedEnum(END.class);
    private static final DECIMAL VDECIMAL = SealedEnum.getSealedEnum(DECIMAL.class);
    private static final DECIMAL_SEP DSEP = SealedEnum.getSealedEnum(DECIMAL_SEP.class);
    private static final INT_SEP ISEP = SealedEnum.getSealedEnum(INT_SEP.class);
    private static final INT VINT = SealedEnum.getSealedEnum(INT.class);
    private static final SIGN VSIGN = SealedEnum.getSealedEnum(SIGN.class);
    private static final START VSTART = SealedEnum.getSealedEnum(START.class);

    /**
     * format:
     *    SIGN? WHITESPACE* (SEPARATOR VALUE+|VALUE+ SEPARATOR| VALUE+ SEPARATOR VALUE+ | VALUE+)
     *  ^ ^                  ^         ^         ^      ^        ^       ^        ^         ^
     *  | SIGN               SEP       DECIMAL   INT  SEP       INT     SEP      DECIMAL   INT   END
     *  START                                                                               or ERROR
     *
     *  START -> SIGN|SEP|INT|ERROR (when encountering OTHER char at START)
     *  SIGN -> SEP|INT|ERROR (when encountering OTHER|SIGN)
     *  INT -> SEP|END|ERROR (when encountering SIGN, WHITESPACE, or reserved char OTHER we transition to END)
     *  SEP -> DECIMAL|END|ERROR (same as above)
     *  DECIMAL -> END|ERROR (same as above)
     *  ERROR
     */
    private enum DecimalCharType implements CharAdvancer<DecimalState> {
        SIGN( x -> x == '+' || x == '-') {
            @Override
            public void accept(char c, DecimalState state) {
                state.sign = c == '-';
                DecimalDAG startState = state.getEnumState();
                if(state.runTransition(enumState -> switch (enumState) {
                    case START s -> VSIGN;
                    case SIGN s -> startState == enumState ? VERROR : null;
                    case ERROR e -> null;
                    case END e -> null;
                    case DECIMAL_SEP d -> VERROR; // ".+" or ".-" is not allowed
                    default -> VEND; // INT_SEP, INT, DECIMAL
                }).isEndState()) {
                    state.stop();
                }
            }
        },
        VALUE( x-> x >= '0' && x <= '9' ) {
            @Override
            public void accept(char c, DecimalState state) {
                state.runTransition(enumState -> switch (enumState) {
                    case START s -> VINT;
                    case SIGN s -> VINT;
                    case DECIMAL_SEP s -> VDECIMAL; // .1
                    case INT_SEP s -> VDECIMAL; // 1.1
                    // INT, DECIMAL, ERROR, END
                    default -> null;
                });
                state.encounterValueChar(c);
            }
        },
        SEPARATOR('.') {
            @Override
            public void accept(char c, DecimalState state) {
                DecimalDAG startState = state.getEnumState();
                if(state.runTransition(enumState -> switch (enumState) {
                    case START s -> DSEP;
                    case SIGN s -> DSEP;
                    case INT i -> ISEP;
                    // don't allow 123..3 (multiple decimals in a row)
                    case DECIMAL_SEP s -> startState == enumState ? VERROR : null;
                    case INT_SEP s -> startState == enumState ? VERROR : null;
                    // INT, DECIMAL, ERROR, END
                    default -> null;
                }).isEndState()) {
                    state.stop();
                } else {
                    state.encounterValueChar(c);
                }
            }
        },
        WHITESPACE(Character::isWhitespace) {
            @Override
            public void accept(char c, DecimalState state) {
                if(state.runTransition(enumState -> switch (enumState) {
                    case START s -> VERROR; // " "
                    case INT i -> VEND;     // "123 "
                    case DECIMAL_SEP i -> VERROR; // ". " - whitespace not allowed between separator
                    case INT_SEP i -> VEND;  // "123. "
                    case DECIMAL d -> VEND; // "123.12 "
                    default -> null; // END, ERROR, SIGN "+ "
                }).isEndState()) {
                    state.stop();
                }
            }
        },
        OTHER(x -> true) {
            @Override
            public void accept(char c, DecimalState state) {
                if(state.runTransition(enumState -> switch (enumState) {
                    case START s -> VERROR; // "q"
                    case INT i -> Arrays.binarySearch(RESERVED_CHARS, c) < 0 ? VERROR : VEND; // "1q" vs "1," or "1("
                    case INT_SEP i -> Arrays.binarySearch(RESERVED_CHARS, c) < 0 ? VERROR : VEND; // "123.q" vs "123.,"
                    case DECIMAL_SEP ds -> VERROR; // ".," or ".q" are not allowed
                    case DECIMAL d -> Arrays.binarySearch(RESERVED_CHARS, c) < 0 ? VERROR : VEND; // 123.123q vs 123.123,
                    case SIGN s -> VERROR; // "+q"
                    default -> null; // END, ERROR
                }).isEndState()) {
                    state.stop();
                }
            }
        };
        private final CharBiPredicate<DecimalState> match;
        DecimalCharType(char flag) {
            this(CharBiPredicate.from(flag));
        }
        DecimalCharType(CharPredicate match) {
            this(CharBiPredicate.from(match));
        }
        DecimalCharType(CharBiPredicate<DecimalState> match) {
            this.match = match;
        }
        @Override
        public boolean test(char c, DecimalState state) {
            return match.test(c, state);
        }
    }

    private static class DecimalState extends DAGState<DecimalState, DecimalDAG> {
        private boolean sign = false;
        private int startDecimal = -1;
        private int endDecimal = -1;
        public DecimalState() {
            super(VSTART);
        }

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
            if((startDecimal|endDecimal) < 0) {
                startDecimal = endDecimal = getPos();
            } else {
                endDecimal = getPos();
            }
        }

        public double parseDouble(CharSequence base) {
            String doubleToParse = ((startDecimal|endDecimal) < 0) ? "" :
                    String.valueOf(base.subSequence(startDecimal, endDecimal + 1));
            return Double.parseDouble(doubleToParse);
        }

        @Override
        protected void stop() {
            super.stop();
        }
    }

    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, DecimalState>> DECIMAL_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                    new CompositeAdvancer<>(DecimalCharType.values()));

    private static DecimalState advance(CharSequence cs) {
        DecimalState state = new DecimalState();
        CharAdvancer.runAdvancer(cs, new ChainedAdvancerState<>(new ACommentParser.CommentState(), state), DECIMAL_ADVANCER);
        state.runTransition(enumState -> switch (enumState) {
            // cannot end on these.
            case START s -> VERROR; // ""
            case SIGN s -> VERROR;  // "+"
            case DECIMAL_SEP s -> VERROR; //"."
            // stop states
            case END e -> null;
            case ERROR e -> null;
            // everything else is valid and transitions to end.
            default ->  VEND;
        });
        if(!state.isStopped() && state.getEnumState().isEndState()) {
            state.stop();
        }

        return state;
    }

    @Override
    public ADecimal parse(CharSequence cs) {
        DecimalState state = advance(cs);
        return new ADecimal(state.parseDouble(cs));
    }

    @Override
    public int match(CharSequence cs) {
        DecimalState state = advance(cs);
        if(state.getEnumState() == VEND) {
            return state.endDecimal + 1;
        } else {
            return -1;
        }
    }
}
