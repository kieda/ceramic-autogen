package io.hostilerobot.ceramicrelief.controller.parser;


import com.sun.net.httpserver.Filter;
import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.ast.APair;
import io.hostilerobot.ceramicrelief.controller.ast.NodePair;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.AdvancerDAG;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.ChainedAdvancerState;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CharAdvancer;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CompositeAdvancer;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.util.List;


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
// todo - a section should not require a list of pairs. We could just have it require a list of values and have pairs as values
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
    private enum PairDAG implements AdvancerDAG<PairMatchState, PairDAG> {
        END(PairType.DONE) {
            @Override
            public void onTransition(PairMatchState state) {
                // parse the val
                state.parseValue();
            }
        },
        // K '=' V without association
            VAL(PairType.RAW, END) {
                @Override
                public void onTransition(PairMatchState state) {
                    // we have encountered the first char of V
                    // we look forward and find the value length
                    state.findValueIndex();
                }
            },
            SEP(PairType.RAW, VAL)  {
                @Override
                public void onTransition(PairMatchState state) {
                    // we encountered '='
                    // parse and get the key
                    state.parseKey();
                }
            },
            KEY(PairType.RAW, SEP),

        // '{' K '=' V '}' with grouped association
            GROUP_END(PairType.GROUP, END),
            GROUP_VAL(PairType.GROUP, GROUP_END),
            GROUP_SEP(PairType.GROUP, GROUP_VAL) {
                @Override
                public void onTransition(PairMatchState state) {
                    // get the key
                    state.parseKey();
                }
            },
            GROUP_KEY(PairType.GROUP, GROUP_SEP),
            GROUP_START(PairType.GROUP, GROUP_KEY),
        // start position
        START(PairType.UNKNOWN, GROUP_START, KEY);
        private final PairDAG[] transitions;
        private final PairType type;
        PairDAG(PairType type, PairDAG... transitions) {
            this.type = type;
            this.transitions = transitions;
        }

        public PairType getType() {
            return type;
        }

        @Override
        public PairDAG[] getTransitions() {
            return transitions;
        }
    }

    // note that we don't need to do the same thing for pairs in lists, since lists are always delimited with ( and )

    // boolean in_list
    // if not in_list then PairCharType
    // if start_list or end_list then OTHER
    // otherwise do nothing
    private enum PairCharType implements CharAdvancer<PairMatchState> {
        OPEN_PAIR('{') {
            @Override
            public void accept(char c, PairMatchState state) {
                PairType pairType = state.getDAG().getType();
                switch (pairType) {
                    case GROUP:
                    case RAW:
                        if(pairType.getBaseDepth() == state.getPairDepth()) {
                            state.encounterValueChar(c);
                        }
                        break;
                    case UNKNOWN:
                        state.transition(PairDAG.GROUP_START);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                // go in depth
                state.pushPairDepth();
            }
        },
        SEP_PAIR('=') {
            @Override
            public void accept(char c, PairMatchState state) {
                PairType pairType = state.getDAG().getType();

                if(pairType.getBaseDepth() == state.getPairDepth()) {
                    // transition to next item
                    switch(state.getDAG()) {
                        case GROUP_START:
                            // empty key
                            state.transition(PairDAG.GROUP_KEY);
                        case GROUP_KEY:
                            state.transition(PairDAG.GROUP_SEP);
                            break;
                        case START:
                            // empty key
                            // e.g. "=123"
                            state.transition(PairDAG.KEY);
                        case KEY:
                            state.transition(PairDAG.SEP);
                            break;
                    }
                }
            }
        },
        CLOSE_PAIR('}') {
            @Override
            public void accept(char c, PairMatchState state) {
                int newDepth = state.popPairDepth();
                PairType pairType = state.getDAG().getType();
                if(newDepth == pairType.getBaseDepth()) {
                    // this is part of a value at base depth
                    state.encounterValueChar(c);
                } else if(pairType == PairType.GROUP
                        && newDepth == pairType.getBaseDepth() - 1) {
                    if(state.getDAG() == PairDAG.GROUP_SEP) {
                        // occurs in the following scenario:
                        // "{ K =  }"
                        // since we never find a valueChar for V, we have an incomplete transition
                        state.transition(PairDAG.GROUP_VAL);
                    }
                    // transition to end and stop
                    state.transition(PairDAG.GROUP_END);
                    state.transition(PairDAG.END);
                    state.stop();
                } else if(newDepth < 0) {
                    throw new AParserException("mismatched pair parentheses");
                }
            }
        },
        WHITESPACE(Character::isWhitespace) {
            @Override
            public void accept(char c, PairMatchState state) {
                // do nothing
            }
        },
        OTHER(c -> true) {
            @Override
            public void accept(char c, PairMatchState state) {
                switch(state.getDAG()) {
                    case START:
                        state.transition(PairDAG.KEY);
                        break;
                    case SEP:
                        state.transition(PairDAG.VAL);
                        break;
                    case GROUP_START:
                        state.transition(PairDAG.GROUP_KEY);
                        break;
                    case GROUP_SEP:
                        state.transition(PairDAG.GROUP_VAL);
                        break;
                }
                PairType pairType = state.getDAG().getType();
                final int baseDepth = pairType == PairType.UNKNOWN ? 0 :
                        pairType.getBaseDepth();
                if(state.getPairDepth() == baseDepth) {
                    state.encounterValueChar(c);
                }
            }
        };

        private final CharBiPredicate<PairMatchState> match;
        PairCharType(char flag) {
            this(CharBiPredicate.from(flag));
        }
        PairCharType(CharPredicate match) {
            this(CharBiPredicate.from(match));
        }
        PairCharType(CharBiPredicate<PairMatchState> match) {
            this.match = match;
        }

        @Override
        public boolean test(char c, PairMatchState state) {
            return match.test(c, state);
        }
    }

    private static class PairMatchState<V> extends ACommentParser.CommentState {
        // look forward to find the end position of the value
        // if we are using GROUP type then this will remain -1
        private int valueIndex = -1;

        private int pairDepth;
        private PairDAG dag;

        private final CharSequence base;
        private final List<AParser<V>> valParsers;

        public PairMatchState(CharSequence base, List<AParser<V>> valParsers) {
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
            if(getDAG() == PairDAG.VAL &&
                    (getValueIndex() > 0 && getPos() >= getValueIndex())) {
                // we reached the last index of the value.
                // transition to the end and stop
                transition(PairDAG.END); 
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

        private PairDAG getDAG() {
            return dag;
        }

        private int getValueIndex() {
            return valueIndex;
        }

        private void transition(PairDAG next) {
            if(!dag.isValidTransition(next))
                throw new AParserException();
            next.onTransition(this);
            dag = next;
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
                    AListParser.buildListMatcher(new CompositeAdvancer<>(PairCharType.values())));

    private static <V> PairMatchState<V> advance(PairMatchState<V> state, CharSequence cs) {
        CharAdvancer.runAdvancer(cs,
                 ChainedAdvancerState.chain(new ACommentParser.CommentState(), new AListParser.ListMatchState(), state),
                PAIR_MATCH_ADVANCER);
        if(!state.isStopped() && state.getDAG() == PairDAG.SEP) {
            // may occur with the following:
            // "K '='  "
            //        ^
            //        EOF/end of section
            // we advance to VAL then to END
            state.transition(PairDAG.VAL);
            state.transition(PairDAG.END);
            state.stop();
        }
        return state;
    }

    @Override
    public APair<K, V> parse(CharSequence cs) {
        PairParseState<K, V> parseState = new PairParseState<>(cs, keyParsers, valParsers);
        advance(parseState, cs);
        return new APair<>(parseState.parsedKey, parseState.parsedVal);
    }

    @Override
    public int match(CharSequence cs) {
        PairMatchState<V> matchState = new PairMatchState<>(cs, valParsers);
        advance(matchState, cs);
        if(matchState.getDAG() == PairDAG.END) {
            int valueIndex = matchState.valueIndex;
            // return the valueIndex if we're a RAW pair, otherwise we know pos ends at '}'
            // and this is included in our match.
            return (valueIndex >= 0 ? valueIndex : matchState.getPos()) + 1;
        }
        return -1;
    }
}
