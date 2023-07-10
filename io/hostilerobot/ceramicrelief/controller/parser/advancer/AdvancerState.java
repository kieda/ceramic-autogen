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

    /* handles */
    protected void clearValue() { hasValue = false; }
    protected void increasePos() { pos++; }

    /* events */
    protected void encounterValueChar(char c) {
        // used to distinguish empty list () vs single list (abc)
        // vs list like the following (,asdf) (two elements)
        if (!hasValue) {
            hasValue = true;
        }
    }

    protected void stop() { stop = true; }


    /* accessors */
    public boolean isStopped() { return stop; }
    public int getPos() { return pos; }

    public boolean hasValue() {
        return hasValue;
    }
}
