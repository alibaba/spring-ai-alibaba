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

import java.util.function.Function;

@FunctionalInterface
public interface TryFunction<T, R, Ex extends Throwable> extends Function<T, R> {

	org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TryFunction.class);

	R tryApply(T t) throws Ex;

	default R apply(T t) {
		try {
			return tryApply(t);
		}
		catch (Throwable ex) {
			log.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
	}

	static <T, R, Ex extends Throwable> Function<T, R> Try(TryFunction<T, R, Ex> function) {
		return function;
	}

}
