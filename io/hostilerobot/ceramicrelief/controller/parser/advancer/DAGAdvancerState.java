package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import io.hostilerobot.ceramicrelief.controller.parser.AParserException;

public class DAGAdvancerState<S extends DAGAdvancerState<S, X>, X extends Enum<X> & AdvancerDAG<S, X>> extends AdvancerState {
    private X enumState;
    public DAGAdvancerState(X startState) {
        this.enumState = startState;
    }
    public void transition(X next) {
        if(!enumState.isValidTransition(next))
            throw new AParserException();
        next.onTransition((S)this);
        enumState = next;
    }

    public X getEnumState() {
        return enumState;
    }
}

// example case:
// private enum QuotientDAG implements AdvancerDAG<QuotientState, QuotientDAG>
// private static class QuotientState extends AdvancerState
// public void onTransition(QuotientState state)

//
