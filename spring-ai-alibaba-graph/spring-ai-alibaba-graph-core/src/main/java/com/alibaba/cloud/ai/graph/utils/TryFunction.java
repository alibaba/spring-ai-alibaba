package com.alibaba.cloud.ai.graph.utils;

import java.util.function.Function;

@FunctionalInterface
public interface TryFunction<T, R, Ex extends Throwable> extends Function<T,R> {
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TryFunction.class);

    R tryApply( T t ) throws Ex;

    default R apply( T t ) {
        try {
            return tryApply(t);
        } catch (Throwable ex) {
            log.error( ex.getMessage(), ex );
            throw new RuntimeException(ex);
        }
    }

    static <T,R,Ex extends Throwable> Function<T,R> Try( TryFunction<T,R,Ex> function ) {
        return function;
    }
}
