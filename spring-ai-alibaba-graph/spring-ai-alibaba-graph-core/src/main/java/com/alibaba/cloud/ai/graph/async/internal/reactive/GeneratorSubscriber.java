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
package com.alibaba.cloud.ai.graph.async.internal.reactive;

import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.AsyncGeneratorQueue;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

/**
 * Represents a subscriber for generating asynchronous data streams.
 *
 * <p>
 * This class implements the {@link Flow.Subscriber} and {@link AsyncGenerator} interfaces
 * to handle data flow and produce asynchronous data. It is designed to subscribe to a
 * publisher, process incoming items, and manage error and completion signals.
 * </p>
 *
 * @param <T> The type of elements produced by this generator.
 */
public class GeneratorSubscriber<T> implements Flow.Subscriber<T>, AsyncGenerator<T> {

	private final AsyncGeneratorQueue.Generator<T> delegate;

	private final Supplier<Object> mapResult;

	public Optional<Supplier<Object>> mapResult() {
		return Optional.ofNullable(mapResult);
	}

	/**
	 * Constructs a new instance of {@code GeneratorSubscriber}.
	 * @param <P> the type of the publisher, which must extend {@link Flow.Publisher}
	 * @param mapResult function that will set generator's result
	 * @param publisher the source publisher that will push data to this subscriber
	 * @param queue the blocking queue used for storing asynchronous generator data
	 */
	public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, Supplier<Object> mapResult,
			BlockingQueue<Data<T>> queue) {
		this.delegate = new AsyncGeneratorQueue.Generator<>(queue);
		this.mapResult = mapResult;
		publisher.subscribe(this);
	}

	/**
	 * Constructs a new instance of {@code GeneratorSubscriber}.
	 * @param <P> the type of the publisher, which must extend {@link Flow.Publisher}
	 * @param publisher the source publisher that will push data to this subscriber
	 * @param queue the blocking queue used for storing asynchronous generator data
	 */
	public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, BlockingQueue<Data<T>> queue) {
		this(publisher, null, queue);
	}

	/**
	 * Handles the subscription event from a Flux.
	 * <p>
	 * This method is called when a subscription to the source {@link Flow} has been
	 * established. The provided {@code Flow.Subscription} can be used to manage and
	 * control the flow of data emissions.
	 * @param subscription The subscription object representing this resource owner
	 * lifecycle. Used to signal that resources being subscribed to should not be released
	 * until this subscription is disposed.
	 */
	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
	}

	/**
	 * Passes the received item to the delegated queue as an {@link Data} object.
	 * @param item The item to be processed and queued.
	 */
	@Override
	public void onNext(T item) {
		delegate.queue().add(Data.of(item));
	}

	/**
	 * Handles an error by queuing it in the delegate's queue with an errored data.
	 * @param error The Throwable that represents the error to be handled.
	 */
	@Override
	public void onError(Throwable error) {
		delegate.queue().add(Data.error(error));
	}

	/**
	 * This method is called when the asynchronous operation is completed successfully. It
	 * notifies the delegate that no more data will be provided by adding a done marker to
	 * the queue.
	 */
	@Override
	public void onComplete() {
		delegate.queue().add(Data.done(mapResult().map(Supplier::get).orElse(null)));
	}

	/**
	 * Returns the next {@code Data<T>} object from this iteration.
	 * @return the next element in the iteration, or null if there is no such element
	 */
	@Override
	public Data<T> next() {
		return delegate.next();
	}

}
