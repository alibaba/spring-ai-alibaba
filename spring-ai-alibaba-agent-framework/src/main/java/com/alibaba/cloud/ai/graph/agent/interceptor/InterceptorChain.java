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
package com.alibaba.cloud.ai.graph.agent.interceptor;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

/**
 * Utility class for chaining model and tool interceptors.
 *
 * This implements the Chain of Responsibility pattern.
 * Interceptors are composed so that the first in the list becomes the outermost layer.
 */
public class InterceptorChain {

	/**
	 * Chain multiple ModelInterceptors into a single handler.
	 *
	 * The first interceptor wraps all others, creating a nested structure:
	 * interceptors[0] -> interceptors[1] -> ... -> base handler
	 *
	 * @param interceptors List of ModelInterceptors to chain
	 * @param baseHandler The base handler that executes the actual model call
	 * @return A composed handler, or the base handler if no interceptors
	 */
	public static ModelCallHandler chainModelInterceptors(
			List<ModelInterceptor> interceptors,
			ModelCallHandler baseHandler) {

		if (interceptors == null || interceptors.isEmpty()) {
			return baseHandler;
		}

		// Start with the base handler
		ModelCallHandler current = baseHandler;

		// Wrap from last to first (right-to-left composition)
		// This ensures first interceptor is outermost
		for (int i = interceptors.size() - 1; i >= 0; i--) {
			ModelInterceptor interceptor = interceptors.get(i);
			ModelCallHandler nextHandler = current;

			// Create a wrapper that calls the interceptor's wrap method
			current = request -> interceptor.interceptModel(request, nextHandler);
		}

		return current;
	}

	/**
	 * Chain multiple ToolInterceptors into a single handler.
	 *
	 * The first interceptor wraps all others, creating a nested structure:
	 * interceptors[0] -> interceptors[1] -> ... -> base handler
	 *
	 * @param interceptors List of ToolInterceptors to chain
	 * @param baseHandler The base handler that executes the actual tool call
	 * @return A composed handler, or the base handler if no interceptors
	 */
	public static ToolCallHandler chainToolInterceptors(
			List<ToolInterceptor> interceptors,
			ToolCallHandler baseHandler) {

		if (interceptors == null || interceptors.isEmpty()) {
			return baseHandler;
		}

		// Start with the base handler
		ToolCallHandler current = baseHandler;

		// Wrap from last to first (right-to-left composition)
		// This ensures first interceptor is outermost
		for (int i = interceptors.size() - 1; i >= 0; i--) {
			ToolInterceptor interceptor = interceptors.get(i);
			ToolCallHandler nextHandler = current;

			// Create a wrapper that calls the interceptor's wrap method
			current = request -> interceptor.interceptToolCall(request, nextHandler);
		}

		return current;
	}

	/**
	 * Apply streaming interceptors to a {@link Flux} of {@link ChatResponse}.
	 *
	 * <p>Each interceptor is applied in order (first to last). For each interceptor:
	 * <ol>
	 *   <li>{@link StreamingModelInterceptor#beforeStreamCall} is called once before subscription</li>
	 *   <li>{@link StreamingModelInterceptor#onStreamChunk} is called for each chunk</li>
	 *   <li>{@link StreamingModelInterceptor#afterStreamComplete} is called when the stream completes</li>
	 *   <li>{@link StreamingModelInterceptor#onStreamError} is called if an error occurs</li>
	 * </ol>
	 *
	 * @param interceptors List of StreamingModelInterceptors to apply
	 * @param flux The original Flux of ChatResponse chunks
	 * @param request The model request (for context)
	 * @return The transformed Flux with all interceptors applied
	 */
	public static Flux<ChatResponse> applyStreamingInterceptors(
			List<StreamingModelInterceptor> interceptors,
			Flux<ChatResponse> flux,
			ModelRequest request) {

		if (interceptors == null || interceptors.isEmpty()) {
			return flux;
		}

		// Apply beforeStreamCall for all interceptors (first to last)
		ModelRequest currentRequest = request;
		for (StreamingModelInterceptor interceptor : interceptors) {
			currentRequest = interceptor.beforeStreamCall(currentRequest);
		}

		// Apply onStreamChunk, afterStreamComplete, and onStreamError for each interceptor
		final ModelRequest finalRequest = currentRequest;
		Flux<ChatResponse> current = flux;

		for (StreamingModelInterceptor interceptor : interceptors) {
			// Aggregate text from all chunks so afterStreamComplete sees the full message,
			// not just the final delta chunk's text.
			final StringBuilder aggregator = new StringBuilder();

			current = current
					.map(chunk -> {
						ChatResponse transformed = interceptor.onStreamChunk(chunk, finalRequest);
						if (transformed != null && transformed.getResult() != null
								&& transformed.getResult().getOutput() != null
								&& transformed.getResult().getOutput().getText() != null) {
							aggregator.append(transformed.getResult().getOutput().getText());
						}
						return transformed;
					})
					.doOnComplete(() -> interceptor.afterStreamComplete(
							new AssistantMessage(aggregator.toString()), finalRequest))
					.doOnError(error -> interceptor.onStreamError(error, finalRequest));
		}

		return current;
	}

	/**
	 * Example of how interceptors are chained:
	 *
	 * Given interceptors [auth, retry, cache] and baseHandler:
	 *
	 * 1. Start: current = baseHandler
	 * 2. Wrap with cache: current = req -> cache.wrap(req, baseHandler)
	 * 3. Wrap with retry: current = req -> retry.wrap(req, cache.wrap(...))
	 * 4. Wrap with auth: current = req -> auth.wrap(req, retry.wrap(...))
	 *
	 * Final call flow:
	 * request -> auth -> retry -> cache -> baseHandler
	 *
	 * Response flow:
	 * baseHandler -> cache -> retry -> auth -> response
	 */
}
