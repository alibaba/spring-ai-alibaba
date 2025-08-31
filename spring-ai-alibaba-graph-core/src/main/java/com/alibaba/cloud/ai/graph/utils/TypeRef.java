package com.alibaba.cloud.ai.graph.utils;

import java.lang.reflect.*;
import java.util.Optional;

/**
 * This class is inspired by TypeReference from Jackson Library
 * It is used to pass full generics type information, and
 * avoid problems with type erasure (that basically removes most
 * usable type references from runtime Class objects).
 * It is based on ideas from
 * <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html">http://gafter.blogspot.com/2006/12/super-type-tokens.html</a>,
 * Additional idea (from a suggestion made in comments of the article)
 * is to require bogus implementation of <code>Comparable</code>
 * (any such generic interface would do, as long as it forces a method
 * with generic type to be implemented).
 * to ensure that a Type argument is indeed given.
 * <p>
 *  Usage is by sub-classing: here is one way to instantiate reference
 *  to generic type <code>List&lt;Integer></code>:
 * <pre>
 *   var TypeRef = new TypeRef&lt;List&lt;Integer>>() { };
 * </pre>
 * which can be passed to methods that accept TypeReference.
 *
 * @param <T>
 */
public abstract class TypeRef<T> implements Comparable<TypeRef<T>> {
    protected final Type _type;

    protected TypeRef() {
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        } else {
            this._type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
        }
    }

    public Type getType() {
        return this._type;
    }

    @SuppressWarnings("unchecked")
    public Optional<T> cast(Object obj ) {
        return erasureOf(this._type)
                .filter( c -> c.isInstance(obj) )
                .map( c -> (T) obj )
                ;
    }

    public static Optional<Class<?>> erasureOf(Type t) {
        if (t instanceof Class<?> c) {
            return Optional.of(c);
        }
        if (t instanceof ParameterizedType pt) {
            return Optional.of((Class<?>) pt.getRawType());
        }
        if (t instanceof GenericArrayType gat) {
            return erasureOf(gat.getGenericComponentType())
                    .map( comp -> Array.newInstance(comp, 0).getClass() );
        }
        if (t instanceof TypeVariable<?> tv) {
            Type[] bounds = tv.getBounds();
            return bounds.length == 0 ? Optional.empty() : erasureOf(bounds[0]);
        }
        if (t instanceof WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            return upper.length == 0 ? Optional.empty() : erasureOf(upper[0]);
        }
        return Optional.empty();
    }

    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    @Override
    public int compareTo(TypeRef<T> o) {
        return 0;
    }
}