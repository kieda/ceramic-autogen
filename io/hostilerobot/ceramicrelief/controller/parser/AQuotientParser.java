package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AQuotient;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.AdvancerState;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CharAdvancer;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CompositeAdvancer;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import org.apache.commons.math.fraction.Fraction;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AQuotientParser implements AParser<Fraction> {

    /**
     * state transitions for quotient parsing
     */
    private enum QuotientPart{
        END() {
            @Override
            public void onTransition(QuotientState state) {
                if(state.getCurrentPart() == QuotientPart.DENOMINATOR) {
                    // if we transitioned to END from INTEGER we don't want to do anything as there is no denominator component
                    state.denominatorComponent = state.parseCurrentInt();
                    // don't reset the current index
                }
            }
        },
        //          .. 1/2
        DENOMINATOR(END),
        //          .. 1/2..
        NUMERATOR(DENOMINATOR) {
            @Override
            public void onTransition(QuotientState state) {
                state.numeratorComponent = state.parseCurrentInt();
                state.resetCurrentInt();
            }
        },
        //          .. 3±1/..
        SECOND_SIGN(NUMERATOR) {
            @Override
            public void onTransition(QuotientState state) {
                // we reset here because it is on the critical path to numerator/denominator
                // this allows us to transition to END from INTEGER while keeping our endIdx
                state.resetCurrentInt();
            }
        },
        //      .. 3 1/..      .. 3±..      ..3
        INTEGER(SECOND_SIGN, END) { // having just an integer is a valid quotient
            @Override
            public void onTransition(QuotientState state) {
                state.integerComponent = state.parseCurrentInt();
            }
        },
        //         ±3 ...   ±3 /
        FIRST_ITEM(INTEGER, NUMERATOR), // transition here after we complete the first item
        //         ±3 ...
        FIRST_SIGN(FIRST_ITEM), // we may have a sign followed by an integer or a numerator
        //    3 ..        ± ..
        START(FIRST_ITEM, FIRST_SIGN)
        ;
        private final QuotientPart[] transitions;
        QuotientPart(QuotientPart... transitions) {
            this.transitions = transitions;
        }
        public QuotientPart[] getTransitions() {
            return transitions;
        }
        public static QuotientPart getStartState() {
            return START;
        }
        public static QuotientPart getEndState() {
            return END;
        }

        public void onTransition(QuotientState state) {
            // default - do nothing. But this can be overridden
        }

        public boolean isValidTransition(QuotientPart next) {
            for(QuotientPart nPossible : this.getTransitions()) {
                if(nPossible == next)
                    return true;
            }
            return false;
        }
    }

    private static class QuotientState extends ACommentParser.CommentAdvancerState {
        // current part of the quotient
        private QuotientPart currentPart = QuotientPart.getStartState();
        private boolean sign = false; // false for +, true for -
        private int startIdx = -1;
        private int endIdx = -1;

        private int integerComponent;
        private int numeratorComponent = 0;
        private int denominatorComponent = 1;

        private final CharSequence baseSequence;
        private QuotientState(CharSequence baseSequence) {
            this.baseSequence = baseSequence;
        }

        public QuotientPart getCurrentPart() {
            return currentPart;
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

        private void transition(QuotientPart next) {
            if(!currentPart.isValidTransition(next))
                throw new AParserException();
            next.onTransition(this);
            currentPart = next;
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
                switch(state.getCurrentPart()) {
                    case FIRST_ITEM:
                        // "123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //      ^
                        // "±123 4/.." FIRST_ITEM => INTEGER => SECOND_SIGN
                        //       ^
                        state.transition(QuotientPart.INTEGER);
                        state.sign = false;
                        state.transition(QuotientPart.SECOND_SIGN);
                        break;
                    case NUMERATOR:
                        // "... 1/ 3" NUMERATOR => DENOMINATOR
                        //         ^
                        state.transition(QuotientPart.DENOMINATOR);
                }

                state.encounterValueChar(c);
            }
        },
        SIGN((c, t) -> {
            // sign is only valid when before the integer part and before the numerator
            // examples:
            // + 123
            // - 123 + 123/456
            // - 123 / 456
            // 123 - 123/456
            if(c != '+' && c != '-')
                return false;

            switch(t.getCurrentPart()) {
                case START:
                    // ±123 => FIRST_SIGN
                    // ^
                    // 123± => FIRST_ITEM => INTEGER => SECOND_SIGN
                    //    ^
                case FIRST_ITEM:
                    // ±123 ±  => INTEGER => SECOND_SIGN
                    //      ^
                    // 123 ±   => INTEGER => SECOND_SIGN
                    //     ^
                    return true;
                case FIRST_SIGN:
                    // ±123± => FIRST_ITEM => INTEGER => SECOND_SIGN
                    //     ^
                    return t.hasValue();
                default:
                    return false;
            }}) {
            @Override
            public void accept(char c, QuotientState state) {
                boolean sign = c == '-'; // set the sign
                switch(state.getCurrentPart()) {
                    case START:
                        if(!state.hasValue()) {
                            // set the sign and transition to first sign
                            state.sign = sign;
                            state.transition(QuotientPart.FIRST_SIGN);
                            break;
                        }
                    case FIRST_SIGN:
                        // first item is now seen
                        state.transition(QuotientPart.FIRST_ITEM);
                    case FIRST_ITEM:
                        state.transition(QuotientPart.INTEGER);
                        state.sign = sign;
                        state.transition(QuotientPart.SECOND_SIGN);
                }
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
                switch(state.getCurrentPart()) {
                    case START:
                    case FIRST_SIGN:
                        state.transition(QuotientPart.FIRST_ITEM);
                    case SECOND_SIGN:
                    case FIRST_ITEM:
                        state.transition(QuotientPart.NUMERATOR);
                        break;
                    default:
                        throw new AParserException();
                }
            }
        },
        WHITESPACE(CharBiPredicate.from(Character::isWhitespace)) {
            @Override
            public void accept(char c, QuotientState state) {
                switch(state.getCurrentPart()) {
                    case DENOMINATOR:
                        // DENOMINATOR => END
                        // "123 / 457 "
                        //           ^
                        // transition to the end and stop
                        state.transition(QuotientPart.END);
                        state.stop();
                        break;
                    case START:
                    case FIRST_SIGN:
                        if(state.hasValue()) {
                            // {START|FIRST_SIGN} => FIRST_ITEM
                            // "123 / 457"
                            //     ^
                            // "123 "
                            //     ^
                            state.transition(QuotientPart.FIRST_ITEM);
                        }
                }
            }
        },
        OTHER((c, t) -> true) {
            @Override
            public void accept(char c, QuotientState state) {
                if(!state.hasValue())
                    // cannot end with no value
                    throw new AParserException();
                switch(state.getCurrentPart()) {
                    case START:
                    case FIRST_SIGN:
                        // {START|FIRST_SIGN} => FIRST_ITEM => INTEGER => END.
                        // "123)"
                        //     ^
                        // "±123)"
                        //      ^
                        state.transition(QuotientPart.FIRST_ITEM);
                    case FIRST_ITEM:
                        // FIRST_ITEM => INTEGER => END
                        // "123 )"
                        //     ^
                        state.transition(QuotientPart.INTEGER);
                    case DENOMINATOR:
                        // transition to the end and stop
                        state.transition(QuotientPart.END);
                        state.stop();
                        break;
                    default:
                        // throw an exception - we shouldn't expect any other characters in a quotient
                        throw new AParserException();

                }
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


    private static final CharAdvancer<QuotientState> QUOTIENT_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                    new CompositeAdvancer<>(QuotientCharType.values()));
    private static QuotientState advance(CharSequence cs) {
        // todo - just add a default method to advancer. Then we can override it for functionality
        //        like below
        QuotientState state = new QuotientState(cs);
        CharAdvancer.runAdvancer(cs, state, QUOTIENT_ADVANCER);
        switch(state.getCurrentPart()) {
            case DENOMINATOR:
            case INTEGER:
                if(state.getPos() == cs.length()) {
                    // we ran through the entire sequence, which ends with DENOMINATOR or INTEGER
                    // transition to the end.
                    state.transition(QuotientPart.END);
                    state.stop();
                }
        }
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
        if(state.getCurrentPart() == QuotientPart.getEndState()) {
            // we use endIdx to handle cases like the following:
            // "123  "
            //      ^ end pos
            return state.endIdx + 1; // result is an exclusive length, so we add one.
        }
        return -1;
    }
}
