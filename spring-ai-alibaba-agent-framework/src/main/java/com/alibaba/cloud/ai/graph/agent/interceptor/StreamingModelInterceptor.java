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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Streaming model interceptor that provides per-chunk interception for streaming model calls.
 *
 * <p>Unlike {@link ModelInterceptor} which intercepts the entire model call (request/response),
 * this interceptor provides fine-grained control over individual streaming chunks.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@link #beforeStreamCall} - Called once before the streaming call starts, can modify the request</li>
 *   <li>{@link #onStreamChunk} - Called for each {@link ChatResponse} chunk during streaming</li>
 *   <li>{@link #afterStreamComplete} - Called once after all chunks have been received</li>
 *   <li>{@link #onStreamError} - Called if an error occurs during streaming</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * StreamingModelInterceptor interceptor = new StreamingModelInterceptor() {
 *     @Override
 *     public ChatResponse onStreamChunk(ChatResponse chunk, ModelRequest request) {
 *         System.out.println("Chunk: " + chunk.getResult().getOutput().getText());
 *         return chunk;
 *     }
 *
 *     @Override
 *     public void afterStreamComplete(AssistantMessage aggregatedMessage, ModelRequest request) {
 *         System.out.println("Complete: " + aggregatedMessage.getText());
 *     }
 * };
 *
 * ReactAgent agent = ReactAgent.builder()
 *     .name("my-agent")
 *     .model(chatModel)
 *     .streamingInterceptors(interceptor)
 *     .build();
 * }</pre>
 *
 * @author haojunpan
 * @since 1.0.0
 * @see ModelInterceptor
 */
public interface StreamingModelInterceptor {

	/**
	 * Called once before the streaming call starts.
	 * Can be used to modify or enrich the request before it is sent to the model.
	 *
	 * @param request the original model request
	 * @return the (possibly modified) model request
	 */
	default ModelRequest beforeStreamCall(ModelRequest request) {
		return request;
	}

	/**
	 * Called for each {@link ChatResponse} chunk during streaming.
	 * Can be used to inspect, transform, or filter individual chunks.
	 *
	 * @param chunk the current streaming chunk
	 * @param request the original model request (for context)
	 * @return the (possibly modified) chunk; return the original chunk if no modification is needed
	 */
	default ChatResponse onStreamChunk(ChatResponse chunk, ModelRequest request) {
		return chunk;
	}

	/**
	 * Called once after all streaming chunks have been received successfully.
	 * Can be used for logging, metrics collection, or post-processing.
	 *
	 * @param aggregatedMessage the aggregated assistant message from all chunks
	 * @param request the original model request (for context)
	 */
	default void afterStreamComplete(AssistantMessage aggregatedMessage, ModelRequest request) {
	}

	/**
	 * Called if an error occurs during streaming.
	 * Can be used for error logging, alerting, or fallback logic.
	 *
	 * @param error the error that occurred
	 * @param request the original model request (for context)
	 */
	default void onStreamError(Throwable error, ModelRequest request) {
	}

}
