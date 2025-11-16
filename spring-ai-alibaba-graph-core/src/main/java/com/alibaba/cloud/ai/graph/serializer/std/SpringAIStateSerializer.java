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
package com.alibaba.cloud.ai.graph.serializer.std;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * This class is responsible for serializing and deserializing the state of an agent
 * executor. It extends {@link ObjectStreamStateSerializer} for handling the serialization
 * of the AgentExecutor.State object.
 */
public class SpringAIStateSerializer extends ObjectStreamStateSerializer {

	public SpringAIStateSerializer() {
		this(OverAllState::new);
	}

	/**
	 * Constructor that initializes the serializer with a supplier for creating new
	 * AgentExecutor.State instances and registers various serializers for different
	 * types.
	 */
	public SpringAIStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);

		mapper().register(Message.class, new MessageSerializer());
		mapper().register(AssistantMessage.ToolCall.class, new ToolCallSerializer());
		mapper().register(ToolResponseMessage.ToolResponse.class, new ToolResponseSerializer());

		// Conditionally register DeepSeekAssistantMessage serializer if available
		registerDeepSeekSupportIfAvailable();
	}

	/**
	 * Conditionally registers DeepSeekAssistantMessage support if the class is available on the classpath.
	 * This avoids forcing a dependency on DeepSeek-related JARs.
	 */
	private void registerDeepSeekSupportIfAvailable() {
		try {
			Class<?> deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
			DeepSeekAssistantMessageSerializer serializer = new DeepSeekAssistantMessageSerializer();
			mapper().register(deepSeekClass, serializer);
		}
		catch (ClassNotFoundException | IllegalStateException e) {
			// DeepSeekAssistantMessage is not available, skip registration
			// This is expected for projects that don't include DeepSeek dependencies
			// IllegalStateException may be thrown if the class is found but constructor fails
		}
	}

}
