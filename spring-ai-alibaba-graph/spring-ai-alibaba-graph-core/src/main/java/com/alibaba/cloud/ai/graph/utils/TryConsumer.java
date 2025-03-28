/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
