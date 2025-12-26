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
package com.alibaba.cloud.ai.graph.agent.interceptor.modelretry;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;

import org.springframework.ai.chat.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
	
	// Constants for ModelCallLimitHook counters
	private static final String THREAD_COUNT_KEY = "__model_call_limit_thread_count__";
	private static final String RUN_COUNT_KEY = "__model_call_limit_run_count__";

	private final int maxAttempts;
	private final long initialDelay;
	private final long maxDelay;
	private final double backoffMultiplier;
	private final Predicate<Exception> retryableExceptionPredicate;

	private ModelRetryInterceptor(Builder builder) {
		this.maxAttempts = builder.maxAttempts;
		this.initialDelay = builder.initialDelay;
		this.maxDelay = builder.maxDelay;
		this.backoffMultiplier = builder.backoffMultiplier;
		this.retryableExceptionPredicate = builder.retryableExceptionPredicate;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		Exception lastException = null;
		long currentDelay = initialDelay;
		
		// Get RunnableConfig to update Hook counters on retries
		RunnableConfig config = null;
		if (request.getContext() != null && request.getContext().containsKey(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY)) {
			Object configObj = request.getContext().get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY);
			if (configObj instanceof RunnableConfig) {
				config = (RunnableConfig) configObj;
			}
		}

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				if (attempt > 1) {
					log.info("Retry model call, on the {}th attempt (out of {} attempts).", attempt, maxAttempts);
					
					// Update ModelCallLimitHook counters for retry attempts
					// This ensures retries are counted towards the limit
					if (config != null) {
						updateHookCounters(config);
					}
				}

				ModelResponse modelResponse = handler.call(request);
				Message message = (Message) modelResponse.getMessage();

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
					return modelResponse;
				}

				// Successful response
				if (attempt > 1) {
					log.info("The model call succeeded after the {}th attempt.", attempt);
				}
				return modelResponse;

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
	
	/**
	 * Update ModelCallLimitHook counters to account for retry attempts.
	 * This ensures that retries are counted towards the model call limit,
	 * making the Interceptor compatible with ModelCallLimitHook.
	 * 
	 * @param config the RunnableConfig containing Hook counters
	 */
	private void updateHookCounters(RunnableConfig config) {
		try {
			// Increment thread count
			int threadCount = config.context().containsKey(THREAD_COUNT_KEY)
					? (int) config.context().get(THREAD_COUNT_KEY) : 0;
			config.context().put(THREAD_COUNT_KEY, threadCount + 1);
			
			// Increment run count
			int runCount = config.context().containsKey(RUN_COUNT_KEY)
					? (int) config.context().get(RUN_COUNT_KEY) : 0;
			config.context().put(RUN_COUNT_KEY, runCount + 1);
			
			log.debug("Updated Hook counters for retry: threadCount={}, runCount={}", 
					threadCount + 1, runCount + 1);
		} catch (Exception e) {
			// Don't let counter update failures break the retry logic
			log.warn("Failed to update Hook counters: {}", e.getMessage());
		}
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

