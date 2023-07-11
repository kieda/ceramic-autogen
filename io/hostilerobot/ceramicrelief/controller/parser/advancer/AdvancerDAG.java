package io.hostilerobot.ceramicrelief.controller.parser.advancer;

// helper class to keep track of the state and possible transitions while advancing
// best when extending an enum, see: AQuotientParser

// todo - have an explicit AdvancerState that implements transitional methods.
//   now that we have ChainedAdvancerState we are more free to compose with multiple DAGs.
// we shy away from having an explicit AdvancerState that implements transitional methods,
// as an Advancer may have multiple DAGs from its inheiritance chain. Thus it's handled manually
//     unless we want to make some sort of DAG map
public interface AdvancerDAG<S extends DAGAdvancerState<S, X>, X extends Enum<X> & AdvancerDAG<S, X>> {
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
