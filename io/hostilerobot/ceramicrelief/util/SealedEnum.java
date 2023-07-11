package io.hostilerobot.ceramicrelief.util;

import java.lang.reflect.InvocationTargetException;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * constructs enum-like instances for a sealed class
 */
public class SealedEnum<T extends SealedEnum<T>> {
    // problem: we only want one map at the level of super-class
    // currently this will fill toOrdinal and instances for each subclass, which we don't want to do
    // todo - make map static?
    private final Map<Class<? extends T>, Integer> toOrdinal;
    private final List<T> instances;
    public SealedEnum(Class<T> base) {
        if(!base.isSealed())
            throw new IllegalArgumentException(base + " is not sealed!");
        // base must directly extend SealedEnum and pass itself as a parameter
        if(base.getSuperclass() != SealedEnum.class) {
            throw new IllegalArgumentException();
        }
        Class<?>[] permitted = base.getPermittedSubclasses();
        try {
            toOrdinal = new HashMap<>();
            instances = new ArrayList<>();

            for(int ordinal = 0; ordinal < permitted.length; ordinal++) {
                Class<? extends T> subClass = (Class<? extends T>) permitted[ordinal];
                toOrdinal.put(subClass, ordinal);
                instances.add(subClass.getConstructor().newInstance());
            }
        } catch(ReflectiveOperationException ex) {
            throw new IllegalArgumentException("all enums values must have zero-arg accessible constructors", ex);
        }
    }

    public final int ordinal(Class<? extends T> clazz) {
        return toOrdinal.get(clazz);
    }

    public final T instance(Class<? extends T> clazz) {
        return instances.get(ordinal(clazz));
    }

}
