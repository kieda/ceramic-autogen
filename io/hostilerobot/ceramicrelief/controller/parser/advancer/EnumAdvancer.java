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
 * @param <Base> the state that we'll be using, subclass of T
 */
public class EnumAdvancer<T extends AdvancerState, Q extends Enum<Q> & CharAdvancer<T>, Base extends T> extends CompositeAdvancer<Base>{

    private static <T extends AdvancerState, Q extends Enum<Q> & CharAdvancer<T>, Base extends T> CharAdvancer<Base>[]
            makeMapping(Q[] enumValues, EnumMap<Q, CharAdvancer<Base>> mapping) {
        CharAdvancer<Base>[] advancers = new CharAdvancer[enumValues.length];
        for(int i = 0; i < enumValues.length; i++) {
            Q entry = enumValues[i];
            advancers[i] = new CharAdvancer<>() {
                @Override
                public void accept(char c, Base state) {
                    entry.accept(c, state);
                    CharAdvancer<Base> delegate = mapping.get(entry);
                    if(delegate != null) {
                        if (delegate.test(c, state))
                            delegate.accept(c, state);
                    }
                }

                @Override
                public boolean test(char c, Base state) {
                    return entry.test(c, state);
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
    public EnumAdvancer(Q[] enumValues, EnumMap<Q, CharAdvancer<Base>> mapping) {
        super(makeMapping(enumValues, mapping));
    }
}
