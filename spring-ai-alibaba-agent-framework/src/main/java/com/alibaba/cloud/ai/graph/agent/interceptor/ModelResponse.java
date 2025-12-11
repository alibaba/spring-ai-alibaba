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
package com.alibaba.cloud.ai.graph.agent.interceptor;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;

/**
 * Response object for model calls.
 * Contains the model's response message.
 */
public class ModelResponse {

	private ChatResponse chatResponse;

	private final Object message;

	public ModelResponse(Object message) {
		this.message = message;
	}

	public ModelResponse(Object message, ChatResponse chatResponse) {
		this.message = message;
		this.chatResponse = chatResponse;
	}

	public static ModelResponse of(AssistantMessage message) {
		return new ModelResponse(message);
	}

	public static ModelResponse of(AssistantMessage message, ChatResponse chatResponse) {
		return new ModelResponse(message, chatResponse);
	}

	public static ModelResponse of(Flux<ChatResponse> flux) {
		return new ModelResponse(flux);
	}

	public static ModelResponse of(GraphFlux<ChatResponse> graphFlux) {
		return new ModelResponse(graphFlux);
	}

	public Object getMessage() {
		return message;
	}

	public ChatResponse getChatResponse() {
		return chatResponse;
	}

	/**
	 * Check if the message is a GraphFlux instance.
	 * 
	 * @return true if message is GraphFlux, false otherwise
	 */
	public boolean isGraphFlux() {
		return message instanceof GraphFlux;
	}

	/**
	 * Check if the message is a plain Flux instance (not GraphFlux).
	 * 
	 * @return true if message is Flux but not GraphFlux, false otherwise
	 */
	public boolean isFlux() {
		return message instanceof Flux && !(message instanceof GraphFlux);
	}

	/**
	 * Check if the message is an AssistantMessage instance.
	 * 
	 * @return true if message is AssistantMessage, false otherwise
	 */
	public boolean isAssistantMessage() {
		return message instanceof AssistantMessage;
	}

	/**
	 * Get the message as GraphFlux if it is one.
	 * 
	 * @return GraphFlux instance
	 * @throws ClassCastException if message is not a GraphFlux
	 */
	@SuppressWarnings("unchecked")
	public GraphFlux<ChatResponse> getAsGraphFlux() {
		if (!isGraphFlux()) {
			throw new IllegalStateException("Message is not a GraphFlux, it is: " +
					(message != null ? message.getClass().getName() : "null"));
		}
		return (GraphFlux<ChatResponse>) message;
	}

	/**
	 * Get the message as Flux if it is one.
	 * 
	 * @return Flux instance
	 * @throws ClassCastException if message is not a Flux
	 */
	@SuppressWarnings("unchecked")
	public Flux<ChatResponse> getAsFlux() {
		if (!(message instanceof Flux)) {
			throw new IllegalStateException("Message is not a Flux, it is: " +
					(message != null ? message.getClass().getName() : "null"));
		}
		return (Flux<ChatResponse>) message;
	}

	/**
	 * Get the message as AssistantMessage if it is one.
	 * 
	 * @return AssistantMessage instance
	 * @throws ClassCastException if message is not an AssistantMessage
	 */
	public AssistantMessage getAsAssistantMessage() {
		if (!isAssistantMessage()) {
			throw new IllegalStateException("Message is not an AssistantMessage, it is: " +
					(message != null ? message.getClass().getName() : "null"));
		}
		return (AssistantMessage) message;
	}
}
