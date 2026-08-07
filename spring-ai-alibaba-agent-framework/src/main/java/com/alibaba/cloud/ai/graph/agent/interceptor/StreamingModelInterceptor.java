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
 * <p>Lifecycle (per subscription — see
 * {@link InterceptorChain#applyStreamingInterceptors}):</p>
 * <ol>
 *   <li>{@link #beforeStreamCall} - Called once before the chunk Flux is subscribed.
 *       The returned {@link ModelRequest} is threaded into <i>subsequent callbacks</i>
 *       of this and later interceptors as the {@code request} parameter; it does
 *       <b>not</b> alter the actual outbound model call (the Flux has already been
 *       constructed). To modify the outbound request use {@link ModelInterceptor}.</li>
 *   <li>{@link #onStreamChunk} - Called for each {@link ChatResponse} chunk; can
 *       transform, observe, or drop ({@code return null}) the chunk</li>
 *   <li>{@link #afterStreamComplete} - Called once after all chunks have been received,
 *       with the aggregated {@link AssistantMessage} built from emitted chunk text</li>
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
	 * Called once per subscription, before the chunk Flux is subscribed to.
	 *
	 * <p>The returned {@link ModelRequest} is propagated as the {@code request} parameter
	 * to this and later interceptors' {@link #onStreamChunk}, {@link #afterStreamComplete},
	 * and {@link #onStreamError} callbacks. Use this hook to attach context (correlation
	 * IDs, trace metadata, computed state) that downstream callbacks need to read.
	 *
	 * <p><b>Important:</b> the returned request does <i>not</i> alter the outbound model
	 * call — by the time this method runs, the chunk Flux has already been built from the
	 * original request. To modify what is sent to the model (messages, options, tools),
	 * use {@link ModelInterceptor} instead.
	 *
	 * @param request the model request as threaded through earlier interceptors'
	 *                {@code beforeStreamCall} (or the original request for the first
	 *                interceptor in the chain)
	 * @return the (possibly enriched) model request to pass to subsequent callbacks
	 */
	default ModelRequest beforeStreamCall(ModelRequest request) {
		return request;
	}

	/**
	 * Called for each {@link ChatResponse} chunk during streaming.
	 * Can be used to inspect, transform, or filter individual chunks.
	 *
	 * @param chunk the current streaming chunk
	 * @param request the model request as it stands after every interceptor's
	 *                {@link #beforeStreamCall} has run (i.e. the fully-threaded request,
	 *                not necessarily the original one passed to the model)
	 * @return the (possibly modified) chunk to keep emitting, or {@code null} to drop this
	 *         chunk from the stream entirely. When dropped, downstream interceptors and
	 *         subscribers will not see this chunk, and its text will not be included in
	 *         the aggregated message passed to {@link #afterStreamComplete}.
	 */
	default ChatResponse onStreamChunk(ChatResponse chunk, ModelRequest request) {
		return chunk;
	}

	/**
	 * Called once after all streaming chunks have been received successfully.
	 * Can be used for logging, metrics collection, or post-processing.
	 *
	 * @param aggregatedMessage an {@link AssistantMessage} built by concatenating the
	 *                          {@code text} of every chunk this interceptor emitted
	 *                          downstream (chunks dropped via {@link #onStreamChunk}
	 *                          returning {@code null} are excluded)
	 * @param request the model request as it stands after every interceptor's
	 *                {@link #beforeStreamCall} has run
	 */
	default void afterStreamComplete(AssistantMessage aggregatedMessage, ModelRequest request) {
	}

	/**
	 * Called if an error occurs during streaming.
	 * Can be used for error logging, alerting, or fallback logic.
	 *
	 * @param error the error that occurred
	 * @param request the model request as it stands after every interceptor's
	 *                {@link #beforeStreamCall} has run
	 */
	default void onStreamError(Throwable error, ModelRequest request) {
	}

}
