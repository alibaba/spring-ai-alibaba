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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import com.fasterxml.jackson.databind.module.SimpleModule;

import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import java.io.IOException;

public class SpringAIJacksonStateSerializer extends JacksonStateSerializer {

	public SpringAIJacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);

		var module = new SimpleModule();

		ChatMessageSerializer.registerTo(module);
		ChatMessageDeserializer.registerTo(module);
        NodeOutputDeserializer.registerTo(module);

		typeMapper.register(new TypeMapper.Reference<ToolResponseMessage>(MessageType.TOOL.name()) {
		}).register(new TypeMapper.Reference<SystemMessage>(MessageType.SYSTEM.name()) {
		}).register(new TypeMapper.Reference<UserMessage>(MessageType.USER.name()) {
		}).register(new TypeMapper.Reference<AssistantMessage>(MessageType.ASSISTANT.name()) {
		}).register(new TypeMapper.Reference<Document>("DOCUMENT") {
		}).register(new TypeMapper.Reference<AgentInstructionMessage>("TEMPLATED_USER") {
		});

		// Conditionally register DeepSeekAssistantMessage if the class is available
		registerDeepSeekSupportIfAvailable(module);

		objectMapper.registerModule(module);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
	}

	/**
	 * Conditionally registers DeepSeekAssistantMessage support if the class is available on the classpath.
	 * This avoids forcing a dependency on DeepSeek-related JARs.
	 */
	private void registerDeepSeekSupportIfAvailable(SimpleModule module) {
		try {
			Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
			// Class is available, register the type mapper
			// TypeMapper only needs the type name, not the actual class
			typeMapper.register(new TypeMapper.Reference<Object>("DEEPSEEK_ASSISTANT") {
			});
		}
		catch (ClassNotFoundException e) {
			// DeepSeekAssistantMessage is not available, skip registration
			// This is expected for projects that don't include DeepSeek dependencies
		}
	}

	interface ChatMessageDeserializer {

		SystemMessageHandler.Deserializer system = new SystemMessageHandler.Deserializer();

		UserMessageHandler.Deserializer user = new UserMessageHandler.Deserializer();

		AssistantMessageHandler.Deserializer ai = new AssistantMessageHandler.Deserializer();

		ToolResponseMessageHandler.Deserializer tool = new ToolResponseMessageHandler.Deserializer();

		DocumentHandler.Deserializer document = new DocumentHandler.Deserializer();

		AgentInstructionMessageHandler.Deserializer templatedUser = new AgentInstructionMessageHandler.Deserializer();

		StreamingOutputDeserializer streamingOutput = new StreamingOutputDeserializer();

		static void registerTo(SimpleModule module) {
			module.addDeserializer(ToolResponseMessage.class, tool)
				.addDeserializer(SystemMessage.class, system)
				.addDeserializer(UserMessage.class, user)
				.addDeserializer(AssistantMessage.class, ai)
				.addDeserializer(Document.class, document)
				.addDeserializer(AgentInstructionMessage.class, templatedUser)
				.addDeserializer(StreamingOutput.class, streamingOutput);

			// Conditionally register DeepSeekAssistantMessage deserializer if available
			registerDeepSeekDeserializerIfAvailable(module);
		}

		/**
		 * Conditionally registers DeepSeekAssistantMessage deserializer if the class is available.
		 */
		@SuppressWarnings("unchecked")
		static void registerDeepSeekDeserializerIfAvailable(SimpleModule module) {
			try {
				Class<?> deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
				DeepSeekAssistantMessageHandler.Deserializer deepSeekAi = new DeepSeekAssistantMessageHandler.Deserializer();
				// Use raw type to avoid type inference issues
				module.addDeserializer((Class<Object>) deepSeekClass, (com.fasterxml.jackson.databind.JsonDeserializer<? extends Object>) deepSeekAi);
			}
			catch (ClassNotFoundException | IllegalStateException e) {
				// DeepSeekAssistantMessage is not available, skip registration
				// IllegalStateException may be thrown if the class is found but constructor fails
			}
		}

	}

	interface ChatMessageSerializer {

		SystemMessageHandler.Serializer system = new SystemMessageHandler.Serializer();

		UserMessageHandler.Serializer user = new UserMessageHandler.Serializer();

		AssistantMessageHandler.Serializer ai = new AssistantMessageHandler.Serializer();

		ToolResponseMessageHandler.Serializer tool = new ToolResponseMessageHandler.Serializer();

		DocumentHandler.Serializer document = new DocumentHandler.Serializer();

		AgentInstructionMessageHandler.Serializer templatedUser = new AgentInstructionMessageHandler.Serializer();

		JacksonNodeOutputSerializer output = new JacksonNodeOutputSerializer();

		StreamingOutputSerializer streamingOutput = new StreamingOutputSerializer();

		static void registerTo(SimpleModule module) {
			module.addSerializer(ToolResponseMessage.class, tool)
				.addSerializer(SystemMessage.class, system)
				.addSerializer(UserMessage.class, user)
				.addSerializer(AssistantMessage.class, ai)
				.addSerializer(Document.class, document)
				.addSerializer(AgentInstructionMessage.class, templatedUser)
				.addSerializer(NodeOutput.class, output)
				.addSerializer(StreamingOutput.class, streamingOutput);

			// Conditionally register DeepSeekAssistantMessage serializer if available
			registerDeepSeekSerializerIfAvailable(module);
		}

		/**
		 * Conditionally registers DeepSeekAssistantMessage serializer if the class is available.
		 */
		@SuppressWarnings("unchecked")
		static void registerDeepSeekSerializerIfAvailable(SimpleModule module) {
			try {
				Class<?> deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
				DeepSeekAssistantMessageHandler.Serializer deepSeekAi = new DeepSeekAssistantMessageHandler.Serializer();
				// Use raw type to avoid type inference issues
				module.addSerializer((Class<Object>) deepSeekClass, (com.fasterxml.jackson.databind.JsonSerializer<Object>) deepSeekAi);
			}
			catch (ClassNotFoundException | IllegalStateException e) {
				// DeepSeekAssistantMessage is not available, skip registration
				// IllegalStateException may be thrown if the class is found but constructor fails
			}
		}

	}

    interface NodeOutputDeserializer {

        JacksonNodeOutputDeserializer nodeOutput = new JacksonNodeOutputDeserializer();

        static void registerTo(SimpleModule module) {
            module.addDeserializer(NodeOutput.class, nodeOutput);
        }
    }

    @Override
    public OverAllState cloneObject(OverAllState object) throws IOException {
        return bytesToObject(objectToBytes(object), object.getClass());
    }

    @Override
    public byte[] objectToBytes(OverAllState object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    private OverAllState bytesToObject(byte[] bytes, Class<? extends OverAllState> clz) throws IOException {
        return objectMapper.readValue(bytes, clz);
    }

}
