package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        DataController.super.update(input);
    }

    @Override
    public void release() {
        listeners = null;
    }

    public static class Builder<T>{
        private final AbstractDataController<T> controller;
        protected Builder(Supplier<? extends AbstractDataController<T>> fn) {
            controller = fn.get();
        }
        protected AbstractDataController<T> getController() {
            return controller;
        }
        public Builder<T> addListener(Consumer<T> listener) {
            controller.addListener(listener);
            return this;
        }
        public AbstractDataController<T> build(InputStream initial) throws IOException{
            controller.update(initial);
            return controller;
        }
    }
}
