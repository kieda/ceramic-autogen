package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;

public interface TextController {
    /**
     * Gives a new input stream to read from when there are modifications in the data that should be notified to the controller
     * @param input the new stream of data to update
     */
    public void update(InputStream input) throws IOException;

    /**
     * called when this controller is being shut down or when the controller is deleted
     */
    public default void release(){};
}
