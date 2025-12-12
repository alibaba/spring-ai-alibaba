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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.document.Document;

import com.fasterxml.jackson.databind.module.SimpleModule;

import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import java.util.Collection;
import java.util.Map;

public class SpringAIJacksonStateSerializer extends JacksonStateSerializer {

	public SpringAIJacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new ObjectMapper());
	}

	public SpringAIJacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory, ObjectMapper objectMapper) {
		super(stateFactory, objectMapper);

		var module = new SimpleModule();

		registerMessageHandlers(module);

		typeMapper.register(new TypeMapper.Reference<ToolResponseMessage>(MessageType.TOOL.name()) {
		}).register(new TypeMapper.Reference<SystemMessage>(MessageType.SYSTEM.name()) {
		}).register(new TypeMapper.Reference<UserMessage>(MessageType.USER.name()) {
		}).register(new TypeMapper.Reference<AssistantMessage>(MessageType.ASSISTANT.name()) {
		}).register(new TypeMapper.Reference<Document>("DOCUMENT") {
		}).register(new TypeMapper.Reference<AgentInstructionMessage>("TEMPLATED_USER") {
		}).register(new TypeMapper.Reference<DeepSeekAssistantMessage>("DEEPSEEK_ASSISTANT") {
		});

		objectMapper.registerModule(module);

		ObjectMapper.DefaultTypeResolverBuilder typeResolver = new ObjectMapper.DefaultTypeResolverBuilder(
				ObjectMapper.DefaultTyping.NON_FINAL, LaissezFaireSubTypeValidator.instance) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean useForType(JavaType t) {
				if (t.isTypeOrSubTypeOf(Map.class) || t.isMapLikeType() || t.isCollectionLikeType()
						|| t.isTypeOrSubTypeOf(Collection.class) || t.isArrayType()) {
					return false;
				}

				// Skip non-static inner classes, local classes, and anonymous classes
				// They cannot be deserialized by Jackson because they require an outer class instance
				Class<?> rawClass = t.getRawClass();
				if (rawClass != null) {
					// Non-static inner class
					if (rawClass.isMemberClass() && !java.lang.reflect.Modifier.isStatic(rawClass.getModifiers())) {
						return false;
					}
					// Local class or anonymous class
					if (rawClass.isLocalClass() || rawClass.isAnonymousClass()) {
						return false;
					}
				}

				return super.useForType(t);
			}
		};
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.init(JsonTypeInfo.Id.CLASS, null);
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.inclusion(JsonTypeInfo.As.PROPERTY);
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.typeProperty("@class");
		objectMapper.setDefaultTyping(typeResolver);
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
					.addDeserializer(StreamingOutput.class, streamingOutput)
					.addDeserializer(DeepSeekAssistantMessage.class, new DeepSeekAssistantMessageHandler.Deserializer());
		}

	}

	/**
	 * Registers all Spring AI Message handlers (serializers and deserializers) to
	 * the provided Jackson module.
	 * This allows other components (like CheckpointSavers) to reuse the same
	 * serialization logic.
	 *
	 * @param module the Jackson SimpleModule to register handlers to
	 */
	public static void registerMessageHandlers(SimpleModule module) {
		ChatMessageSerializer.registerTo(module);
		ChatMessageDeserializer.registerTo(module);
		NodeOutputDeserializer.registerTo(module);
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
					.addSerializer(StreamingOutput.class, streamingOutput)
					.addSerializer(DeepSeekAssistantMessage.class, new DeepSeekAssistantMessageHandler.Serializer());
		}

	}

	interface NodeOutputDeserializer {

		JacksonNodeOutputDeserializer nodeOutput = new JacksonNodeOutputDeserializer();

		static void registerTo(SimpleModule module) {
			module.addDeserializer(NodeOutput.class, nodeOutput);
		}
	}

}
