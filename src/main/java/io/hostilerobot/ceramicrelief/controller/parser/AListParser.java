package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AList;
import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.*;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

// AList:
// X is resulting value of Y
// list of items are nodes of type Y which result in X
// Y[] getItems() : <Y extends ANode<X>>[] getValue()

public class AListParser<X> implements AParser<ANode<X>[]>{
    private final List<AParser<? extends X>> parsers;

    public AListParser(List<AParser<? extends X>> parsers) {
        // we can add 'this' with List<AParser<?, ?>>
        this.parsers = parsers;
    }

    private enum ListCharType implements CharAdvancer<ListMatchState>{
        START_LIST('(', s -> s.getDepth() == 0) {
            @Override
            public void accept(char c, ListMatchState state) {
                state.pushDepth();
            }
        },
        OPEN_LIST('(') {
            @Override
            public void accept(char c, ListMatchState state) {
                if(state.getDepth() == 1) {
                    // first item is '(', which we don't consider as a new found value
                    // since it belongs to this list.
                    // in fact we can do this for any item that is not at depth 1
                    state.encounterValueChar(c);
                }
                state.pushDepth();
            }
        },
        END_LIST(')', s ->
                s.getDepth() == 1) {
            @Override
            public void accept(char c, ListMatchState state) {
                // increase count if we have a value or encountered ','
                // covers cases (), (asdf), and (,) to have 0, 1, and 2 values respectively
                if(state.getCount() > 0 || state.hasValue())
                    state.increaseCount();
                state.popDepth(); // depth -> 0
                state.stop();
            }
        },
        CLOSE_LIST(')') {
            @Override
            public void accept(char c, ListMatchState state) {
                int newDepth = state.popDepth();
                switch(newDepth) {
                    case 1:
                        state.encounterValueChar(c);
                        return;
                    default:
                        if(newDepth < 0) {
                            throw new AParserException("mismatched parentheses");
                        }
                }
            }
        },
        ITEM_SEP(',') {
            @Override
            public void accept(char c, ListMatchState state) {
                if(state.getDepth() == 1) {
                    // item separator is not counted in the item itself, so we don't call encounterValueChar
                    state.increaseCount();
                }
            }
        },
        WHITESPACE(Character::isWhitespace) {
            // no-op
            @Override
            public void accept(char c, ListMatchState state) {}
        },
        IN_LIST((c, s) -> s.getDepth() > 0) {
            @Override
            public void accept(char c, ListMatchState state) {
                if(state.getDepth() == 1) {
                    state.encounterValueChar(c);
                }
            }
        },
        OUT_OF_LIST((c, s) -> s.getDepth() == 0) {
            @Override
            public void accept(char c, ListMatchState state) {}
        };
        private final CharBiPredicate<ListMatchState> match;
        ListCharType(char flag) {
            this(CharBiPredicate.from(flag));
        }
        ListCharType(CharPredicate match) {
            this(CharBiPredicate.from(match));
        }
        ListCharType(char flag, Predicate<ListMatchState> match) {
            this(CharBiPredicate.from(flag, match));
        }
        ListCharType(CharBiPredicate<ListMatchState> match) {
            this.match = match;
        }

        @Override
        public boolean test(char c, ListMatchState state) {
            return match.test(c, state);
        }
    }

    // advances the list but automatically ignores comments
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, ListMatchState>> LIST_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                new CompositeAdvancer<>(ListCharType.values()));
    private static final CharAdvancer<ChainedAdvancerState<ACommentParser.CommentState, ListParseState>> PARSE_ADVANCER;

    static {
        EnumMap<ListCharType, CharAdvancer<ListParseState>> map = new EnumMap<>(ListCharType.class);
        map.put(ListCharType.ITEM_SEP, new CharAdvancer<>() {
            @Override
            public void accept(char c, ListParseState state) {
                state.onItemEnd();
            }

            @Override
            public boolean test(char c, ListParseState state) {
                return state.getDepth() == 1;
            }
        });
        map.put(ListCharType.END_LIST, new CharAdvancer<>() {
            @Override
            public void accept(char c, ListParseState state) {
                state.onItemEnd();
            }

            @Override
            public boolean test(char c, ListParseState state) {
                return state.getDepth() == 0 && state.getCount() > 0;
            }
        });
        PARSE_ADVANCER = ACommentParser.buildCommentAdvancer(new SubclassEnumAdvancer<>(ListCharType.values(), map));
    }

    public static <S extends AdvancerState> CharAdvancer<ChainedAdvancerState<ListMatchState, S>> buildListMatcher(CharAdvancer<S> whileOutOfList) {
        return buildListMatcher(whileOutOfList, whileOutOfList, whileOutOfList, null);
    }

    public static <S extends AdvancerState> CharAdvancer<ChainedAdvancerState<ListMatchState, S>> buildListMatcher(
            CharAdvancer<S> onListStart,
            CharAdvancer<S> onListEnd,
            CharAdvancer<S> whileOutOfList,
            CharAdvancer<S> whileInList) {
        EnumMap<ListCharType, CharAdvancer<S>> map = new EnumMap<>(ListCharType.class);
        map.put(ListCharType.START_LIST, onListStart);
        map.put(ListCharType.END_LIST, onListEnd);
        map.put(ListCharType.OUT_OF_LIST, whileOutOfList);
        if(whileInList != null) {
            map.put(ListCharType.OPEN_LIST, whileInList);
            map.put(ListCharType.CLOSE_LIST, whileInList);
            map.put(ListCharType.ITEM_SEP, whileInList);
            map.put(ListCharType.IN_LIST, whileInList);
        }

        return new ChainedEnumAdvancer<>(ListCharType.values(), map);
    }


    static class ListMatchState extends AdvancerState {
        private final boolean stopOnListEnd;
        private int depth;
        private int count;

        // internal constructor
        private ListMatchState(boolean stopOnListEnd) {
            this.stopOnListEnd = stopOnListEnd;
            depth = 0;
            count = 0;
        }
        public ListMatchState() {
            this(false);
        }

        /* handles */
        private void pushDepth() { depth++; }
        private int popDepth() { return --depth; }
        private void increaseCount() { count++; }
        @Override
        protected void encounterValueChar(char c) {
            super.encounterValueChar(c);
        }
        @Override
        protected void stop() {
            if(stopOnListEnd)
                super.stop();
            else {
                // otherwise reset the state
                depth = 0;
                count = 0;
                super.clearValue();
            }
        }

        @Override
        protected void increasePos() {
            super.increasePos();
        }

        /* accessors */
        public int getDepth() { return depth; }
        public int getCount() { return count; }
    }

    private static class ListParseState<X> extends ListMatchState{
        private int itemBegin = -1;
        private int itemEnd = -1;
        private final CharSequence base;
        private final List<AParser<? extends X>> parsers;
        private final ANode<? extends X>[] items;
        private ListParseState(CharSequence base, ANode<X>[] items, List<AParser<? extends X>> parsers) {
            super(true); // only used internally. Thus we can just stop as usual
            this.base = base;
            this.parsers = parsers;
            this.items = items;
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

        protected void onItemEnd() {
            // called when we encounter a ',' or the last parentheses in the list
            // itemBegin and itemEnd are both -1 in certain cases, e.g. (,)
            CharSequence itemSequence = (itemBegin|itemEnd) < 0 ?
                    SmallCharSequence.make() : // empty sequence
                    base.subSequence(itemBegin, itemEnd + 1); // end is exclusive, but our indexing is inclusive
            for(int parserIdx = 0; parserIdx < parsers.size();) {
                AParser<? extends X> parser = parsers.get(parserIdx);
                int matchLen = parser.match(itemSequence);
                if(matchLen >= 0) {
                    ANode<? extends X> node = parser.parse(itemSequence);
                    if(node.ignore()) {
                        // if we are ignoring this node, we want to move the beginning forward by the chars occupied
                        // by the ignored node and continue from 0
                        itemSequence = itemSequence.subSequence(matchLen, itemSequence.length());
                        parserIdx = 0;
                        continue;
                    } else {
                        items[getCount() - 1] = node;
                        break;
                    }
                }

                parserIdx++;
            }
            itemBegin = itemEnd = -1;
        }
    }

    @Override
    public int match(CharSequence cs) {
        // first char must be '('
        ListMatchState state = new ListMatchState(true);
        if(cs.isEmpty() || !ListCharType.OPEN_LIST.test(cs.charAt(0), state))
            return -1;
        CharAdvancer.runAdvancer(cs, ChainedAdvancerState.chain(
                new ACommentParser.CommentState(),
                state
        ), LIST_ADVANCER);

        // if we don't end on depth 0 then our list must have some errors
        if(state.getDepth() == 0)
            return state.getPos();
        else
            return -1;
    }


    @Override
    public AList<X> parse(CharSequence cs) {
        ListMatchState matchState = new ListMatchState(true);
        CharAdvancer.runAdvancer(cs, ChainedAdvancerState.chain(new ACommentParser.CommentState(), matchState),
                LIST_ADVANCER);
        // items for each in the count
        ANode<X>[] items = (ANode<X>[]) new ANode[matchState.getCount()];

        // traverse again but now get the charsequences for each item
        ListParseState<X> parseState = new ListParseState<>(cs, items, parsers);
        CharAdvancer.runAdvancer(cs, ChainedAdvancerState.chain(new ACommentParser.CommentState(), parseState),
                PARSE_ADVANCER);

        AList<X> result = new AList<>(items);
        return result;
    }
}
