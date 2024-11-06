package com.alibaba.cloud.ai.graph.utils;

import java.util.function.Consumer;

@FunctionalInterface
public interface TryConsumer<T, Ex extends Throwable> extends Consumer<T> {

	org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TryConsumer.class);

	void tryAccept(T t) throws Ex;

	default void accept(T t) {
		try {
			tryAccept(t);
		}
		catch (Throwable ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
	}

	static <T, Ex extends Throwable> Consumer<T> Try(TryConsumer<T, Ex> consumer) {
		return consumer;
	}

}
