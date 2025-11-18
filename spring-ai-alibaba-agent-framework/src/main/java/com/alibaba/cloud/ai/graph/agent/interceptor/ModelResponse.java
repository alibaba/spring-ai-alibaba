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

	public Object getMessage() {
		return message;
	}

	public ChatResponse getChatResponse() {
		return chatResponse;
	}
}

