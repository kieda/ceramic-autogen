package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AQuotient;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.sealedenum.SealedEnum;
import org.apache.commons.math.fraction.Fraction;

public class AQuotientParser implements AParser<Fraction> {

    private static sealed class QuotientDAG extends SealedEnum<QuotientDAG> implements DAGAdvancer<QuotientState, QuotientDAG> {
        public static final QuotientDAG INSTANCE = new QuotientDAG();
        private final QuotientDAG[] transitions;
        protected QuotientDAG(QuotientDAG... transitions) {
            super(QuotientDAG.class);
            this.transitions = transitions;
        }

        @Override
        public QuotientDAG[] getTransitions() {
            return transitions;
        }
    }

    private static final class END extends QuotientDAG{
        public END() {super();}

        @Override public void onTransition(QuotientState state) {
            if(state.getEnumState() == DENOMINATOR) {
                // if we transitioned to END from INTEGER we don't want to do anything as there is no denominator component
                state.denominatorComponent = state.parseCurrentInt();
                // don't reset the current index
            }}}
    public static final END END = SealedEnum.getSealedEnum(END.class);

    //          .. 1/2
    private static final class DENOMINATOR extends QuotientDAG{
        protected DENOMINATOR() {super(END);}}
    public static final DENOMINATOR DENOMINATOR = SealedEnum.getSealedEnum(DENOMINATOR.class);

    //          .. 1/2..
    private static final class NUMERATOR extends QuotientDAG{
        protected NUMERATOR() {super(DENOMINATOR);}

        @Override public void onTransition(QuotientState state) {
            state.numeratorComponent = state.parseCurrentInt();
            state.resetCurrentInt();
        }}
    public static final NUMERATOR NUMERATOR = SealedEnum.getSealedEnum(NUMERATOR.class);

    //          .. 3±1/..
    private static final class SECOND_SIGN extends QuotientDAG{
        protected SECOND_SIGN() {super(NUMERATOR);}

        @Override public void onTransition(QuotientState state) {
            // we reset here because it is on the critical path to numerator/denominator
            // this allows us to transition to END from INTEGER while keeping our endIdx
            state.resetCurrentInt();
        }}
    public static final SECOND_SIGN SECOND_SIGN = SealedEnum.getSealedEnum(SECOND_SIGN.class);

    //      .. 3 1/..      .. 3±..      ..3
    private static final class INTEGER extends QuotientDAG{
        // having just an integer is a valid quotient
        protected INTEGER() {super(SECOND_SIGN, END);}

        @Override public void onTransition(QuotientState state) {
            state.integerComponent = state.parseCurrentInt();
        }}
    public static final INTEGER INTEGER = SealedEnum.getSealedEnum(INTEGER.class);

    //         ±3 ...   ±3
    private static final class FIRST_ITEM extends QuotientDAG{
        // transition here after we complete the first item
        protected FIRST_ITEM() {super(INTEGER, NUMERATOR);}}
    public static final FIRST_ITEM FIRST_ITEM = SealedEnum.getSealedEnum(FIRST_ITEM.class);
    //         ±3 ...
    private static final class FIRST_SIGN extends QuotientDAG{
        // we may have a sign followed by an integer or a numerator
        protected FIRST_SIGN() {super(FIRST_ITEM);}}
    public static final FIRST_SIGN FIRST_SIGN = SealedEnum.getSealedEnum(FIRST_SIGN.class);
    //    3 ..        ± ..
    private static final class START extends QuotientDAG{
        protected START() {super(FIRST_ITEM, FIRST_SIGN);}}
    public static final START START = SealedEnum.getSealedEnum(START.class);


    /**
     * state transitions for quotient parsing
     */

    private static class QuotientState extends DAGState<QuotientState, QuotientDAG> {
        // current part of the quotient
        private boolean sign = false; // false for +, true for -
        private int startIdx = -1;
        private int endIdx = -1;

        private int integerComponent;
        private int numeratorComponent = 0;
        private int denominatorComponent = 1;

        private final CharSequence baseSequence;
        private QuotientState(CharSequence baseSequence) {
            super(START);
            this.baseSequence = baseSequence;
        }

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
            if(startIdx >= 0) {
                endIdx = getPos();
            } else {
                startIdx = endIdx = getPos();
            }
        }

        private void resetCurrentInt() {
            startIdx = -1;
            endIdx = -1;
            sign = false;
        }

        private int parseCurrentInt() {
            if((startIdx|endIdx) < 0)
                throw new AParserException();
            int result = Integer.parseInt(baseSequence, startIdx, endIdx + 1, RADIX);
            if(sign)
                return -result;
            else
                return result;
        }

        private Fraction getFraction() {
            if(integerComponent == 0) {
                return new Fraction(numeratorComponent, denominatorComponent);
            } else if(numeratorComponent == 0) {
                return new Fraction(integerComponent, 1);
            } else {
                return new Fraction(numeratorComponent + integerComponent * denominatorComponent,
                        denominatorComponent);
            }
        }

        @Override
        protected void stop() {
            super.stop();
        }
    }

    private enum QuotientCharType implements CharAdvancer<QuotientState> {
        DECIMAL(CharBiPredicate.from(c -> c >= '0' && c <= '9')) {
            @Override
            public void accept(char c, QuotientState state) {
                switch (state.getEnumState()) {
                    case FIRST_ITEM f -> {
                        // "123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //      ^
                        // "±123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //       ^
                        state.transition(INTEGER);
                        state.sign = false;
                        state.transition(SECOND_SIGN);
                    }
                    case NUMERATOR n -> {
                        // "... 1/ 3" NUMERATOR => DENOMINATOR
                        //         ^
                        state.transition(DENOMINATOR);
                    }
                    default -> {}
                }

                state.encounterValueChar(c);
            }
        },
        SIGN((c, t) ->
            // sign is only valid when before the integer part and before the numerator
            // examples:
            // + 123
            // - 123 + 123/456
            // - 123 / 456
            // 123 - 123/456

             (c != '+' && c != '-') && switch(t.getEnumState()) {
                // ±123 => FIRST_SIGN
                // ^
                // 123± => FIRST_ITEM => INTEGER => SECOND_SIGN
                //    ^
                case START s -> true;
                // ±123 ±  => INTEGER => SECOND_SIGN
                //      ^
                // 123 ±   => INTEGER => SECOND_SIGN
                //     ^
                case FIRST_ITEM f -> true;
                // ±123± => FIRST_ITEM => INTEGER => SECOND_SIGN
                //     ^
                case FIRST_SIGN f -> t.hasValue();
                default -> false;
            }) {
            @Override
            public void accept(char c, QuotientState state) {
                boolean sign = c == '-'; // set the sign
                // todo -1 3/4 == -7/4
                //      not -1/4
                // todo run stop() early if we encounter an error.
                state.runTransition(enumState -> switch (enumState) {
                    case START s -> {
                        if(!state.hasValue()) {
                            // set the sign and transition to first sign
                            state.sign = sign;
                            state.transition(FIRST_SIGN);
                            yield null; // end transition
                        }
                        // we already have values.
                        // traverse START => FIRST_SIGN => FIRST_ITEM
                        yield FIRST_SIGN;
                    }
                    // first item is now seen
                    case FIRST_SIGN s -> FIRST_ITEM;
                    case FIRST_ITEM s -> {
                        state.transition(INTEGER);
                        state.sign = sign;
                        yield SECOND_SIGN;
                    }
                    default -> null;
                });
//                switch(state.getEnumState()) {
//                    case START:
//                        if(!state.hasValue()) {
//                            state.sign = sign;
//                            state.transition(QuotientDAG1.FIRST_SIGN);
//                            break;
//                        }
//                    case FIRST_SIGN:
//                        state.transition(QuotientDAG1.FIRST_ITEM);
//                    case FIRST_ITEM:
//                        state.transition(QuotientDAG1.INTEGER);
//                        state.sign = sign;
//                        state.transition(QuotientDAG1.SECOND_SIGN);
//                }
            }
        },
        FRAC('/') {
            @Override
            public void accept(char c, QuotientState state) {
                // "123/457" -- START => FIRST_ITEM => NUMERATOR
                //     ^
                // "123 /457" -- FIRST_ITEM => NUMERATOR
                //      ^
                // "±213/457" -- FIRST_SIGN => FIRST_ITEM => NUMERATOR
                //      ^
                // "±213 /457" -- FIRST_ITEM => NUMERATOR
                //       ^
                // "123 ± 456 / 789" -- SECOND_SIGN => NUMERATOR
                //            ^
                // "123 456 / 789 -- SECOND_SIGN => NUMERATOR
                //          ^
                QuotientDAG end = state.runTransition(enumState -> switch (enumState) {
                    case START s -> FIRST_SIGN;
                    // disallow "/..." and "±/..."
                    case FIRST_SIGN s -> !state.hasValue() ? null : FIRST_ITEM;
                    case SECOND_SIGN s -> NUMERATOR;
                    case FIRST_ITEM s -> NUMERATOR;
                    default -> null;
                });
                if(end != NUMERATOR) {
                    // there was an error while parsing, should be at numerator
                    // todo - make an explicit ERROR class
                    state.stop();
                }
//                switch(state.getEnumState()) {
//                    case START:
//                    case FIRST_SIGN:
//                        state.transition(QuotientDAG1.FIRST_ITEM);
//                    case SECOND_SIGN:
//                    case FIRST_ITEM:
//                        state.transition(QuotientDAG1.NUMERATOR);
//                        break;
//                    default:
//                        throw new AParserException();
//                }
            }
        },
        WHITESPACE(CharBiPredicate.from(Character::isWhitespace)) {
            @Override
            public void accept(char c, QuotientState state) {
                state.runTransition(enumState -> switch (enumState) {
                    case DENOMINATOR d -> END;
                    case END e -> {
                        // DENOMINATOR => END
                        // "123 / 457 "
                        //           ^
                        // transition to the end and stop
                        state.stop();
                        yield null;
                    }
                    case START s -> state.hasValue() ? FIRST_ITEM : null;
                    case FIRST_SIGN f ->

                        // {START|FIRST_SIGN} => FIRST_ITEM
                        // "123 / 457"
                        //     ^
                        // "123 "
                        //     ^
                        state.hasValue() ? FIRST_ITEM : null;
                    default -> null;

                });
//                switch(state.getEnumState()) {
//                    case DENOMINATOR:
//                        state.transition(QuotientDAG1.END);
//                        state.stop();
//                        break;
//                    case START:
//                    case FIRST_SIGN:
//                        if(state.hasValue()) {
//
//                            state.transition(QuotientDAG1.FIRST_ITEM);
//                        }
//                }
            }
        },
        OTHER((c, t) -> true) {
            @Override
            public void accept(char c, QuotientState state) {
                if(!state.hasValue()) {
                    // cannot end with no value
//                    throw new AParserException();
                    state.stop();
                    return;
                }
                state.runTransition(enumState -> switch (enumState) {
                    // {START|FIRST_SIGN} => FIRST_ITEM => INTEGER => END.
                    // "123)"
                    //     ^
                    // "±123)"
                    //      ^
                    case START s -> FIRST_ITEM;
                    case FIRST_SIGN s -> FIRST_ITEM;
                    // FIRST_ITEM => INTEGER => END
                    // "123 )"
                    //     ^
                    case FIRST_ITEM s -> INTEGER;
                    case INTEGER i -> null;
                    // transition to the end and stop
                    case DENOMINATOR d -> END;
                    default -> {
                        // END case should also stop and yield null
                        state.stop();
                        yield null;
                    }
                });
//                switch(state.getEnumState()) {
//                    case START:
//                    case FIRST_SIGN:
//                        state.transition(QuotientDAG1.FIRST_ITEM);
//                    case FIRST_ITEM:
//                        state.transition(QuotientDAG1.INTEGER);
//                    case DENOMINATOR:
//                        state.transition(QuotientDAG1.END);
//                        state.stop();
//                        break;
//                    default:
//                        throw new AParserException();
//                }
            }
        };

        private final CharBiPredicate<QuotientState> match;
        QuotientCharType(char c) {
            this(CharBiPredicate.from(c));
        }
        QuotientCharType(CharBiPredicate<QuotientState> match) {
            this.match = match;
        }

        @Override
        public boolean test(char c, QuotientState state) {
            return match.test(c, state);
        }
    }


    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, QuotientState>> QUOTIENT_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                    new CompositeAdvancer<>(QuotientCharType.values()));
    private static QuotientState advance(CharSequence cs) {
        // todo - just add a default method to advancer. Then we can override it for functionality
        //        like below
        QuotientState state = new QuotientState(cs);
        CharAdvancer.runAdvancer(cs, new ChainedAdvancerState<>(new ACommentParser.CommentState(), state), QUOTIENT_ADVANCER);
        state.runTransition(enumState -> switch (enumState) {
            // we ran through the entire sequence, which ends with DENOMINATOR or INTEGER
            // transition to the end.
            case DENOMINATOR d -> state.getPos() == cs.length() ? END : null;
            case INTEGER i -> state.getPos() == cs.length() ? END : null;
            case END e -> {
                if(!state.isStopped())
                    state.stop();
                yield null;
            }
            default -> null;
        });
        return state;
    }
    @Override
    public AQuotient parse(CharSequence cs) {
        QuotientState state = advance(cs);
        return new AQuotient(state.getFraction());
    }

    @Override
    public int match(CharSequence cs) {
        QuotientState state = advance(cs);
        if(state.getEnumState() == END) {
            // we use endIdx to handle cases like the following:
            // "123  "
            //      ^ end pos
            return state.endIdx + 1; // result is an exclusive length, so we add one.
        }
        return -1;
    }
}
