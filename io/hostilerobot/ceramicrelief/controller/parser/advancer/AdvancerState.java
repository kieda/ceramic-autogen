package io.hostilerobot.ceramicrelief.controller.parser.advancer;

public class AdvancerState {
    private int pos;
    private boolean stop;
    private boolean hasValue;

    public AdvancerState() {
        pos = 0;
        stop = false;
        hasValue = false;
    }

    /* events */
    protected void encounterValueChar(char c) {
        // used to distinguish empty list () vs single list (abc)
        // vs list like the following (,asdf) (two elements)
        if (!hasValue) {
            hasValue = true;
            onValueFound();
        }
    }
    protected void onValueFound() {}


    protected void stop() { stop = true; }
    protected void increasePos() { pos++; }

    /* accessors */
    public boolean isStopped() { return stop; }
    public int getPos() { return pos; }

    public boolean hasValue() {
        return hasValue;
    }
}
