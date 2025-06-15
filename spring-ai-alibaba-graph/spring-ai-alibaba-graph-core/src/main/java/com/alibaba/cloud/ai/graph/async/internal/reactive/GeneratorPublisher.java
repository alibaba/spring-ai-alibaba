package com.alibaba.cloud.ai.graph.async.internal.reactive;

import com.alibaba.cloud.ai.graph.async.AsyncGenerator;

import java.util.concurrent.Flow;

/**
 * A {@code GeneratorPublisher} is a {@link Flow.Publisher} that generates items from an
 * asynchronous generator.
 *
 * @param <T> the type of items to be published
 */
public class GeneratorPublisher<T> implements Flow.Publisher<T> {

	private final AsyncGenerator<? extends T> delegate;

	/**
	 * Constructs a new <code>GeneratorPublisher</code> with the specified async
	 * generator.
	 * @param delegate The async generator to be used by this publisher.
	 */
	public GeneratorPublisher(AsyncGenerator<? extends T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Subscribes the provided {@code Flow.Subscriber} to this signal. The subscriber
	 * receives initial subscription, handles asynchronous data flow, and manages any
	 * errors or completion signals.
	 * @param subscriber The subscriber to which the signal will be delivered.
	 */
	@Override
	public void subscribe(Flow.Subscriber<? super T> subscriber) {
		subscriber.onSubscribe(new Flow.Subscription() {
			/**
			 * Requests more elements from the upstream Publisher.
			 *
			 * <p>
			 * The Publisher calls this method to indicate that it wants more items. The
			 * parameter {@code n} specifies the number of additional items requested.
			 * @param n the number of items to request, a count greater than zero
			 */
			@Override
			public void request(long n) {
			}

			/**
			 * Cancels the operation.
			 * @throws UnsupportedOperationException if the method is not yet implemented.
			 */
			@Override
			public void cancel() {
				throw new UnsupportedOperationException("cancel is not implemented yet!");
			}
		});

		delegate.forEachAsync(subscriber::onNext).thenAccept(value -> {
			subscriber.onComplete();
		}).exceptionally(ex -> {
			subscriber.onError(ex);
			return null;
		}).join();
	}

}