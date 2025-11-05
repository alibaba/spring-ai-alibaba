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

		objectMapper.registerModule(module);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
	}

	interface ChatMessageDeserializer {

		SystemMessageHandler.Deserializer system = new SystemMessageHandler.Deserializer();

		UserMessageHandler.Deserializer user = new UserMessageHandler.Deserializer();

		AssistantMessageHandler.Deserializer ai = new AssistantMessageHandler.Deserializer();

		ToolResponseMessageHandler.Deserializer tool = new ToolResponseMessageHandler.Deserializer();

		DocumentHandler.Deserializer document = new DocumentHandler.Deserializer();

		AgentInstructionMessageHandler.Deserializer templatedUser = new AgentInstructionMessageHandler.Deserializer();

		static void registerTo(SimpleModule module) {
			module.addDeserializer(ToolResponseMessage.class, tool)
				.addDeserializer(SystemMessage.class, system)
				.addDeserializer(UserMessage.class, user)
				.addDeserializer(AssistantMessage.class, ai)
				.addDeserializer(Document.class, document)
				.addDeserializer(AgentInstructionMessage.class, templatedUser);
		}

	}

	interface ChatMessageSerializer {

		SystemMessageHandler.Serializer system = new SystemMessageHandler.Serializer();

		UserMessageHandler.Serializer user = new UserMessageHandler.Serializer();

		AssistantMessageHandler.Serializer ai = new AssistantMessageHandler.Serializer();

		ToolResponseMessageHandler.Serializer tool = new ToolResponseMessageHandler.Serializer();

		DocumentHandler.Serializer document = new DocumentHandler.Serializer();

		AgentInstructionMessageHandler.Serializer templatedUser = new AgentInstructionMessageHandler.Serializer();

		static void registerTo(SimpleModule module) {
			module.addSerializer(ToolResponseMessage.class, tool)
				.addSerializer(SystemMessage.class, system)
				.addSerializer(UserMessage.class, user)
				.addSerializer(AssistantMessage.class, ai)
				.addSerializer(Document.class, document)
				.addSerializer(AgentInstructionMessage.class, templatedUser);

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
