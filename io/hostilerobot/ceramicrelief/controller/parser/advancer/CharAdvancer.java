package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import io.hostilerobot.ceramicrelief.controller.parser.AListParser;
import io.hostilerobot.ceramicrelief.util.chars.CharBiConsumer;
import io.hostilerobot.ceramicrelief.util.chars.CharBiPredicate;

public interface CharAdvancer<T extends AdvancerState> extends CharBiPredicate<T>, CharBiConsumer<T> {
    void accept(char c, T state);
    boolean test(char c, T state);

    public static <S extends AdvancerState> void runAdvancer(CharSequence cs, S state, CharAdvancer<S> advancer) {
        for(int pos; (pos = state.getPos()) < cs.length() && !state.isStopped(); state.increasePos()) {
            char c = cs.charAt(pos);
            if(advancer.test(c, state)) {
                advancer.accept(c, state);
            }
        }
    }
}
