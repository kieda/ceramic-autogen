package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DataController<T> implements TextController {
    private T current; // represents the current data stored
    private List<Consumer<T>> listeners; // listeners to notify
    private final DataProcessor<T> processor; // process where we transform or modify data

    private boolean alwaysNotify = false;
        // when false, only sends notifications to listeners when there has been a substantive change
        // from the old to the new. When true, always send notifications to listeners on notify
    private boolean mergeExisting = true; // if true, we use DataProcessor.merge

    private DataController(T target,
                           boolean alwaysNotify,
                           boolean mergeExisting,
                           List<Consumer<T>> listeners,
                           DataProcessor<T> processor) {
        this.current = target;
        this.alwaysNotify = alwaysNotify;
        this.mergeExisting = mergeExisting;
        this.listeners = listeners;
        this.processor = processor;
    }

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

        boolean wasUpdated;
        T data;
        if(mergeExisting && current != null) {
            wasUpdated = processor.merge(current, input);
            data = current;
        } else {
            var previous = current;
            data = processor.transform(input);
            wasUpdated = !Objects.deepEquals(previous, data);
            current = data;
        }
        if(alwaysNotify || wasUpdated) {
            notify(data);
        }
    }

    @Override
    public void release() {
        listeners = null;
    }

    public static <T> Builder<T> builder(DataProcessor<T> processor) {
        return new Builder<>(processor);
    }
    public static <T> Builder<T> builder(T target, DataProcessor<T> processor){
        return new Builder<>(processor).setTarget(target);
    }
    public final static class Builder<T>{
        private T target = null; // represents the current data stored
        private final List<Consumer<T>> listeners; // listeners to notify
        private final DataProcessor<T> processor; // process where we transform or modify data

        // when false, only sends notifications to listeners when there has been a substantive change
        // from the old to the new. When true, always send notifications to listeners on notify
        private boolean alwaysNotify = false;

        // if true set, we will disable merging
        private boolean disableMerging = false;

        private Builder(DataProcessor<T> processor) {
            this.processor = Objects.requireNonNull(processor);
            this.listeners = new ArrayList<>();
        }

        public Builder<T> setTarget(T target) {
            this.target = target;
            return this;
        }

        /**
         * disables merging, only actually disables functionality if processor.mergeSupported()
         * If !processor.mergeSupported(), we will not be doing merging anyway
         */
        public Builder<T> disableMerging() {
            this.disableMerging = true;
            return this;
        }

        public Builder<T> setAlwaysNotify(boolean alwaysNotify) {
            this.alwaysNotify = alwaysNotify;
            return this;
        }

        public Builder<T> addListener(Consumer<T> listener) {
            this.listeners.add(listener);
            return this;
        }

        @SafeVarargs
        public final Builder<T> addListener(Consumer<T>... listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
            return this;
        }

        public DataController<T> build(InputStream in) throws IOException{
            boolean mergeExisting = !disableMerging && processor.mergeSupported();
            var result =  new DataController<>(target, alwaysNotify, mergeExisting, listeners, processor);
            result.update(in);
            return result;
        }
    }
}
