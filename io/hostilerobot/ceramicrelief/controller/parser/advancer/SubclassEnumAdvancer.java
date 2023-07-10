package io.hostilerobot.ceramicrelief.controller.parser.advancer;

import java.util.EnumMap;

/**
 * chains two advancers where one advancer is a subclass of the other advancer.
 * Permits the whole advancer to be of the subclass type
 */
public class SubclassEnumAdvancer<T extends AdvancerState, Q extends Enum<Q> & CharAdvancer<T>, Base extends T> extends CompositeAdvancer<Base>{

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
    public SubclassEnumAdvancer(Q[] enumValues, EnumMap<Q, CharAdvancer<Base>> mapping) {
        super(makeMapping(enumValues, mapping));
    }
}
