package io.hostilerobot.ceramicrelief.controller.parser;


import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.ast.APair;
import io.hostilerobot.ceramicrelief.controller.ast.NodePair;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;
import io.hostilerobot.sealedenum.SealedEnum;

import java.util.List;
import java.util.function.UnaryOperator;


// values: ADecimal, AList, AName, AQuotient,
// simple [Value1] = [Value2]
// can we have pairs as values?
// [ValueA1] = [ValueA2] = [ValueB1] = [ValueB2]
// this is ambiguous however...
// ([ValueA1] = [ValueA2]) = ([ValueB1] = [ValueB2])
// (([ValueA1] = [ValueA2]) = [ValueB1]) = [ValueB2]
// [ValueA1] = ([ValueA2] = ([ValueB1] = [ValueB2]))
// disambiguate:
//   (([ValueA1] = [ValueA2]) = [ValueB1]) = [ValueB2]
//   XXX -- we might do better disambiguating with this:
//          [ValueA1] = ([ValueA2] = ([ValueB1] = [ValueB2]))
//          since we can easily get first equals (and parse value1)
//          then match V by going through our parsers
//   resolution order:
//     1. ValueA1 : K
//     2. ValueA2 : V
//     3. ValueA1 = ValueA2 : Pair<K, V>
//     4. ValueB1 : V'
//     5. (ValueA1 = ValueA2) = ValueB1 : Pair<<K, V>, V'>
//     6. ValueB2 : V''
//     7. ((ValueA1 = ValueA2) = ValueB1) = ValueB2 : Pair<Pair<<K, V>, V'>, V''>
//
// permit explicit association using brackets '{' and '}'
// todo - have interface method to allow each parser to declare our reserved chars and whether or not they need to be escaped (in a regex)
// todo - change list to use '[' and ']'. Pairs can use '(' and ')'
public class APairParser<K, V> implements AParser<NodePair<K, V>> {
    // format:
    //   '{' K '=' V '}' : GROUP
    //   K '=' V         : RAW

    private enum PairType{
        // items at depth 1 are considered part of values
        GROUP(1),
        // items at depth 0 are considered part of values
        RAW(0),
        UNKNOWN(-1), // we don't know the base depth
        DONE(-1);
        private final int baseDepth;
        PairType(int baseDepth) {
            this.baseDepth = baseDepth;
        }

        public int getBaseDepth() {
            return baseDepth;
        }
    }

    /*  K '=' V
     * ^^  ^  ^ ^
     * ||  |  | END
     * ||  |  VAL
     * ||  SEP
     * |KEY
     * START
     */
    /*  '{' K '=' V '}'
     * ^ ^  ^  ^  ^  ^ ^
     * | |  |  |  |  | END
     * | |  |  |  |  GROUP_END
     * | |  |  |  GROUP_VAL
     * | |  |  GROUP_SEP
     * | |  GROUP_KEY
     * | GROUP_START
     * START
     */

    private static sealed class PairDAG<V> extends SealedEnum<PairDAG<V>> implements DAGAdvancer<PairMatchState<V>, PairDAG<V>> {
        public static final PairDAG INSTANCE = new PairDAG(null);
        private final PairDAG<V>[] transitions;
        private final PairType type;
        protected PairDAG(PairType type, PairDAG<V>... transitions) {
            super(PairDAG.class);
            this.type = type;
            this.transitions = transitions;
        }

        public PairType getType() {
            return type;
        }

        @Override public PairDAG<V>[] getTransitions() {
            return transitions;
        }
    }
    private static final class END<V> extends PairDAG<V> {
        protected END() { super(PairType.DONE); }
        @Override public void onTransition(PairMatchState<V> state) {
            // parse the val
            state.parseValue();
        }}
    public static final END END = SealedEnum.getSealedEnum(END.class);

    // K '=' V without association
    private static final class VAL<V> extends PairDAG<V> {
        protected VAL() { super(PairType.RAW, END); }
        @Override public void onTransition(PairMatchState<V> state) {
            // we have encountered the first char of V
            // we look forward and find the value length
            state.findValueIndex();
        }}
    public static final VAL VAL = SealedEnum.getSealedEnum(VAL.class);

    private static final class SEP<V> extends PairDAG<V> {
        protected SEP() { super(PairType.RAW, VAL); }
        @Override public void onTransition(PairMatchState<V> state) {
            // we encountered '='
            // parse and get the key
            state.parseKey();
        }}
    public static final SEP SEP = SealedEnum.getSealedEnum(SEP.class);

    private static final class KEY<V> extends PairDAG<V> {
        protected KEY() {super(PairType.RAW, SEP);}}
    public static final KEY KEY = SealedEnum.getSealedEnum(KEY.class);

    // '{' K '=' V '}' with grouped association
    private static final class GROUP_END<V> extends PairDAG<V> {
        protected GROUP_END() {super(PairType.GROUP, END);}}
    public static final GROUP_END GROUP_END = SealedEnum.getSealedEnum(GROUP_END.class);

    private static final class GROUP_VAL<V> extends PairDAG<V> {
        protected GROUP_VAL() {super(PairType.GROUP, GROUP_END);}}
    public static final GROUP_VAL GROUP_VAL = SealedEnum.getSealedEnum(GROUP_VAL.class);

    private static final class GROUP_SEP<V> extends PairDAG<V> {
        protected GROUP_SEP() {super(PairType.GROUP, GROUP_VAL);}
        @Override public void onTransition(PairMatchState<V> state) {
            // get the key
            state.parseKey();
        }}
    public static final GROUP_SEP GROUP_SEP = SealedEnum.getSealedEnum(GROUP_SEP.class);

    private static final class GROUP_KEY<V> extends PairDAG<V> {
        protected GROUP_KEY() {super(PairType.GROUP, GROUP_SEP);}}
    public static final GROUP_END GROUP_KEY = SealedEnum.getSealedEnum(GROUP_END.class);

    private static final class GROUP_START<V> extends PairDAG<V> {
        protected GROUP_START() {super(PairType.GROUP, GROUP_KEY);}}
    public static final GROUP_END GROUP_START = SealedEnum.getSealedEnum(GROUP_END.class);
    // starting point
    private static final class START<V> extends PairDAG<V> {
        protected START() {super(PairType.UNKNOWN, GROUP_START, KEY);}}
    public static final START START = SealedEnum.getSealedEnum(START.class);

    // note that we don't need to do the same thing for pairs in lists, since lists are always delimited with ( and )

    private static sealed class PairCharType<V> extends SealedEnum<PairCharType<V>> implements CharAdvancer<PairMatchState<V>> {
        public static final PairCharType INSTANCE = new PairCharType(c -> false);
        private final CharBiPredicate<PairMatchState<V>> match;
        private PairCharType(CharBiPredicate<PairMatchState<V>> match) {
            super(PairCharType.class);
            this.match = match;
        }
        private PairCharType(CharPredicate match){
            this(CharBiPredicate.from(match));
        }

        private PairCharType(char flag) {
            this(CharBiPredicate.from(flag));
        }
        @Override
        public boolean test(char c, PairMatchState<V> state) {
            return match.test(c, state);
        }
        @Override
        public void accept(char c, PairMatchState<V> state) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class OPEN_PAIR<V> extends PairCharType<V> {
        OPEN_PAIR() {super('{');}
        @Override
        public void accept(char c, PairMatchState<V> state) {
            PairType pairType = state.getEnumState().getType();
            switch (pairType) {
                case GROUP:
                case RAW:
                    if(pairType.getBaseDepth() == state.getPairDepth()) {
                        state.encounterValueChar(c);
                    }
                    break;
                case UNKNOWN:
                    state.transition(GROUP_START);
                    break;
                default:
                    throw new IllegalStateException();
            }
            // go in depth
            state.pushPairDepth();
        }
    }
    private final static class SEP_PAIR<V> extends PairCharType<V> {
        SEP_PAIR() { super('='); }
        private final UnaryOperator<PairDAG<V>> transitions = enumState -> switch(enumState) {
            case GROUP_START<V> s -> GROUP_KEY;
            // empty key
            // e.g. "{  =123}"
            case GROUP_KEY<V> s -> GROUP_SEP;
            // empty key
            // e.g. "=123"
            case START<V> s -> KEY;
            case KEY<V> s -> SEP;
            // GROUP_SEP and SEP end the transition chain, so all chains will terminate
            default -> null;
        };
        @Override
        public void accept(char c, PairMatchState<V> state) {
            PairType pairType = state.getEnumState().getType();

            if(pairType.getBaseDepth() == state.getPairDepth()) {
                // transition to next item
                state.runTransition(transitions);
            }
        }
    }

    private final static class CLOSE_PAIR<V> extends PairCharType<V> {
        CLOSE_PAIR() {super('}');}

        @Override
        public void accept(char c, PairMatchState<V> state) {
            int newDepth = state.popPairDepth();
            PairType pairType = state.getEnumState().getType();
            if(newDepth == pairType.getBaseDepth()) {
                // this is part of a value at base depth
                state.encounterValueChar(c);
            } else if(pairType == PairType.GROUP
                    && newDepth == pairType.getBaseDepth() - 1) {


                if(state.getEnumState() == GROUP_SEP) {
                    // occurs in the following scenario:
                    // "{ K =  }"
                    // since we never find a valueChar for V, we have an incomplete transition
                    state.transition(GROUP_VAL);
                }
                // transition to end and stop
                state.transition(GROUP_END);
                state.transition(END);
                state.stop();
            } else if(newDepth < 0) {
                throw new AParserException("mismatched pair parentheses");
            }
        }
    }
    private final static class WHITESPACE<V> extends PairCharType<V>{
        WHITESPACE() {super(Character::isWhitespace);}

        @Override
        public void accept(char c, PairMatchState<V> state) {
            // do nothing
        }
    }
    private final static class OTHER<V> extends PairCharType<V>{
        OTHER() {super(c -> true);}

        private final UnaryOperator<PairDAG<V>> transitions = enumState -> switch(enumState) {
            // note that each result will be mapped to a value not in the matrix
            // so it will map to null after one transition.
            case START<V> s -> KEY;
            case SEP<V> s -> VAL;
            case GROUP_START<V> s -> GROUP_KEY;
            case GROUP_SEP<V> s -> GROUP_VAL;
            default -> null;
        };

        @Override
        public void accept(char c, PairMatchState<V> state) {
            state.runTransition(transitions);
            PairType pairType = state.getEnumState().getType();
            final int baseDepth = pairType == PairType.UNKNOWN ? 0 :
                    pairType.getBaseDepth();
            if(state.getPairDepth() == baseDepth) {
                state.encounterValueChar(c);
            }
        }
    }

    private static class PairMatchState<V> extends DAGState<PairMatchState<V>, PairDAG<V>> {
        // look forward to find the end position of the value
        // if we are using GROUP type then this will remain -1
        private int valueIndex = -1;

        private int pairDepth;

        private final CharSequence base;
        private final List<AParser<V>> valParsers;

        public PairMatchState(CharSequence base, List<AParser<V>> valParsers) {
            super(START);
            this.base = base;
            this.valParsers = valParsers;
            pairDepth = 0;
        }

        /**
         * finds the length of the next parsable value and sets the private variable valueLength
         */
        private int findValueIndex() {
            if(valueIndex >= 0)
                return valueIndex;
            int startIndex = getPos();
            CharSequence value = base.subSequence(startIndex, base.length());
            for(AParser<V> vParser : valParsers) {
                int matchIdx = vParser.match(value);
                if(matchIdx >= 0) {
                    valueIndex = matchIdx + startIndex;
                    return valueIndex;
                }
            }
            return -1;
        }
        protected void parseValue() {}
        protected void parseKey() {}

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
            if(getEnumState() == VAL &&
                    (getValueIndex() > 0 && getPos() >= getValueIndex())) {
                // we reached the last index of the value.
                // transition to the end and stop
                transition(END);
                stop();
            }
        }

        protected void pushPairDepth() {
            pairDepth++;
        }
        protected int popPairDepth() {
            return --pairDepth;
        }
        protected int getPairDepth() {
            return pairDepth;
        }

        private int getValueIndex() {
            return valueIndex;
        }

        @Override
        protected void stop() {
            super.stop();
        }
    }

    private static class PairParseState<K, V> extends PairMatchState<V> {
        private int itemBegin = -1;
        private int itemEnd = -1;

        private final List<AParser<K>> keyParsers;
        private PairParseState(CharSequence base, List<AParser<K>> keyParsers, List<AParser<V>> valParsers) {
            super(base, valParsers);
            this.keyParsers = keyParsers;
        }

        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
            if(itemBegin >= 0) {
                itemEnd = getPos();
            } else{
                itemEnd = itemBegin = getPos();
            }
        }

        private ANode<K> parsedKey = null;
        private ANode<V> parsedVal = null;

        @Override
        protected void parseKey() {
            CharSequence target = itemBegin == itemEnd ?
                    SmallCharSequence.make() : super.base.subSequence(itemBegin, itemEnd);
            for(AParser<K> kP : keyParsers) {
                int size = kP.match(target);
                if(size >= 0) {
                    if(size + itemBegin != itemEnd)
                        throw new AParserException();

                    parsedKey = kP.parse(target);
                    return;
                }
            }
            throw new AParserException();
        }

        @Override
        protected void parseValue() {
            int start = itemBegin;
            int end;

            int valIdx = super.getValueIndex();
            if(valIdx >= 0) {
                end = valIdx;
            } else {
                end = itemEnd;
            }
            CharSequence target = itemBegin == itemEnd ?
                    SmallCharSequence.make() : super.base.subSequence(start, end);

            for(AParser<V> vP : super.valParsers) {
                int size = vP.match(target);
                if(size >= 0) {
                    if(size + start != end)
                        throw new AParserException();
                    parsedVal = vP.parse(target);
                    return;
                }
            }
            throw new AParserException();
        }
    }

    private final List<AParser<K>> keyParsers;
    private final List<AParser<V>> valParsers;

    public APairParser(List<AParser<K>> keyParsers, List<AParser<V>> valParsers) {
        this.keyParsers = keyParsers;
        this.valParsers = valParsers;
    }


    // combined with commentAdvancer and listMatcher
    //   so we can have lists in pairs (a = b, c = d) = (e = f, g = h)
    //   such that we split into K:"(a = b, c = d)", V:"(e = f, g = h)"
    //   rather than K:"(a"  V:"b, c = d) = (e = f, g = h)"
    //   this also permits comments anywhere in this string
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, ChainedAdvancerState<AListParser.ListMatchState, PairMatchState>>> PAIR_MATCH_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                    AListParser.buildListMatcher(new CompositeAdvancer<>(PairCharType.INSTANCE.values())));

    private static <V> PairMatchState<V> advance(PairMatchState<V> state, CharSequence cs) {
        CharAdvancer.runAdvancer(cs,
                 ChainedAdvancerState.chain(new ACommentParser.CommentState(), new AListParser.ListMatchState(), state),
                PAIR_MATCH_ADVANCER);
        if(!state.isStopped() && state.getEnumState() == SEP) {
            // may occur with the following:
            // "K '='  "
            //        ^
            //        EOF/end of section
            // we advance to VAL then to END
            state.transition(VAL);
            state.transition(END);
            state.stop();
        }
        return state;
    }

    @Override
    public APair<K, V> parse(CharSequence cs) {
        // so apparently java can auto-convert APair to APair<K, V> and Node to Node<K> easily
        // compile time errors when manually casting from List<AParser<K>> to List<AParser>
        // what??
        PairParseState<K, V> parseState = new PairParseState<>(cs, keyParsers, valParsers);
        advance(parseState, cs);
        return new APair<>(parseState.parsedKey, parseState.parsedVal);
    }

    @Override
    public int match(CharSequence cs) {
        PairMatchState<V> matchState = new PairMatchState<>(cs, valParsers);
        advance(matchState, cs);
        if(matchState.getEnumState() == END) {
            int valueIndex = matchState.valueIndex;
            // return the valueIndex if we're a RAW pair, otherwise we know pos ends at '}'
            // and this is included in our match.
            return (valueIndex >= 0 ? valueIndex : matchState.getPos()) + 1;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(PairCharType.INSTANCE.values());
    }
}
