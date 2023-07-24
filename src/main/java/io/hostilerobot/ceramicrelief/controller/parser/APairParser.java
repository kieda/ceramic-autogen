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
    /*  '{' K '=' V '}'  '='
     * ^ ^  ^  ^  ^  ^    ^
     * | |  |  |  |  |    SEP (we may traverse back to SEP)
     * | |  |  |  |  GROUP_END
     * | |  |  |  GROUP_VAL
     * | |  |  GROUP_SEP
     * | |  GROUP_KEY
     * | GROUP_START
     * START
     */

    private static sealed class PairDAG<K, V> extends SealedEnum<PairDAG<K, V>> implements DAGAdvancer<PairMatchState<K, V>, PairDAG<K, V>> {
        private final PairDAG<K, V>[] transitions;
        private final PairType type;
        protected PairDAG(PairType type, PairDAG<K, V>... transitions) {
            super(PairDAG.class);
            this.type = type;
            this.transitions = transitions;
        }

        public PairType getType() {
            return type;
        }

        @Override public PairDAG<K, V>[] getTransitions() {
            return transitions;
        }
    }
    private static final class END<K, V> extends PairDAG<K, V> {
        protected END() { super(PairType.DONE); }
        @Override public void onTransition(PairMatchState<K, V> state) {
            // parse the val
            state.parseValue();
        }}
    // K '=' V without association
    private static final class VAL<K, V> extends PairDAG<K, V> {
        protected VAL() { super(PairType.RAW, SealedEnum.getSealedEnum(END.class)); }
        @Override public void onTransition(PairMatchState<K, V> state) {
            // we have encountered the first char of V
            // we look forward and find the value length
            state.findValueIndex();
        }}
    private static final class SEP<K, V> extends PairDAG<K, V> {
        protected SEP() { super(PairType.RAW, SealedEnum.getSealedEnum(VAL.class)); }
        @Override public void onTransition(PairMatchState<K, V> state) {
            if(state.getEnumState() == GROUP_END) {
                // we transition from GROUP_END to SEP
                // meaning that we are in the form { ... } = ...
                //                                         ^ currently here
                // we set the current state's key to Pair(key, value), both of which are parsed.
                if(state instanceof APairParser.PairParseState parseState) {
                    APair<? extends Object, V> newKey = new APair<>(parseState.parsedKey, (ANode<V>)parseState.parsedVal);
                    parseState.parsedKey = newKey; // key is now the initial pair
                    parseState.parsedVal = null;   // val is set to null, and will be parsed later
                }
                // we're not capturing a group anymore
                state.resetEndGroupPos();
            } else {
                // we encountered '='
                // parse and get the key
                state.parseKey();
            }
            state.resetItem(); // reset the item so we can parse the value
        }}
    private static final class KEY<K, V> extends PairDAG<K, V> {
        protected KEY() {super(PairType.RAW, SealedEnum.getSealedEnum(SEP.class));}}
    // '{' K '=' V '}' with grouped association
    private static final class GROUP_END<K, V> extends PairDAG<K, V> {
        // we may traverse back to SEP if we have a pair in the form "{...} = ..."
        // if we encounter anything else besides '=' (including end of file) after GROUP_END, then we end with our position being our last '}'
        protected GROUP_END() {super(PairType.GROUP, SealedEnum.getSealedEnum(SEP.class), SealedEnum.getSealedEnum(END.class));}}
    private static final class GROUP_VAL<K, V> extends PairDAG<K, V> {
        protected GROUP_VAL() {super(PairType.GROUP, SealedEnum.getSealedEnum(GROUP_END.class));}}
    private static final class GROUP_SEP<K, V> extends PairDAG<K, V> {
        protected GROUP_SEP() {super(PairType.GROUP, SealedEnum.getSealedEnum(GROUP_VAL.class));}
        @Override public void onTransition(PairMatchState<K, V> state) {
            // get the key
            state.parseKey();
            state.resetItem(); // reset the item
        }}
    private static final class GROUP_KEY<K, V> extends PairDAG<K, V> {
        protected GROUP_KEY() {super(PairType.GROUP, SealedEnum.getSealedEnum(GROUP_SEP.class));}}
    private static final class GROUP_START<K, V> extends PairDAG<K, V> {
        protected GROUP_START() {super(PairType.GROUP, SealedEnum.getSealedEnum(GROUP_KEY.class));}}
    // starting point
    private static final class START<K, V> extends PairDAG<K, V> {
        protected START() {super(PairType.UNKNOWN, SealedEnum.getSealedEnum(GROUP_START.class), SealedEnum.getSealedEnum(KEY.class));}}

    public static final PairDAG INSTANCE = new PairDAG(null);
    public static final END END = SealedEnum.getSealedEnum(END.class);
    public static final VAL VAL = SealedEnum.getSealedEnum(VAL.class);
    public static final SEP SEP = SealedEnum.getSealedEnum(SEP.class);
    public static final KEY KEY = SealedEnum.getSealedEnum(KEY.class);
    public static final GROUP_END GROUP_END = SealedEnum.getSealedEnum(GROUP_END.class);
    public static final GROUP_VAL GROUP_VAL = SealedEnum.getSealedEnum(GROUP_VAL.class);
    public static final GROUP_SEP GROUP_SEP = SealedEnum.getSealedEnum(GROUP_SEP.class);
    public static final GROUP_KEY GROUP_KEY = SealedEnum.getSealedEnum(GROUP_KEY.class);
    public static final GROUP_START GROUP_START = SealedEnum.getSealedEnum(GROUP_START.class);
    public static final START START = SealedEnum.getSealedEnum(START.class);

    // note that we don't need to do the same thing for pairs in lists, since lists are always delimited with ( and )

    private static sealed class PairCharType<K, V> extends SealedEnum<PairCharType<K, V>> implements CharAdvancer<PairMatchState<K, V>> {
        public static final PairCharType INSTANCE = new PairCharType(c -> false);
        private final CharBiPredicate<PairMatchState<K, V>> match;
        private PairCharType(CharBiPredicate<PairMatchState<K, V>> match) {
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
        public boolean test(char c, PairMatchState<K, V> state) {
            return match.test(c, state);
        }
        @Override
        public void accept(char c, PairMatchState<K, V> state) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class OPEN_PAIR<K, V> extends PairCharType<K, V> {
        OPEN_PAIR() {super('{');}
        @Override
        public void accept(char c, PairMatchState<K, V> state) {
            PairType pairType = state.getEnumState().getType();
            switch (pairType) {
                case GROUP:
                case RAW:
                    if((state.getEnumState() == GROUP_END && pairType.getBaseDepth() == state.getPairDepth() + 1) ||
                            pairType.getBaseDepth() == state.getPairDepth()) {
                        // act like we're encountering another char
                        SealedEnum.getSealedEnum(OTHER.class).accept(c, state);
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
    private final static class SEP_PAIR<K, V> extends PairCharType<K, V> {
        SEP_PAIR() { super('='); }
        private final UnaryOperator<PairDAG<K, V>> transitions = enumState -> switch(enumState) {
            case GROUP_START<K, V> s -> GROUP_KEY;
            // empty key
            // e.g. "{  =123}"
            case GROUP_KEY<K, V> s -> GROUP_SEP;
            // empty key
            // e.g. "=123"
            case START<K, V> s -> KEY;
            case KEY<K, V> s -> SEP;
            case GROUP_END<K, V> g -> SEP; // we have { ... } = ...
            // SEP ends the transition chain, so all chains will terminate
            default -> null; // todo - what about SEP followed by SEP, e.g. a = = b?
        };
        @Override
        public void accept(char c, PairMatchState<K, V> state) {
            PairType pairType = state.getEnumState().getType();

            if((state.getEnumState() == GROUP_END && pairType.getBaseDepth() == state.getPairDepth() + 1) ||
                    pairType.getBaseDepth() == state.getPairDepth()) {
                // transition to next item
                state.runTransition(transitions);
            }
        }
    }

    private final static class CLOSE_PAIR<K, V> extends PairCharType<K, V> {
        CLOSE_PAIR() {super('}');}

        @Override
        public void accept(char c, PairMatchState<K, V> state) {
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
                // transition to GROUP_END
                // however, we don't want to stop just yet, in case this group pair is actually a key for another pair
                // e.g.
                //  { ... } = 123
                //        ^ we are here.
                state.transition(GROUP_END);
                state.captureEndGroupPos(); // this is the position for the end of the group
                state.parseValue(); // parse the value of v for { k = v }
            } else if(newDepth < 0) {
                throw new AParserException("mismatched pair parentheses");
            }
        }
    }
    private final static class WHITESPACE<K, V> extends PairCharType<K, V>{
        WHITESPACE() {super(Character::isWhitespace);}

        @Override
        public void accept(char c, PairMatchState<K, V> state) {
            // do nothing
        }
    }
    private final static class OTHER<K, V> extends PairCharType<K, V>{
        OTHER() {super(c -> true);}

        private final UnaryOperator<PairDAG<K, V>> transitions = enumState -> switch(enumState) {
            // note that each result will be mapped to a value not in the matrix
            // so it will map to null after one transition.
            case START<K, V> s -> KEY;
            case SEP<K, V> s -> VAL;
            case GROUP_START<K, V> s -> GROUP_KEY;
            case GROUP_SEP<K, V> s -> GROUP_VAL;
            case GROUP_END<K, V> s -> END;
            default -> null;
        };

        @Override
        public void accept(char c, PairMatchState<K, V> state) {
            state.runTransition(transitions);
            if(state.getEnumState() == END) {
                if(!state.isStopped())
                    state.stop();
                return;
            }

            PairType pairType = state.getEnumState().getType();
            final int baseDepth = pairType == PairType.UNKNOWN ? 0 :
                    pairType.getBaseDepth();
            if(state.getPairDepth() == baseDepth) {
                state.encounterValueChar(c);
            }
        }
    }

    private static class PairMatchState<K, V> extends DAGState<PairMatchState<K, V>, PairDAG<K, V>> {
        // look forward to find the end position of the value
        // if we are using GROUP type then this will remain -1
        private int valueIndex = -1;
        private int endGroupPos = -1;
        // the end of this pair when we're in group form { ... }
        // note that this might not be the actual end of the pair if we have { ... } = ...

        private void resetEndGroupPos() {
            endGroupPos = -1;
        }
        private void captureEndGroupPos() {
            endGroupPos = getPos() + 1;
        }
        private int getEndPos() {
            return Math.max(endGroupPos, valueIndex);
        }
        private int pairDepth;

        private final CharSequence base;
        private final List<AParser<? extends V>> rawValParsers;

        private int itemBegin = -1;
        private int itemEnd = -1;

        private final List<AParser<? extends K>> groupKeyParsers;
        private final List<AParser<? extends V>> groupValParsers;
        private final List<AParser<? extends K>> rawKeyParsers;

        private PairMatchState(CharSequence base,
                               List<AParser<? extends K>> groupKeyParsers, List<AParser<? extends V>> groupValParsers,
                               List<AParser<? extends K>> rawKeyParsers, List<AParser<? extends V>> rawValParsers) {
            super(START);
            this.base = base;
            pairDepth = 0;
            this.groupKeyParsers = groupKeyParsers;
            this.groupValParsers = groupValParsers;
            this.rawKeyParsers = rawKeyParsers;
            this.rawValParsers = rawValParsers;
        }

        @Override
        protected void encounterValueChar(char c) { // match
            super.encounterValueChar(c);

            if(itemBegin >= 0) {
                itemEnd = getPos();
            } else{
                itemEnd = itemBegin = getPos();
            }

            if(getEnumState() == VAL &&
                    (getValueIndex() >= 0 && (getPos() + 1) >= getValueIndex())) {
                // we reached the last index of the value.
                // transition to the end and stop
                transition(END);
                stop();
            }
        }

        protected void resetItem() {
            itemBegin = itemEnd = -1;
        }

        private void noMatch() {
            valueIndex = -1;
            endGroupPos = -1;
            stop();
        }

        /**
         * finds the length of the next parsable value and sets the private variable valueLength
         */
        private int findValueIndex() {
            if(valueIndex >= 0)
                return valueIndex;
            if(getEnumState().getType() != PairType.RAW)
                throw new IllegalStateException("Should not be finding value index of non-RAW pairs");

            // this should only occur when we're a RAW type, but
            int startIndex = getPos();
            CharSequence value = base.subSequence(startIndex, base.length());
            for(AParser<? extends V> vParser : rawValParsers) {
                int matchIdx = vParser.match(value);
                if(matchIdx >= 0) {
                    valueIndex = matchIdx + startIndex;
                    return valueIndex;
                }
            }

            noMatch();
            return -1;
        }

        protected void parseKey() {
            CharSequence target = (itemBegin | itemEnd) < 0 ?
                    SmallCharSequence.make() : base.subSequence(itemBegin, itemEnd + 1);
            List<AParser<? extends K>> keyParsers = switch (getEnumState().getType()) {
                case GROUP -> groupKeyParsers;
                case RAW -> rawKeyParsers;
                default -> throw new IllegalStateException("parsing Key while not in " + PairType.GROUP + " or " + PairType.RAW + " state.");
            };

            foundMatch: {
                for (AParser<? extends K> kP : keyParsers) {
                    int size = kP.match(target);
                    if (size >= 0 && size == target.length()) {
                        break foundMatch; // we found a match
                    }
                }
                // otherwise we have no match on the key, stop parsing
                noMatch();
            }
        }

        protected void parseValue() {
            int start = itemBegin;
            int end;

            int valIdx = getValueIndex();
            if(valIdx >= 0) {
                end = valIdx;
            } else {
                end = itemEnd + 1;
            }
            CharSequence target = (itemBegin|itemEnd) < 0 ?
                    SmallCharSequence.make() : base.subSequence(start, end);

            List<AParser<? extends V>> valParsers = switch(getEnumState().getType()) {
                case GROUP -> groupValParsers;
                case RAW ->  rawValParsers;
                default -> throw new IllegalStateException("parsing Val while not in " + PairType.GROUP + " or " + PairType.RAW + " state.");
            };
            foundMatch: {
                for (AParser<? extends V> vP : valParsers) {
                    int size = vP.match(target);
                    if (size >= 0 && size + start == end) {
                        break foundMatch; // we found a match
                    }
                }
                // no match found - stop parsing.
                noMatch();
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

    private static class PairParseState<K, V> extends PairMatchState<K, V> {

        private PairParseState(CharSequence base,
                               List<AParser<? extends K>> groupKeyParsers, List<AParser<? extends V>> groupValParsers,
                               List<AParser<? extends K>> rawKeyParsers, List<AParser<? extends V>> rawValParsers) {
            super(base, groupKeyParsers, groupValParsers, rawKeyParsers, rawValParsers);
        }

        private ANode<? extends K> parsedKey = null;
        private ANode<? extends V> parsedVal = null;

        @Override
        protected void parseKey() {
            CharSequence target = (super.itemBegin|super.itemEnd) < 0 ?
                    SmallCharSequence.make() : super.base.subSequence(super.itemBegin, super.itemEnd + 1);
            List<AParser<? extends K>> keyParsers = switch(getEnumState().getType()) {
                case GROUP -> super.groupKeyParsers;
                case RAW ->  super.rawKeyParsers;
                default -> throw new IllegalStateException("parsing Key while not in " + PairType.GROUP + " or " + PairType.RAW + " state.");
            };
            for(AParser<? extends K> kP : keyParsers) {
                int size = kP.match(target);
                if(size >= 0) {
                    if(size != target.length())
                        throw new AParserException(
                                "parser %s did not find a full match \"%s\" in \"%s\":%d:%d".formatted(kP.getClass().getSimpleName(),
                                        target, super.base, super.itemBegin, super.itemEnd + 1)
                        );

                    parsedKey = kP.parse(target);
                    return;
                }
            }
            throw new AParserException(
                    "could not parse \"%s\" in \"%s\":%d:%d".formatted(target, super.base, super.itemBegin, super.itemEnd)
            );
        }

        @Override
        protected void parseValue() {
            int start = super.itemBegin;
            int end;

            int valIdx = super.getValueIndex();
            if(valIdx >= 0) {
                end = valIdx;
            } else {
                end = super.itemEnd + 1;
            }
            CharSequence target = (super.itemBegin|super.itemEnd) < 0 ?
                    SmallCharSequence.make() : super.base.subSequence(start, end);

            List<AParser<? extends V>> valParsers = switch(getEnumState().getType()) {
                case GROUP -> super.groupValParsers;
                case RAW ->  super.rawValParsers;
                default -> throw new IllegalStateException("parsing Val while not in " + PairType.GROUP + " or " + PairType.RAW + " state.");
            };
            for(AParser<? extends V> vP : valParsers) {
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

    private final List<AParser<? extends K>> groupKeyParsers;
    private final List<AParser<? extends V>> groupValParsers;
    private final List<AParser<? extends K>> rawKeyParsers;
    private final List<AParser<? extends V>> rawValParsers;

    public APairParser(List<AParser<? extends K>> groupKeyParsers, List<AParser<? extends V>> groupValParsers,
                       List<AParser<? extends K>> rawKeyParsers, List<AParser<? extends V>> rawValParsers) {
        this.groupKeyParsers = groupKeyParsers;
        this.groupValParsers = groupValParsers;
        this.rawKeyParsers = rawKeyParsers;
        this.rawValParsers = rawValParsers;
    }


    // combined with commentAdvancer and listMatcher
    //   so we can have lists in pairs (a = b, c = d) = (e = f, g = h)
    //   such that we split into K:"(a = b, c = d)", V:"(e = f, g = h)"
    //   rather than K:"(a"  V:"b, c = d) = (e = f, g = h)"
    //   this also permits comments anywhere in this string
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, ChainedAdvancerState<AListParser.ListMatchState, PairMatchState>>> PAIR_MATCH_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                    AListParser.buildListMatcher(new CompositeAdvancer<>(PairCharType.INSTANCE.values())));

    private static <K, V> PairMatchState<K, V> advance(PairMatchState<K, V> state, CharSequence cs) {
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
        } if(!state.isStopped() && state.getEnumState() == GROUP_END) {
            state.transition(END);
            state.stop();
        }
        return state;
    }

    @Override
    public APair<K, V> parse(CharSequence cs) {
        /*
         * 123 456 {(678=123) = 456} = 123 hij abc = def
         *         ^                     ^
         *         start                 end
         * {abc = def} hij = mon
         * {abc = def} section: hij = mon
         *
         * make sure when we parse
         * -invalid 456 = 123
         * ^ start here, nothing matches. we enter
         * that when we parse the value here, we throw an exception to show that there is no match for pairs.
         * this is then handled later on signifying we couldn't find a match in general.
         * asfd: 123 = asdf (properties handled first)
         *
         */
        // we want to do this in two phases.
        // phase 1. traverse { and } (ignoring comments, lists, and whitespace) till we get to either
        //          a.) an '=' sign at depth 0
        //          b.) the end of the chars.
        // if we reach a.) we can easily get the bounds of the key. We find the bounds of the val by parsing next
        // if we either don't have a complete match on key or value, we try and parse again but using "{" and "}" as beginning and end
        PairParseState<K, V> parseState = new PairParseState<>(cs, groupKeyParsers, groupValParsers, rawKeyParsers, rawValParsers);
        advance(parseState, cs);
        return new APair<>((ANode<K>)parseState.parsedKey, (ANode<V>)parseState.parsedVal);
    }

    @Override
    public int match(CharSequence cs) {
        PairMatchState<K, V> matchState = new PairMatchState<>(cs,
                groupKeyParsers, groupValParsers,
                rawKeyParsers, rawValParsers);
        advance(matchState, cs);
        if(matchState.getEnumState() == END) {
            return matchState.getEndPos();
        }
        return -1;
    }
}
