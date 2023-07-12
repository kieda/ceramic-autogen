package io.hostilerobot.ceramicrelief.util;

import com.codepoetics.protonpack.StreamUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.list.UnmodifiableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * constructs enum-like instances for a sealed class. We keep a thread-safe static map to track enum instances
 * and to guarantee there is only one instance
 */
public class SealedEnum<T extends SealedEnum<T>> implements Comparable<T>{
    // problem: we only want one map at the level of super-class
    // currently this will fill toOrdinal and instances for each subclass, which we don't want to do
    // todo - could we just have SealedEnum myEnum = SealedEnum.of(MySealedEnum.class); -- I like this better. We can also return the same object using this method.
    // myEnum.values()

    // toOrdinal: SealedEnumBase -> (EnumClass -> ordinal:int)
    // instances: SealedEnumBase -> (T list)

    private static final Map<Class<? extends SealedEnum<?>>, SealedEnum<?>> baseToInstance // base enum class to actual instance
            = new Hashtable<>();
    private static final Map<Class<? extends SealedEnum<?>>, Map<Class<? extends SealedEnum<?>>, Integer>> toOrdinal
            = new Hashtable<>();
    private static final Map<Class<? extends SealedEnum<?>>, List<? extends SealedEnum<?>>> instances
            = new Hashtable<>();

    // maps enum subclass to its base class
    // we use this over having a private variable, since we would have to set the private variable after the constructor has completed
    // and there might be constructor logic that requires operations like getValues()
    private static final Map<Class<? extends SealedEnum<?>>, Class<? extends SealedEnum<?>>> toBaseClass
            = new Hashtable<>();

    private final boolean isBase;

    private static <T extends SealedEnum<T>> boolean hasEntry(Class<T> base) {
        return baseToInstance.containsKey(base);
    }
    private static <T extends SealedEnum<T>> void putEntries(Class<T> k, T v,
                                                           Map<Class<? extends SealedEnum<?>>, Integer> ordinals,
                                                           List<T> inst) {

        //java.util.Map<java.lang.Class<? extends T>,java.lang.Integer>
        // cannot be converted to
        // java.util.Map<java.lang.Class<? extends io.hostilerobot.ceramicrelief.util.SealedEnum<?>>,
        // java.lang.Integer>
        baseToInstance.put(k, v);
        toOrdinal.put(k, ordinals);
        instances.put(k, inst);
    }
    private static <T extends SealedEnum<T>> void clearEntries(Class<T> k) {

        //java.util.Map<java.lang.Class<? extends T>,java.lang.Integer>
        // cannot be converted to
        // java.util.Map<java.lang.Class<? extends io.hostilerobot.ceramicrelief.util.SealedEnum<?>>,
        // java.lang.Integer>
        baseToInstance.remove(k);
        toOrdinal.remove(k);
        instances.remove(k);
    }



    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static Set<StackWalker.Option> ALL_OPTIONS = EnumSet.allOf(StackWalker.Option.class);

    /*
         thought experiment on naughty usage:

         public sealed MySealedEnum extends SealedEnum<T>{
            todo -- support multiple constructors HERE.
            protected MySealedEnum() {
                super(MySealedEnum.class)
                SealedEnum badInstance = new SealedEnum(MySealedEnum.class);
                SealedEnum anotherBadInstance = new MySealedEnum();
                    // we might be able to circumvent our security checks :(
            }

            public static final Enum1 extends MySealedEnum{
                public Enum1(otherArgs) {
                    super(otherArgs);
                }
                public Enum1() {
                    this(someStaticFunction());
                    new Enum1();
                }
            }

            // we can basically follow the type hierarchy in the stack heirarchy and ensure it's an exact match
            // after that we ensure that it's SealedEnum, then back to base

            stacktrace:
               MySealedEnum (constructor)
               SealedEnum (constructor)
               [reflect] Enum1.newInstance()
               Enum1 (constructor)
                   MySealedEnum (constructor) <Enum1>
                   SealedEnum (constructor) <Enum1>
                      ... stop from checks
                   new Enum1() (constructor) <newInst>
                      MySealedEnum (constructor) <newInst>
                      SealedEnum (constructor) <newInst>, current.
         }
         */

    private final static Class<?>[] CLASS_ARRAY = new Class[0];
    protected SealedEnum(Class<T> base) {
        if(!base.isSealed())
            throw new IllegalArgumentException(base + " is not sealed!");
        // base must directly extend SealedEnum and pass itself as a parameter
        if(base.getSuperclass() != SealedEnum.class) {
            throw new IllegalArgumentException(base + " must directly extend " + getClass().getSimpleName());
        }
        Class<?> caller = StackWalker.getInstance(ALL_OPTIONS).getCallerClass();
        if(caller != base) {
            throw new IllegalArgumentException("caller " + caller + " must be of type " + base);
        }
        if(!base.isAssignableFrom(getClass())) {
            // we must have this instanceof T
            // prevents creating SealedEnum directly
            throw new IllegalArgumentException(getClass() + " is not of type " + base);
        }


        /* there are only two cases where this constructor should be called.
         * 1. by the constructor of the enum class
         *    MyEnum extends SealedEnum<MyEnum> {
         *       public static MyEnum INSTANCE = new MyEnum();
         *    }
         * 2. by this class via reflection
         *       Class<MyEnumCase1>.getDeclaredConstructor().newInstance()
         * we check the stacktrace to ensure the instance was constructed this way
         */
        int superClassCount = 0;

        final Class<?> stopClass = SealedEnum.class.getSuperclass();
        for(Class<?> traverse = getClass(); traverse != stopClass; traverse = traverse.getSuperclass()) {
            superClassCount++;
        }

        isBase = base == getClass();
        Class<?>[] expectedStack;

        if(isBase) {
            expectedStack = new Class[superClassCount];
        } else {
            expectedStack = new Class[superClassCount + 1];
            // last frame of stack should go back to SealedEnum
            expectedStack[superClassCount] = SealedEnum.class;
        }
        int stackIdx = superClassCount;
        for(Class<?> traverse = getClass(); traverse != stopClass; traverse = traverse.getSuperclass()) {
            // fill in the opposite order such that SealedEnum is at 0 and our initial class is at (superClassCount -1)
            expectedStack[(--stackIdx)] = traverse;
        }
        System.out.println(Arrays.toString(expectedStack));

        boolean stacksMatch = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stackFrameStream ->
                StreamUtils.zipWithIndex(stackFrameStream//.limit(expectedStack.length)
                        )
                        .allMatch(stackFrameIndexed -> {

            StackWalker.StackFrame actualFrame = stackFrameIndexed.getValue();
            System.out.println(actualFrame);
            if(stackFrameIndexed.getIndex() >= expectedStack.length)
                return true;
            Class<?> expectedClass = expectedStack[(int) stackFrameIndexed.getIndex()];
            Class<?> actualClass = actualFrame.getDeclaringClass();

            if(expectedClass != actualClass) {
//                throw new IllegalArgumentException(expectedClass + " != " + actualClass);
//                return false;
            }

            try {
                // we find the constructor given our param list
                Class<?>[] args = actualFrame.getMethodType().parameterList().toArray(CLASS_ARRAY);
                return actualClass.getDeclaredConstructor(args) != null;
            } catch(NoSuchMethodException ex) {
                return false;
            }
            /*catch(IllegalAccessException ex)  {
                ex.printStackTrace();
                throw new SecurityException("cannot access constructor " + actualClass.getName() + "." + actualFrame.getMethodName() + actualFrame.getMethodType());
            } */
        }));

        if(!stacksMatch) {
            throw new IllegalArgumentException("Only the base enum " + base + " can be created by the user");
        }


        // if(base == getClass())
        //    -- only permit this constructor to be called once for base
        // else
        //    getClass() is a subclass of the enum. The map should already be generated. If not, someone is attempting to instantiate an enum value directly. prevent this
        //    what if someone instantiates an enum subclass AFTER map entry is filled, not from this class's reflection?
        ok:{
            // isBase && hasEntry then we have already processed base => error
            // isBase && !hasEntry then we should process base since it's the first time => continue
            // !isBase && hasEntry then we should do nothing => OK
            // !isBase && !hasEntry then we are processing subclass too early => error

            // todo - how do we want to deal with sealed classes that are not final?
            //    possibly: build a tree
            boolean hasEntry = hasEntry(base);
            duplicateError: // base should always be created first and exactly once.
            if (isBase && !hasEntry) {
                synchronized (base) {
                    hasEntry = hasEntry(base);
                    // lock and check again.
                    if (!hasEntry) {
                        // if we still don't have the entry, then create all subclasses
                        Class<?>[] permitted = base.getPermittedSubclasses();
                        final int enumCount = permitted.length;
                        Map<Class<? extends SealedEnum<?>>, Integer> ordinalsForBase = new HashMap<>(enumCount);
                        List<T> instancesForBase = new ArrayList<>(enumCount);

                        // optimistically place entries. revert later if there's a problem
                        putEntries(base, (T)this, ordinalsForBase, instancesForBase);
                        int ordinal = 0;
                        try {
                            for (; ordinal < enumCount; ordinal++) {
                                Class<? extends T> subClass = (Class<? extends T>) permitted[ordinal];
                                if(!Modifier.isFinal(subClass.getModifiers())) {
                                    // todo - consider how we could utilize non-final subclasses
                                    //        and perhaps build a tree (?)
                                    throw new ReflectiveOperationException("subclasses must be final");
                                }
                                toBaseClass.put(subClass, base);

                                ordinalsForBase.put(subClass, ordinal);
                                Constructor<? extends T> constructor = subClass.getDeclaredConstructor();
                                if (!constructor.canAccess(null) && !constructor.trySetAccessible()) {
                                    throw new SecurityException("cannot access SealedEnum constructor " + constructor);
                                }
                                T enumInstance = constructor.newInstance();
                                instancesForBase.add(enumInstance);
                                ordinalsForBase.put(subClass, ordinal);
                            }
                            break ok;
                        } catch(ReflectiveOperationException ex) {
                            // undo entries
                            clearEntries(base);
                            // undo base class mapping
                            for (int j = 0; j < ordinal; j++) {
                                Class<? extends T> subClass = (Class<? extends T>) permitted[j];
                                toBaseClass.remove(subClass);
                            }

                            throw new IllegalArgumentException("all enums values must have zero-arg accessible constructors", ex);
                        }
                    } else {
                        // put the break explicitly here even though it falls through to exception without it
                        // this case occurs if another thread initialized base in between the lock time
                        // when this shouldn't happen!
                        break duplicateError;
                    }
                }
            } else if (isBase != hasEntry) {
                // if this is the base, then we're attempting to initialize base a second time
                break ok;
            }

            throw new IllegalStateException("attempting to initialize SealedEnum " + base + " more than once");
        }
    }

    public final List<T> values() {
        Class<?> base = isBase ? getClass() : toBaseClass.get(getClass());
        // todo - possibly cache value on base instance
        // todo - add in isBaseClass as a final member
        return (List<T>)UnmodifiableList.unmodifiableList(instances.get(base));
    }
    public final int ordinal() {
        if(isBase)
            return -1;
        else
            return toOrdinal.get(toBaseClass.get(getClass())).get(getClass());
    }

    public final T instance(Class<? extends T> clazz) {
        Class<?> base = isBase ? getClass() : toBaseClass.get(getClass());
        // these casts will pass given the guarantee from the constructor
        if(clazz == base)
            return (T)this;
        else
            return (T)instances.get(base).get(toOrdinal.get(base).get(clazz));
    }

    public static <T> T getSealedEnum(Class<? extends T> clazz) {
        // returns the cached enum (base class)
        return (T)baseToInstance.get(clazz);
    }

    @Override
    public final int compareTo(T o) {
        if(o == null)
            return 1; // other is less than this
        return Integer.compare(this.ordinal(), o.ordinal());
    }

    @Override
    public boolean equals(Object other) {
        return this == other; // only one instance :)
    }

    public final int hashCode() {
        return super.hashCode();
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public final String toString() {
        if(isBase) {
            return "Base<" + getClass().getSimpleName() + ">";
        } else {
            return getClass().getSimpleName();
        }
    }
}
