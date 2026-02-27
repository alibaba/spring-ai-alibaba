/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.interceptor.modelretry;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * The model calls a retry interceptor to handle retryable exceptions such as network errors.
 * When a model call fails, it will be retried according to the configured retry policy until it succeeds or the maximum number of retries is reached.
 * Example:
 * <pre>
 * ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
 *     .maxAttempts(3)
 *     .initialDelay(1000)
 *     .maxDelay(10000)
 *     .backoffMultiplier(2.0)
 *     .build();
 * </pre>
 */
public class ModelRetryInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ModelRetryInterceptor.class);

	private final int maxAttempts;
	private final long initialDelay;
	private final long maxDelay;
	private final double backoffMultiplier;
	private final Predicate<Exception> retryableExceptionPredicate;
	private final Predicate<Throwable> retryableThrowablePredicate;

	private ModelRetryInterceptor(Builder builder) {
		this.maxAttempts = builder.maxAttempts;
		this.initialDelay = builder.initialDelay;
		this.maxDelay = builder.maxDelay;
		this.backoffMultiplier = builder.backoffMultiplier;
		this.retryableExceptionPredicate = builder.retryableExceptionPredicate;
		this.retryableThrowablePredicate = this::isRetryableException;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {

		// 1. Initial attempt to call the model
		ModelResponse response = handler.call(request);

		// 2. Handle streaming calls (Flux)
		if (response.getMessage() instanceof Flux<?>) {
			return handleStreamRetry(request, handler, response);
		}

		// 3. Handle blocking calls
		return handleBlockingRetry(request, handler, response);

	}


	/**
	 * Retry logic for blocking (non-streaming) model calls.
	 */
	private ModelResponse handleBlockingRetry(ModelRequest request, ModelCallHandler handler, ModelResponse initialResponse) {
		Exception lastException = null;
		long currentDelay = initialDelay;

		ModelResponse currentResponse = initialResponse;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				// If it is not the first iteration (i.e., this is a retry attempt), re-invoke the handler.
				if (attempt > 1) {
					log.info("Retry model call, on the {}th attempt (out of {} attempts).", attempt, maxAttempts);
					currentResponse = handler.call(request);
				}

				Message message = (Message) currentResponse.getMessage();

				// Check if the response contains any exception information (exceptions captured from AgentLlmNode).
				if (message != null && message.getText() != null && message.getText().startsWith("Exception:")) {
					String exceptionText = message.getText();
					log.warn("The model call returned an exception message: {}", exceptionText);

					// Extract anomaly information from the text and determine whether a retry is possible.
					if (attempt < maxAttempts && isRetryableExceptionMessage(exceptionText)) {
						lastException = new RuntimeException(exceptionText);
						// Wait and try again
						if (currentDelay > 0) {
							try {
								log.info("Retry after {} ms", currentDelay);
								Thread.sleep(currentDelay);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								throw new RuntimeException("Retry interrupted", e);
							}
						}
						// Calculate the next delay time (exponential backoff)
						currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelay);
						continue;
					} else if (attempt >= maxAttempts) {
						log.error("The maximum number of retries has been reached {}, and the model call has failed.", maxAttempts);
						throw new RuntimeException("Model call failed, maximum number of retries reached:" + exceptionText);
					}

					// For non-retryable exceptions, return immediately.
					return currentResponse;
				}

				// Successful response
				if (attempt > 1) {
					log.info("The model call succeeded after the {}th attempt.", attempt);
				}
				return currentResponse;

			} catch (Exception e) {
				lastException = e;
				log.warn("Model call failed (attempted {}/{}): {}", attempt, maxAttempts, e.getMessage());

				if (attempt >= maxAttempts) {
					log.error("The maximum number of retries has been reached {}, and the model call has failed.", maxAttempts);
					throw new RuntimeException("Model call failed, maximum number of retries reached.", lastException);
				}

				// Determine if an exception can be retried.
				if (!retryableExceptionPredicate.test(e)) {
					log.warn("Exceptions cannot be retried and are thrown immediately: {}", e.getMessage());
					throw new RuntimeException("Model call failed (non-retryable exception)", e);
				}

				// Wait and try again
				if (currentDelay > 0) {
					try {
						log.info("Retry after {} ms", currentDelay);
						Thread.sleep(currentDelay);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Retry interrupted", ie);
					}
				}

				// Calculate the next delay time (exponential backoff)
				currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelay);
			}
		}

		// All retries failed.
		throw new RuntimeException("Model call failed, maximum number of retries reached. " + maxAttempts, lastException);
	}

	/**
	 * Streaming call retry logic using Reactor's retry mechanism.
	 * Retries are only triggered for retryable exceptions and if no data has been emitted yet.
	 */
	private ModelResponse handleStreamRetry(ModelRequest request, ModelCallHandler handler, ModelResponse initialResponse) {
		// Flag to track whether the stream has emitted any data
		AtomicBoolean hasOutput = new AtomicBoolean(false);
		AtomicBoolean isFirstAttempt = new AtomicBoolean(true);
		AtomicLong currentDelay = new AtomicLong(initialDelay);

		Flux<ChatResponse> retryableFlux = Flux.defer(() -> {
					// Re-invoke the handler on each subscription (including retries)
					if (isFirstAttempt.compareAndSet(true, false)) {
						return (Flux<ChatResponse>) initialResponse.getMessage();
					}
					// Subsequent subscriptions (i.e., retries) will trigger a fresh handler.call
					ModelResponse newResponse = handler.call(request);
					return (Flux<ChatResponse>) newResponse.getMessage();
				})
				.doOnNext(data -> {
					// Mark as output emitted once the first data chunk is received
					if (!hasOutput.get()) {
						hasOutput.set(true);
					}
				})
				.retryWhen(Retry.from(signals ->
						signals.flatMap(signal -> {
							// 1. Get current retry count (starts from 0, so +1 represents the current attempt number)
							long attempt = signal.totalRetries() + 1;
							Throwable throwable = signal.failure();

							// 2. Check if the maximum number of attempts has been reached
							if (attempt >= maxAttempts) {
								log.error("The maximum number of retries has been reached ({}), and the model call has failed.", maxAttempts);
								return Mono.error(throwable);
							}

							// 3. Business logic: Check if any data has already been emitted
							if (hasOutput.get()) {
								log.error("Stream failed after partial output. Cannot retry to avoid data duplication.");
								return Mono.error(throwable);
							}

							// 4. Business logic: Determine if the exception is retryable (aligned with blocking retryableExceptionPredicate)
							if (!retryableThrowablePredicate.test(throwable)) {
								log.warn("Exception is non-retryable and will be thrown immediately: {}", throwable.getMessage());
								return Mono.error(throwable);
							}

							// 5. Calculate aligned backoff delay (consistent with blocking retry logic)
							currentDelay.set(Math.min((long) (currentDelay.get() * backoffMultiplier), maxDelay));

							log.info("Retrying model call, attempt {}/{}", attempt, maxAttempts);
							log.info("Wait for {} ms before next retry", currentDelay.get());

							// 6. Generate a delay signal to trigger the retry
							return Mono.delay(Duration.ofMillis(currentDelay.get()));
						})
				));

		return new ModelResponse(retryableFlux);
	}

	/**
	 * Determine if the exception message indicates a retryable error.
	 */
	private boolean isRetryableExceptionMessage(String exceptionText) {
		String lowerText = exceptionText.toLowerCase();
		return lowerText.contains("i/o error") ||
				lowerText.contains("remote host terminated") ||
				lowerText.contains("connection") ||
				lowerText.contains("timeout") ||
				lowerText.contains("network") ||
				lowerText.contains("handshake") ||
				lowerText.contains("socket");
	}

	private boolean isRetryableException(Throwable e) {
		return isRetryableExceptionMessage(e.getMessage());
	}

	@Override
	public String getName() {
		return "ModelRetry";
	}

	public static class Builder {
		private int maxAttempts = 3;
		private long initialDelay = 1000;
		private long maxDelay = 30000;
		private double backoffMultiplier = 2.0;
		private Predicate<Exception> retryableExceptionPredicate = Builder::isRetryableException;

		/**
		 * Set the maximum number of retries (including the first call).
		 * @param maxAttempts The maximum number of attempts must be >= 1.
		 */
		public Builder maxAttempts(int maxAttempts) {
			if (maxAttempts < 1) {
				throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
			}
			this.maxAttempts = maxAttempts;
			return this;
		}

		/**
		 * Set the initial retry delay (milliseconds).
		 * @param initialDelay Initial delay time, in milliseconds
		 */
		public Builder initialDelay(long initialDelay) {
			if (initialDelay < 0) {
				throw new IllegalArgumentException("initialDelay must be greater than or equal to 0.");
			}
			this.initialDelay = initialDelay;
			return this;
		}

		/**
		 * Set the maximum retry delay (milliseconds).
		 * @param maxDelay Maximum delay time, in milliseconds
		 */
		public Builder maxDelay(long maxDelay) {
			if (maxDelay < 0) {
				throw new IllegalArgumentException("maxDelay must be greater than or equal to 0.");
			}
			this.maxDelay = maxDelay;
			return this;
		}

		/**
		 * Set the backoff factor (the multiplier for the delay time on each retry).
		 * @param backoffMultiplier The retreat factor must be >= 1.0
		 */
		public Builder backoffMultiplier(double backoffMultiplier) {
			if (backoffMultiplier < 1.0) {
				throw new IllegalArgumentException("The backoffMultiplier must be >= 1.0");
			}
			this.backoffMultiplier = backoffMultiplier;
			return this;
		}

		/**
		 * Configure custom retryable exception handling logic
		 * @param predicate Exception detection function
		 */
		public Builder retryableExceptionPredicate(Predicate<Exception> predicate) {
			this.retryableExceptionPredicate = predicate;
			return this;
		}

		public ModelRetryInterceptor build() {
			return new ModelRetryInterceptor(this);
		}

		/**
		 * Default retryable exception handling logic
		 */
		private static boolean isRetryableException(Exception e) {
			String message = e.getMessage();
			if (message == null) {
				return false;
			}

			String lowerMessage = message.toLowerCase();

			// Network-related exceptions
			if (lowerMessage.contains("i/o error") ||
				lowerMessage.contains("remote host terminated") ||
				lowerMessage.contains("connection") ||
				lowerMessage.contains("timeout") ||
				lowerMessage.contains("handshake") ||
				lowerMessage.contains("socket")) {
				return true;
			}

			// Spring WebClient related exceptions
			if (e.getClass().getName().contains("ResourceAccessException") ||
				e.getClass().getName().contains("WebClientRequestException")) {
				return true;
			}

			// Check the exception type
			Throwable cause = e.getCause();
			while (cause != null) {
				String causeClassName = cause.getClass().getName();
				if (causeClassName.contains("IOException") ||
					causeClassName.contains("SocketException") ||
					causeClassName.contains("ConnectException") ||
					causeClassName.contains("TimeoutException") ||
					causeClassName.contains("SSLException")) {
					return true;
				}
				cause = cause.getCause();
			}

			return false;
		}
	}
}

