package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface DataController<T> extends TextController {
    public T transform(InputStream input) throws IOException;
    public void notify(T data);

    @Override
    public default void update(InputStream input) throws IOException {
        T data = transform(input);
        notify(data);
    }
}
