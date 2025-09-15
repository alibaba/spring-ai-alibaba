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
package com.alibaba.cloud.ai.graph.async;

import com.alibaba.cloud.ai.graph.async.internal.reactive.GeneratorPublisher;
import com.alibaba.cloud.ai.graph.async.internal.reactive.GeneratorSubscriber;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Provides methods for converting between {@link FlowGenerator} and various
 * {@link Flow.Publisher} types.
 *
 * @since 3.0.0
 */
public interface FlowGenerator {

	/**
	 * Creates an {@code AsyncGenerator} from a {@code Flow.Publisher}.
	 * @param <T> the type of item emitted by the publisher
	 * @param <P> the type of the publisher
	 * @param publisher the publisher to subscribe to for retrieving items asynchronously
	 * @param mapResult function that will set generator's result
	 * @return an {@code AsyncGenerator} that emits items from the publisher
	 */
	@SuppressWarnings("unchecked")
	static <T, P extends Flow.Publisher<T>, R> AsyncGenerator<T> fromPublisher(P publisher, Supplier<R> mapResult) {
		var queue = new LinkedBlockingQueue<AsyncGenerator.Data<T>>();
		return new GeneratorSubscriber<>(publisher, (Supplier<Object>) mapResult, queue);
	}

	/**
	 * Creates an {@code AsyncGenerator} from a {@code Flow.Publisher}.
	 * @param <T> the type of item emitted by the publisher
	 * @param <P> the type of the publisher
	 * @param publisher the publisher to subscribe to for retrieving items asynchronously
	 * @return an {@code AsyncGenerator} that emits items from the publisher
	 */
	static <T, P extends Flow.Publisher<T>> AsyncGenerator<T> fromPublisher(P publisher) {
		return fromPublisher(publisher, null);
	}

	/**
	 * Converts an {@code AsyncGenerator} into a {@code Flow.Publisher}.
	 * @param <T> the type of elements emitted by the publisher
	 * @param generator the async generator to convert
	 * @return a flow publisher
	 */
	static <T> Flow.Publisher<T> toPublisher(AsyncGenerator<T> generator) {
		return new GeneratorPublisher<>(generator);
	}

}
