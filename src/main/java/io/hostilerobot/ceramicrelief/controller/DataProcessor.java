package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;

public interface DataProcessor<T> {
    /**
     * Transforms the inputstream in into a datatype T
     */
    public T transform(InputStream in) throws IOException;

    /**
     * Merges the data from the input stream into current, modifying the value.
     * Returns true if the data was meaningfully updated
     */
    public default boolean merge(T current, InputStream in) throws IOException {
        throw new UnsupportedOperationException(getClass().getName() + " has not implemented merge functionality");
    }
    public default boolean mergeSupported() {
        return false;
    }
}
