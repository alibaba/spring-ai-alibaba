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
package com.alibaba.cloud.ai.example.deepresearch.serializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.*;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom JSON deserializer for Message objects
 */
public class MessageDeserializer extends JsonDeserializer<Message> {

	private static final Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

	private static final Map<String, MessageFactory> MESSAGE_FACTORIES = new ConcurrentHashMap<>();

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
		.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
		.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
		.build();

	static {
		MESSAGE_FACTORIES.put("USER",
				((textContent, metadata, toolCalls, toolResponses) -> UserMessage.builder()
					.text(textContent)
					.metadata(CollectionUtils.isEmpty(metadata) ? Map.of() : metadata)
					.build()));
		MESSAGE_FACTORIES.put("ASSISTANT",
				((textContent, metadata, toolCalls, toolResponses) -> new AssistantMessage(textContent,
						CollectionUtils.isEmpty(metadata) ? Map.of() : metadata,
						CollectionUtils.isEmpty(toolCalls) ? List.of() : toolCalls)));
		MESSAGE_FACTORIES.put("SYSTEM",
				((textContent, metadata, toolCalls, toolResponses) -> SystemMessage.builder()
					.text(textContent)
					.metadata(CollectionUtils.isEmpty(metadata) ? Map.of() : metadata)
					.build()));
		MESSAGE_FACTORIES.put("TOOL",
				((textContent, metadata, toolCalls, toolResponses) -> new ToolResponseMessage(toolResponses,
						CollectionUtils.isEmpty(metadata) ? Map.of() : metadata)));
	}

	@Override
	public Message deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		JsonNode node = p.getCodec().readTree(p);

		logger.debug("Deserializing message: {}", node);

		// If node is plain text, create a UserMessage by default
		if (node.isTextual()) {
			return new UserMessage(node.asText());
		}

		// Extract message type
		String type = extractMessageType(node);

		// Extract content
		String content = extractContent(node);

		// Extract metadata
		Map<String, Object> metadata = extractMetadata(node);

		// Extract tool calls for AssistantMessage
		List<AssistantMessage.ToolCall> toolCalls = extractToolCalls(node);

		// Extract tool responses for ToolResponseMessage
		List<ToolResponseMessage.ToolResponse> toolResponses = extractToolResponses(node);

		// Create corresponding message object based on type
		return Optional.ofNullable(type).map(String::toUpperCase).map(MESSAGE_FACTORIES::get).orElseGet(() -> {
			if (type == null) {
				logger.warn("Message type not found, defaulting to USER");
			}
			else {
				logger.warn("Unknown message type: {}, defaulting to USER", type);
			}
			return MESSAGE_FACTORIES.get("USER");
		}).create(content, metadata, toolCalls, toolResponses);
	}

	/**
	 * Extract message type from JsonNode
	 */
	private String extractMessageType(JsonNode node) {
		return Optional.ofNullable(node.get("messageType"))
			.map(JsonNode::asText)
			.orElseGet(() -> Optional.ofNullable(node.get("type"))
				.map(JsonNode::asText)
				.orElseGet(
						() -> Optional.ofNullable(node.get("role")).map(n -> n.asText().toUpperCase()).orElse(null)));
	}

	/**
	 * Extract message content from JsonNode
	 */
	private String extractContent(JsonNode node) {
		return Optional.ofNullable(node.get("content"))
			.map(JsonNode::asText)
			.orElseGet(
					() -> Optional.ofNullable(node.get("textContent")).map(JsonNode::asText).orElseGet(node::toString));
	}

	/**
	 * Extract metadata from JsonNode
	 */
	private Map<String, Object> extractMetadata(JsonNode node) {
		return Optional.ofNullable(node.get("metadata")).map(metadataNode -> {
			try {
				return OBJECT_MAPPER.convertValue(metadataNode, new TypeReference<>() {
				});
			}
			catch (IllegalArgumentException e) {
				logger.warn("Failed to deserialize metadata: {}", e.getMessage());
				return Collections.<String, Object>emptyMap();
			}
		}).orElseGet(Collections::emptyMap);
	}

	/**
	 * Extract tool calls from JsonNode
	 */
	private List<AssistantMessage.ToolCall> extractToolCalls(JsonNode node) {
		return Optional.ofNullable(node.get("toolCalls")).filter(JsonNode::isArray).map(toolCallNode -> {
			try {
				return OBJECT_MAPPER.convertValue(toolCallNode, new TypeReference<>() {
				});
			}
			catch (IllegalArgumentException e) {
				logger.warn("Failed to deserialize toolCalls: {}", e.getMessage());
				return Collections.<AssistantMessage.ToolCall>emptyList();
			}
		}).orElseGet(Collections::emptyList);
	}

	/**
	 * Extract tool responses from JsonNode
	 */
	private List<ToolResponseMessage.ToolResponse> extractToolResponses(JsonNode node) {
		return Optional.ofNullable(node.get("responses")).filter(JsonNode::isArray).map(toolResponsesNode -> {
			try {
				return OBJECT_MAPPER.convertValue(toolResponsesNode, new TypeReference<>() {
				});
			}
			catch (IllegalArgumentException e) {
				logger.warn("Failed to deserialize toolResponses: {}", e.getMessage());
				return Collections.<ToolResponseMessage.ToolResponse>emptyList();
			}
		}).orElseGet(Collections::emptyList);
	}

}
