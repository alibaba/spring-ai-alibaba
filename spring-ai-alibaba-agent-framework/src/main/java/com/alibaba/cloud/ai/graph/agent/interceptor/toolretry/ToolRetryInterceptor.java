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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolretry;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool interceptor that automatically retries failed tool calls with configurable backoff.
 *
 * Supports retrying on specific exceptions and exponential backoff.
 *
 * Example:
 * ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder()
 *     .maxRetries(3)
 *     .backoffFactor(2.0)
 *     .initialDelay(1000)
 *     .build();
 */
public class ToolRetryInterceptor extends ToolInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolRetryInterceptor.class);

	private final int maxRetries;
	private final Set<String> toolNames;
	private final Predicate<Exception> retryOn;
	private final OnFailureBehavior onFailure;
	private final Function<Exception, String> errorFormatter;
	private final double backoffFactor;
	private final long initialDelayMs;
	private final long maxDelayMs;
	private final boolean jitter;

	private ToolRetryInterceptor(Builder builder) {
		this.maxRetries = builder.maxRetries;
		this.toolNames = builder.toolNames != null ? new HashSet<>(builder.toolNames) : null;
		this.retryOn = builder.retryOn;
		this.onFailure = builder.onFailure;
		this.errorFormatter = builder.errorFormatter;
		this.backoffFactor = builder.backoffFactor;
		this.initialDelayMs = builder.initialDelayMs;
		this.maxDelayMs = builder.maxDelayMs;
		this.jitter = builder.jitter;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		String toolName = request.getToolName();

		// Check if this tool should be retried
		if (toolNames != null && !toolNames.contains(toolName)) {
			return handler.call(request);
		}

		Exception lastException = null;
		int attempt = 0;

		while (attempt <= maxRetries) {
			try {
				return handler.call(request);
			}
			catch (Exception e) {
				lastException = e;

				// Check if we should retry this exception
				if (!retryOn.test(e)) {
					log.debug("Exception {} not configured for retry, re-throwing", e.getClass().getSimpleName());
					throw e;
				}

				if (attempt == maxRetries) {
					// Max retries reached
					break;
				}

				// Calculate delay
				long delay = calculateDelay(attempt);
				log.warn("Tool '{}' failed (attempt {}/{}), retrying in {}ms: {}",
						toolName, attempt + 1, maxRetries + 1, delay, e.getMessage());

				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Retry interrupted", ie);
				}

				attempt++;
			}
		}

		// All retries exhausted
		if (onFailure == OnFailureBehavior.RAISE) {
			throw new RuntimeException("Tool call failed after " + (maxRetries + 1) + " attempts", lastException);
		}
		else {
			// Return error message as tool response
			String errorMessage = errorFormatter != null
					? errorFormatter.apply(lastException)
					: "Tool call failed after " + (maxRetries + 1) + " attempts: " + lastException.getMessage();

			log.error("Tool '{}' failed after {} attempts: {}", toolName, maxRetries + 1, lastException.getMessage());
			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), errorMessage);
		}
	}

	private long calculateDelay(int retryNumber) {
		long delay = (long) (initialDelayMs * Math.pow(backoffFactor, retryNumber));
		delay = Math.min(delay, maxDelayMs);

		if (jitter) {
			// Add random jitter ±25%
			double jitterFactor = 0.75 + (Math.random() * 0.5);
			delay = (long) (delay * jitterFactor);
		}

		return delay;
	}

	@Override
	public String getName() {
		return "ToolRetry";
	}

	public enum OnFailureBehavior {
		RAISE,
		RETURN_MESSAGE
	}

	public static class Builder {
		private int maxRetries = 2;
		private Set<String> toolNames;
		private Predicate<Exception> retryOn = e -> true; // Retry on all exceptions by default
		private OnFailureBehavior onFailure = OnFailureBehavior.RETURN_MESSAGE;
		private Function<Exception, String> errorFormatter;
		private double backoffFactor = 2.0;
		private long initialDelayMs = 1000;
		private long maxDelayMs = 60000;
		private boolean jitter = true;

		public Builder maxRetries(int maxRetries) {
			if (maxRetries < 0) {
				throw new IllegalArgumentException("maxRetries must be >= 0");
			}
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder toolNames(Set<String> toolNames) {
			this.toolNames = toolNames;
			return this;
		}

		public Builder toolName(String toolName) {
			if (this.toolNames == null) {
				this.toolNames = new HashSet<>();
			}
			this.toolNames.add(toolName);
			return this;
		}

		@SafeVarargs
		public final Builder retryOn(Class<? extends Exception>... exceptionTypes) {
			Set<Class<? extends Exception>> types = new HashSet<>(Arrays.asList(exceptionTypes));
			this.retryOn = e -> {
				for (Class<? extends Exception> type : types) {
					if (type.isInstance(e)) {
						return true;
					}
				}
				return false;
			};
			return this;
		}

		public Builder retryOn(Predicate<Exception> predicate) {
			this.retryOn = predicate;
			return this;
		}

		public Builder onFailure(OnFailureBehavior behavior) {
			this.onFailure = behavior;
			return this;
		}

		public Builder errorFormatter(Function<Exception, String> formatter) {
			this.errorFormatter = formatter;
			this.onFailure = OnFailureBehavior.RETURN_MESSAGE;
			return this;
		}

		public Builder backoffFactor(double backoffFactor) {
			this.backoffFactor = backoffFactor;
			return this;
		}

		public Builder initialDelay(long initialDelayMs) {
			this.initialDelayMs = initialDelayMs;
			return this;
		}

		public Builder maxDelay(long maxDelayMs) {
			this.maxDelayMs = maxDelayMs;
			return this;
		}

		public Builder jitter(boolean jitter) {
			this.jitter = jitter;
			return this;
		}

		public ToolRetryInterceptor build() {
			return new ToolRetryInterceptor(this);
		}
	}
}

