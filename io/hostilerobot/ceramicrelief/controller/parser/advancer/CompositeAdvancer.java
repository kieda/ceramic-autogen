package io.hostilerobot.ceramicrelief.controller.parser.advancer;

public class CompositeAdvancer<T extends AdvancerState> implements CharAdvancer<T> {
    private CharAdvancer<T>[] advancers;
    public CompositeAdvancer(CharAdvancer<T>[] advancers) {
        this.advancers = advancers;
    }

    @Override
    public void accept(char c, T state) {
        for(CharAdvancer<T> a : advancers) {
            if(a.test(c, state)) {
                a.accept(c, state);
                return;
            }
        }
    }

    @Override
    public boolean test(char c, T state) {
        for(CharAdvancer<T> a : advancers) {
            if(a.test(c, state)) {
                return true;
            }
        }
        return false;
    }
}
