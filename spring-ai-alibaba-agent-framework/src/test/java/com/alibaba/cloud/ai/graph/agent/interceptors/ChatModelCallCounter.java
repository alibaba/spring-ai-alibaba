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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper class that counts the number of times a ChatModel is called.
 * This is useful for testing and monitoring purposes.
 */
public class ChatModelCallCounter implements ChatModel {

	private final ChatModel delegate;
	private final AtomicInteger callCount;
	private final String modelName;

	public ChatModelCallCounter(ChatModel delegate, String modelName) {
		this.delegate = delegate;
		this.modelName = modelName;
		this.callCount = new AtomicInteger(0);
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		callCount.incrementAndGet();
		System.out.println("[" + modelName + "] Call count: " + callCount.get());
		return delegate.call(prompt);
	}

	/**
	 * Get the total number of times this model has been called.
	 * @return the call count
	 */
	public int getCallCount() {
		return callCount.get();
	}

	/**
	 * Reset the call counter to zero.
	 */
	public void resetCallCount() {
		callCount.set(0);
	}

	/**
	 * Get the name of this model.
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Get the underlying delegate ChatModel.
	 * @return the delegate ChatModel
	 */
	public ChatModel getDelegate() {
		return delegate;
	}
}

