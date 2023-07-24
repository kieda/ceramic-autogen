package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AQuotient;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.sealedenum.SealedEnum;
import org.apache.commons.math.fraction.Fraction;

public class AQuotientParser implements AParser<Fraction> {

    /**
     * DFA:
     *
     * ERROR
     *     there is some error that has occurred
     * END
     *     we have ended successfully
     *     DENOMINATOR component parsed here if we were in DENOMINATOR state
     * DENOMINATOR => END
     *     we are currently in digits for the denominator
     * NUMERATOR => DENOMINATOR
     *     transition here after we've parsed the numerator portion, e.g. we've encountered a '/'
     *     NUMERATOR component parsed here
     *     and current int bounds are reset
     * SECOND_SIGN => NUMERATOR
     *     after finding FIRST_ITEM, we have found a +, -, or a subsequent digit. This means we found the second part of quotient,
     *         and first integer found is INTEGER component.
     *     current int bounds reset.
     * INTEGER => SECOND_SIGN, END
     *     we know that the first item is an integer. Parse it.
     *     INTEGER component parsed here
     * FIRST_ITEM => INTEGER, NUMERATOR
     *     we have found the end of first integer we've detected, but don't know what it it yet.
     * FIRST_SIGN => FIRST_ITEM
     *     if we see a + or a - before finding any decimals, transition here
     *     continue parsing decimals till we receive the end of the int and transition to FIRST_ITEM
     *     "  +   123"
     * START => FIRST_SIGN, FIRST_ITEM
     *     we have only found (optional) whitespace and decimals
     */
    private static sealed class QuotientDAG extends SealedEnum<QuotientDAG> implements DAGAdvancer<QuotientState, QuotientDAG> {
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
    public static final QuotientDAG INSTANCE = new QuotientDAG();

    private static final class ERROR extends QuotientDAG{
        private ERROR() {super();}

        @Override public void onTransition(QuotientState state) {
            if(!state.isStopped())
                state.stop();
        }}
    private static final ERROR ERROR = SealedEnum.getSealedEnum(ERROR.class);

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
        protected DENOMINATOR() {super(SealedEnum.getSealedEnum(END.class), SealedEnum.getSealedEnum(ERROR.class));}}
    public static final DENOMINATOR DENOMINATOR = SealedEnum.getSealedEnum(DENOMINATOR.class);

    //          .. 1/2..
    private static final class NUMERATOR extends QuotientDAG{
        protected NUMERATOR() {super(SealedEnum.getSealedEnum(DENOMINATOR.class), SealedEnum.getSealedEnum(ERROR.class));}

        @Override public void onTransition(QuotientState state) {
            state.numeratorComponent = state.parseCurrentInt();
            state.resetCurrentInt();
        }}
    public static final NUMERATOR NUMERATOR = SealedEnum.getSealedEnum(NUMERATOR.class);

    //          .. 3±1/..
    private static final class SECOND_SIGN extends QuotientDAG{
        protected SECOND_SIGN() {super(SealedEnum.getSealedEnum(NUMERATOR.class), SealedEnum.getSealedEnum(ERROR.class));}

        @Override public void onTransition(QuotientState state) {
            // we reset here because it is on the critical path to numerator/denominator
            // this allows us to transition to END from INTEGER while keeping our endIdx
            state.resetCurrentInt();
        }}
    public static final SECOND_SIGN SECOND_SIGN = SealedEnum.getSealedEnum(SECOND_SIGN.class);

    //      .. 3 1/..      .. 3±..      ..3
    private static final class INTEGER extends QuotientDAG{
        // having just an integer is a valid quotient
        protected INTEGER() {super(SealedEnum.getSealedEnum(SECOND_SIGN.class), SealedEnum.getSealedEnum(END.class));}

        @Override public void onTransition(QuotientState state) {
            state.integerComponent = state.parseCurrentInt();
        }}
    public static final INTEGER INTEGER = SealedEnum.getSealedEnum(INTEGER.class);

    //         ±3 ...   ±3
    private static final class FIRST_ITEM extends QuotientDAG{
        // transition here after we complete the first item
        protected FIRST_ITEM() {super(SealedEnum.getSealedEnum(INTEGER.class), SealedEnum.getSealedEnum(NUMERATOR.class));}}
    public static final FIRST_ITEM FIRST_ITEM = SealedEnum.getSealedEnum(FIRST_ITEM.class);
    //         ±3 ...
    private static final class FIRST_SIGN extends QuotientDAG{
        // we may have a sign followed by an integer or a numerator
        protected FIRST_SIGN() {super(SealedEnum.getSealedEnum(FIRST_ITEM.class), SealedEnum.getSealedEnum(ERROR.class));}}
    public static final FIRST_SIGN FIRST_SIGN = SealedEnum.getSealedEnum(FIRST_SIGN.class);
    //    3 ..        ± ..
    private static final class START extends QuotientDAG{
        protected START() {super(SealedEnum.getSealedEnum(FIRST_ITEM.class), SealedEnum.getSealedEnum(FIRST_SIGN.class), SealedEnum.getSealedEnum(ERROR.class));}}
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
                throw new IllegalStateException();
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
                    // START and FIRST_SIGN remain the same.

                    // we completed the first int, and now we've found another one without a second sign
                    case FIRST_ITEM f -> {
                        // "123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //      ^
                        // "±123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //       ^
                        boolean previousSign = state.sign;
                        state.transition(INTEGER);
                        state.transition(SECOND_SIGN);
                        state.sign = previousSign; // second sign matches previous
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
        SIGN((c, t) -> (c == '+' || c == '-')) {
            @Override
            public void accept(char c, QuotientState state) {
                // sign is only valid when before the integer part and before the numerator
                // examples:
                // + 123
                // - 123 + 123/456
                // - 123 / 456
                // 123 - 123/456
                QuotientDAG startEnumState = state.getEnumState();
                boolean valid = switch(startEnumState) {
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
                    case FIRST_SIGN f -> state.hasValue(); // don't permit two signs in a row
                    default -> false;
                };
                if(!valid) {
                    state.transition(ERROR);
                    state.stop();
                    return;
                }

                boolean sign = c == '-'; // set the sign
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
                        yield FIRST_ITEM;
                    }
                    // first item is now seen
                    case FIRST_SIGN s -> FIRST_ITEM;
                    case FIRST_ITEM s -> {
                        state.transition(INTEGER);
                        yield SECOND_SIGN;
                    }
                    case SECOND_SIGN s -> {
                        state.sign = sign;
                        yield null;
                    }
                    default -> null;
                });
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
                QuotientDAG startState = state.getEnumState();
                QuotientDAG end = state.runTransition(enumState -> switch (enumState) {
                    // disallow "/..."
                    case START s -> !state.hasValue() ? ERROR : FIRST_ITEM;
                    // disallow "±/..."
                    case FIRST_SIGN s -> !state.hasValue() ? ERROR : FIRST_ITEM;
                    case SECOND_SIGN s -> NUMERATOR;
                    case FIRST_ITEM s -> NUMERATOR;
                    case NUMERATOR n -> (startState == NUMERATOR) ? ERROR : null;
                    case DENOMINATOR d -> ERROR;
                    case ERROR e -> {
                        state.stop();
                        yield null;
                    }
                    default -> null;
                });
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
            }
        },
        OTHER((c, t) -> true) {
            @Override
            public void accept(char c, QuotientState state) {
                if(!state.hasValue()) {
                    // cannot end with no value
                    state.transition(ERROR);
                    state.stop();
                    return;
                }
                state.runTransition(enumState -> switch (enumState) {
                    // {START|FIRST_SIGN} => FIRST_ITEM => INTEGER => END.
                    // "123)"
                    //     ^
                    case START s -> FIRST_ITEM;
                    // "±123)"
                    //      ^
                    case FIRST_SIGN s -> FIRST_ITEM;
                    // FIRST_ITEM => INTEGER => END
                    // "123 )"
                    //     ^
                    case FIRST_ITEM s -> INTEGER;
                    case INTEGER i -> END;
                    // transition to the end and stop, denominator complete
                    case DENOMINATOR d -> END;
                    case ERROR e -> null;
                    default -> {
                        // END case should also stop and yield null
                        // all other cases should ERROR
                        state.stop();
                        yield enumState == END ? null : ERROR;
                    }
                });
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
    private static QuotientState advance(QuotientState state, CharSequence cs) {
        // todo - just add a default method to advancer. Then we can override it for functionality
        //        like below
            CharAdvancer.runAdvancer(cs, new ChainedAdvancerState<>(new ACommentParser.CommentState(), state), QUOTIENT_ADVANCER);
        state.runTransition(enumState -> switch (enumState) {
            // we ran through the entire sequence, We advance forward whatever states left.
            case START s -> FIRST_ITEM;
            case FIRST_SIGN fs -> FIRST_ITEM;
            case FIRST_ITEM fi -> INTEGER;
            case DENOMINATOR d -> state.getPos() == cs.length() ? END : ERROR;
            case INTEGER i -> state.getPos() == cs.length() ? END : ERROR;
            case END e -> {
                if(!state.isStopped())
                    state.stop();
                yield null;
            }
            case ERROR e -> {
                if(!state.isStopped())
                    state.stop();
                yield null;
            }
            default -> ERROR;
        });
        return state;
    }
    @Override
    public AQuotient parse(CharSequence cs) {
        QuotientState state = advance(new QuotientState(cs), cs);
        return new AQuotient(state.getFraction());
    }

    @Override
    public int match(CharSequence cs) {
        QuotientState state = new QuotientState(cs);
        if(cs.isEmpty() ||
                // we aren't directly looking at the start of the quotient, so we return -1
                (!QuotientCharType.DECIMAL.test(cs.charAt(0), state) && !QuotientCharType.SIGN.test(cs.charAt(0), state))) {
            return -1;
        }
        state = advance(state, cs);
        if(state.getEnumState() == END) {
            // we use endIdx to handle cases like the following:
            // "123  "
            //      ^ end pos
            return state.endIdx + 1; // result is an exclusive length, so we add one.
        }
        return -1;
    }
}
