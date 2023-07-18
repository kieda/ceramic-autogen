package io.hostilerobot.ceramicrelief.controller.parser.advancer;


import io.hostilerobot.sealedenum.SealedEnum;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SealedEnumAdvancer<T extends AdvancerState, Q extends SealedEnum<Q> & CharAdvancer<T>, D extends AdvancerState> extends CompositeAdvancer<ChainedAdvancerState<T, D>>{

    private static <T extends AdvancerState, Q extends SealedEnum<Q> & CharAdvancer<T>, D extends AdvancerState> CharAdvancer<ChainedAdvancerState<T, D>>[]
    makeMapping(List<Q> enumValues, Map<Q, CharAdvancer<D>> mapping) {
        CharAdvancer<ChainedAdvancerState<T, D>>[] advancers = new CharAdvancer[enumValues.size()];
        for(int i = 0; i < enumValues.size(); i++) {
            Q entry = enumValues.get(i);
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
    public SealedEnumAdvancer(List<Q> sealedEnumValues, Map<Q, CharAdvancer<D>> mapping) {
        super(makeMapping(sealedEnumValues, mapping));
    }
}
