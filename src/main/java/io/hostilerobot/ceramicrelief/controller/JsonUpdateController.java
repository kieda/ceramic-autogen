package io.hostilerobot.ceramicrelief.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * uses Jackson's merge to update an existing object.
 *
 * We can also specify a "Target" object at the beginning that the updated data will be dumped into
 *
 * @param <T>
 */
public class JsonUpdateController<T> extends JsonDataController<T> {
    private T current;

    public T getCurrent() {
        return current;
    }

    protected JsonUpdateController(Class<T> clazz) {
        this(clazz, null);
    }

    protected JsonUpdateController(Class<T> clazz, T target) {
        super(clazz);
        this.current = target;
    }



    public static class Builder<T> extends JsonDataController.Builder<T> {
        public Builder<T> setTarget(T target) {
            ((JsonUpdateController<T>)getController()).current = target;
            return this;
        }

        @Override
        public Builder<T> addListener(Consumer<T> listener) {
            return (Builder<T>) super.addListener(listener);
        }

        protected Builder(final Class<T> clazz) {
            super(() -> new JsonUpdateController<>(clazz));
        }
        protected Builder(final Class<T> clazz, T target) {
            super(() -> new JsonUpdateController<>(clazz, target));
        }
    }

    public static <T> Builder<T> builder(Class<T> clazz){
        return new Builder<>(clazz);
    }
    public static <T> Builder<T> builder(Class<T> clazz, T target) {
        return new Builder<>(clazz, target);
    }

    @Override
    public T transform(InputStream input) throws IOException {
        var updated = current;
        if(updated == null) {
            updated = super.transform(input);
        } else {
            updated = mapper.readerForUpdating(updated)
                    .readValue(input);
        }
        current = updated;
        return updated;
    }
}
