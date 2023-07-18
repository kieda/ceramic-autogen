package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import io.hostilerobot.sealedenum.SealedEnum;

public interface DAGAdvancer<S extends DAGState<S, X>, X extends SealedEnum<X> & DAGAdvancer<S, X>> {
    public X[] getTransitions();
    public default void onTransition(S state) {} // default - do nothing. But this can be overridden
    public default boolean isValidTransition(X next) {
        for(X possible : getTransitions()) {
            if(possible == next)
                return true;
        }
        return false;
    }
}
