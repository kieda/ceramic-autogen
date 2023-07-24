package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import io.hostilerobot.ceramicrelief.controller.parser.AParserException;
import io.hostilerobot.sealedenum.SealedEnum;

import java.util.function.UnaryOperator;

public class DAGState<S extends DAGState<S, X>, X extends SealedEnum<X> & DAGAdvancer<S, X>> extends AdvancerState{
    private X enumState;
    public DAGState(X startState) {
        this.enumState = startState;
    }
    public void transition(X next) {
        if(!enumState.isValidTransition(next))
            // todo make this an illegalstateexception
            //   parser exception should only be thrown on invalid user input
            throw new AParserException("invalid transition: " + enumState + " -> " + next);
        next.onTransition((S)this);
        enumState = next;
    }

    /**
     * runs through a series of transitions specified by transitionFn from the current enumState
     * and returns the resulting enumState produced
     *
     * if the transitionFn results in null, then the traversal stops
     *
     * @param transitionFn function from PairDag
     * @return
     */
    public X runTransition(UnaryOperator<X> transitionFn) {
        X current = getEnumState();
        while(current != null) {
            X next = transitionFn.apply(current);
            if(next != null) {
                transition(next);
                current = getEnumState();
            }
            else break;
        }
        return current;
    }

    public X getEnumState() {
        return enumState;
    }
}
