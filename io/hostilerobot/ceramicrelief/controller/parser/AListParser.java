package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AList;
import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CharAdvancer;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CompositeAdvancer;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.EnumAdvancer;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;
import io.hostilerobot.ceramicrelief.util.chars.CharPredicate;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.List;

// AList:
// X is resulting value of Y
// list of items are nodes of type Y which result in X
// Y[] getItems() : <Y extends ANode<X>>[] getValue()

public class AListParser<X, Y extends ANode<X>> implements AParser<Y[], AList<X, Y>>{
    private final List<AParser<X, Y>> parsers;

    public AListParser(List<AParser<X, Y>> parsers) {
        // we can add 'this' with List<AParser<?, ?>>
        this.parsers = parsers;
    }

    private enum ListCharType implements CharAdvancer<ListMatchState>{
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
        CLOSE_LIST(')') {
            @Override
            public void accept(char c, ListMatchState state) {
                int newDepth = state.popDepth();
                switch(newDepth) {
                    case 0:
                        // increase count if we have a value or encountered ','
                        // covers cases (), (asdf), and (,) to have 0, 1, and 2 values respectively
                        if(state.getCount() > 0 || state.hasValue())
                            state.increaseCount();
                        state.stop();
                        return;
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
        OTHER(c -> true) {
            @Override
            public void accept(char c, ListMatchState state) {
                if(state.getDepth() == 1) {
                    state.encounterValueChar(c);
                }
            }
        };
        private final CharBiPredicate<ListMatchState> match;
        ListCharType(char flag) {
            this(c -> c == flag);
        }
        ListCharType(CharPredicate match) {
            this((c, f) -> match.test(c));
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
    private static final CharAdvancer<ListMatchState> LIST_ADVANCER =
            ACommentParser.buildCommentAdvancer(
                new CompositeAdvancer<>(ListCharType.values()));
    private static final CharAdvancer<ListParseState> PARSE_ADVANCER;
    static {
        EnumMap<ListCharType, CharAdvancer<ListParseState>> map = new EnumMap<>(ListCharType.class);
        map.put(ListCharType.ITEM_SEP, new CharAdvancer<>() {
            @Override
            public void accept(char c, ListParseState state) {
                if(state.getDepth() == 1) {
                    state.onItemEnd();
                }
            }

            @Override
            public boolean test(char c, ListParseState state) {
                return true;
            }
        });
        map.put(ListCharType.CLOSE_LIST, new CharAdvancer<>() {
            @Override
            public void accept(char c, ListParseState state) {
                if(state.getDepth() == 0 && state.getCount() > 0) {
                    state.onItemEnd();
                }
            }

            @Override
            public boolean test(char c, ListParseState state) {
                return true;
            }
        });
        PARSE_ADVANCER = new EnumAdvancer<>(ListCharType.values(), map);
    }


    private static class ListMatchState extends ACommentParser.CommentAdvancerState{
        private int depth;
        private int count;

        public ListMatchState() {
            depth = 0;
            count = 0;
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
            super.stop();
        }
        @Override
        protected void increasePos() {
            super.increasePos();
        }

        /* accessors */
        public int getDepth() { return depth; }
        public int getCount() { return count; }
    }

    public static class ListParseState<X, Y extends ANode<X>> extends ListMatchState{
        private int itemBegin = -1;
        private int itemEnd = -1;
        private final CharSequence base;
        private final List<AParser<X, Y>> parsers;
        private final Y[] items;
        public ListParseState(CharSequence base, Y[] items, List<AParser<X, Y>> parsers) {
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
            CharSequence itemSequence = itemBegin == itemEnd ?
                    SmallCharSequence.make() : // empty sequence
                    base.subSequence(itemBegin, itemEnd + 1); // end is exclusive, but our indexing is inclusive
            for(int parserIdx = 0; parserIdx < parsers.size(); parserIdx++) {
                AParser<X, Y> parser = parsers.get(parserIdx);
                int matchLen = parser.match(itemSequence);
                if(matchLen >= 0) {
                    Y node = parser.parse(itemSequence);
                    items[getCount() - 1] = node;
                    break;
                }
            }
            itemBegin = itemEnd = -1;
        }
    }

    @Override
    public int match(CharSequence cs) {
        // first char must be '('
        ListMatchState state = new ListMatchState();
        if(cs.isEmpty() || !ListCharType.OPEN_LIST.test(cs.charAt(0), state))
            return -1;

        CharAdvancer.runAdvancer(cs, state, LIST_ADVANCER);

        // if we don't end on depth 0 then our list must have some errors
        if(state.getDepth() == 0)
            return state.getPos();
        else
            return -1;
    }


    @Override
    public AList<X, Y> parse(CharSequence cs) {
        ListMatchState matchState = new ListMatchState();
        CharAdvancer.runAdvancer(cs, matchState, LIST_ADVANCER);
        // items for each in the count
        Y[] items = (Y[]) new Object[matchState.getCount()];

        // traverse again but now get the charsequences for each item
        ListParseState<X, Y> parseState = new ListParseState<>(cs, items, parsers);
        CharAdvancer.runAdvancer(cs, parseState, PARSE_ADVANCER);

        AList<X, Y> result = new AList<>(items);
        return result;
    }
}
