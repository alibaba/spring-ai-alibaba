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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class SpringAIJacksonStateSerializer extends JacksonStateSerializer {

	public SpringAIJacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);

		var module = new SimpleModule();

		ChatMessageSerializer.registerTo(module);
		ChatMessageDeserializer.registerTo(module);

		typeMapper.register(new TypeMapper.Reference<ToolResponseMessage>(MessageType.TOOL.name()) {
		}).register(new TypeMapper.Reference<SystemMessage>(MessageType.SYSTEM.name()) {
		}).register(new TypeMapper.Reference<UserMessage>(MessageType.USER.name()) {
		}).register(new TypeMapper.Reference<AssistantMessage>(MessageType.ASSISTANT.name()) {
		}).register(new TypeMapper.Reference<Document>("DOCUMENT") {
		});

		objectMapper.registerModule(module);
	}

	interface ChatMessageDeserializer {

		SystemMessageHandler.Deserializer system = new SystemMessageHandler.Deserializer();

		UserMessageHandler.Deserializer user = new UserMessageHandler.Deserializer();

		AssistantMessageHandler.Deserializer ai = new AssistantMessageHandler.Deserializer();

		ToolResponseMessageHandler.Deserializer tool = new ToolResponseMessageHandler.Deserializer();

		DocumentHandler.Deserializer document = new DocumentHandler.Deserializer();

		static void registerTo(SimpleModule module) {
			module.addDeserializer(ToolResponseMessage.class, tool)
				.addDeserializer(SystemMessage.class, system)
				.addDeserializer(UserMessage.class, user)
				.addDeserializer(AssistantMessage.class, ai)
				.addDeserializer(Document.class, document);
		}

	}

	interface ChatMessageSerializer {

		SystemMessageHandler.Serializer system = new SystemMessageHandler.Serializer();

		UserMessageHandler.Serializer user = new UserMessageHandler.Serializer();

		AssistantMessageHandler.Serializer ai = new AssistantMessageHandler.Serializer();

		ToolResponseMessageHandler.Serializer tool = new ToolResponseMessageHandler.Serializer();

		DocumentHandler.Serializer document = new DocumentHandler.Serializer();

		static void registerTo(SimpleModule module) {
			module.addSerializer(ToolResponseMessage.class, tool)
				.addSerializer(SystemMessage.class, system)
				.addSerializer(UserMessage.class, user)
				.addSerializer(AssistantMessage.class, ai)
				.addSerializer(Document.class, document);

		}

	}

}
