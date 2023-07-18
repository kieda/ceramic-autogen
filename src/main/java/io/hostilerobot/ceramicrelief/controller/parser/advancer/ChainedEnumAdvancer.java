package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import java.util.EnumMap;

/**
 * todo - it's possible to get rid of inheiritance muck by having a ChainedAdvancerState<P extends AdvancerState, Q extends AdvancerState>
 *        where DelegateAdvancerState has fields passed in through constructor P pState, Q qState.
 *        then, we use the Enum's test/accept on pState, and the map's test/accept on qState
 *        thus we will be favoring composition over inheiritance and that's pretty neat
 *        WE SHOULD MAKE THE CHANGE NOW - states are kinda stepping on each other's toes
 * @param <T> the state that the enum uses
 * @param <Q> the enum type
 * @param <D> the delegate state
 */
public class ChainedEnumAdvancer<T extends AdvancerState, Q extends Enum<Q> & CharAdvancer<T>, D extends AdvancerState> extends CompositeAdvancer<ChainedAdvancerState<T, D>>{

    private static <T extends AdvancerState, Q extends Enum<Q> & CharAdvancer<T>, D extends AdvancerState> CharAdvancer<ChainedAdvancerState<T, D>>[]
            makeMapping(Q[] enumValues, EnumMap<Q, CharAdvancer<D>> mapping) {
        CharAdvancer<ChainedAdvancerState<T, D>>[] advancers = new CharAdvancer[enumValues.length];
        for(int i = 0; i < enumValues.length; i++) {
            Q entry = enumValues[i];
            advancers[i] = new CharAdvancer<>() {
                @Override
                public void accept(char c, ChainedAdvancerState<T, D> state) {
                    entry.accept(c, state.getA());
                    CharAdvancer<D> delegate = mapping.get(entry);
                    if(delegate != null) {
                        if (delegate.test(c, state.getB()))
                            delegate.accept(c, state.getB());
                    }
                }

                @Override
                public boolean test(char c, ChainedAdvancerState<T, D> state) {
                    return entry.test(c, state.getA());
                }
            };
        }

        return advancers;
    }

    // allows us to chain advancers defined by enums in the following manner while advancing:
    //     if(enum.test(...)) {
    //         enum.accept(...);
    //         if(mapping.test(...)) mapping.accept(...)
    //     }
    // for example, this is useful to enter a comment mode and ignore all other advancers, then go back
    // to our original advancer when the comment is completed
    public ChainedEnumAdvancer(Q[] enumValues, EnumMap<Q, CharAdvancer<D>> mapping) {
        super(makeMapping(enumValues, mapping));
    }
}
