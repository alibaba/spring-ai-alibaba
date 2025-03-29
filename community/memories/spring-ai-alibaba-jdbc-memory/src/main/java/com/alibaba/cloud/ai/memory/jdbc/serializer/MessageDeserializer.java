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
package com.alibaba.cloud.ai.memory.jdbc.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MessageDeserializer extends JsonDeserializer<Message> {

	private static final Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

	public Message deserialize(JsonParser p, DeserializationContext ctxt) {
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		JsonNode node = null;
		Message message = null;
		try {
			node = mapper.readTree(p);
			String messageType = node.get("messageType").asText();
			switch (messageType) {
				case "USER" -> message = new UserMessage(node.get("text").asText(),
						mapper.convertValue(node.get("media"), new TypeReference<Collection<Media>>() {
						}), mapper.convertValue(node.get("metadata"), new TypeReference<Map<String, Object>>() {
						}));
				case "ASSISTANT" -> message = new AssistantMessage(node.get("text").asText(),
						mapper.convertValue(node.get("metadata"), new TypeReference<Map<String, Object>>() {
						}), (List<AssistantMessage.ToolCall>) mapper.convertValue(node.get("toolCalls"),
								new TypeReference<Collection<AssistantMessage.ToolCall>>() {
								}),
						(List<Media>) mapper.convertValue(node.get("media"), new TypeReference<Collection<Media>>() {
						}));
				default -> throw new IllegalArgumentException("Unknown message type: " + messageType);
			}
			;
		}
		catch (IOException e) {
			logger.error("Error deserializing message", e);
		}
		return message;
	}

}
