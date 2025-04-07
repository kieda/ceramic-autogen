package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractDataController<T> implements DataController<T> {
    private List<Consumer<T>> listeners;
    @SafeVarargs
    public AbstractDataController(Consumer<T>... listeners) {
        this(Arrays.asList(listeners));
    }
    public AbstractDataController(List<Consumer<T>> listeners) {
        this.listeners = new ArrayList<>(listeners);
    }

    public void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void notify(T data) {
        for(var listener : listeners) {
            listener.accept(data);
        }
    }

    @Override
    public void update(InputStream input) throws IOException {
        if(listeners == null) {
            throw new IllegalStateException("Controller has been released");
        }
        T data = transform(input);
        notify(data);
    }

    @Override
    public void release() {
        listeners = null;
    }
}
