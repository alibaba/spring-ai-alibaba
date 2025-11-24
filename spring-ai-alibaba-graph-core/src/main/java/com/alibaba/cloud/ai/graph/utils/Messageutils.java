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
package com.alibaba.cloud.ai.graph.utils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Messageutils {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Generic method to convert various types of messages to Message list
	 * Supported types:
	 * 1. String - converted to UserMessage
	 * 2. Map (e.g. {"role": "user", "text": "test"}) - create corresponding Message based on role
	 * 3. Spring AI Message - use directly
	 * 4. List - recursively process each element in the list
	 */
	public static List<Message> convertToMessages(Object value) {
		List<Message> result = new ArrayList<>();

		if (value == null) {
			return result;
		}

		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.isEmpty()) {
				return result;
			}

			for (Object item : list) {
				result.addAll(convertToMessages(item));
			}
		}
		else if (value instanceof Message) {
			// If it's already a Spring AI Message, add it directly
			result.add((Message) value);
		}
		else if (value instanceof String) {
			// If it's a string, convert to UserMessage
			String text = (String) value;
			if (StringUtils.hasLength(text)) {
				result.add(new UserMessage(text));
			}
		}
		else if (value instanceof Map) {
			// If it's a Map, attempt to parse as Message
			Map<?, ?> map = (Map<?, ?>) value;
			Message message = convertMapToMessage(map);
			if (message != null) {
				result.add(message);
			}
		}
		else {
			// For other types, attempt conversion through JSON serialization/deserialization
			try {
				String json = objectMapper.writeValueAsString(value);
				JsonNode jsonNode = objectMapper.readTree(json);
				if (jsonNode.isObject()) {
					Message message = convertJsonNodeToMessage(jsonNode);
					if (message != null) {
						result.add(message);
					}
				}
			}
			catch (Exception e) {
				// If conversion fails, treat it as a string
				String text = value.toString();
				if (StringUtils.hasLength(text)) {
					result.add(new UserMessage(text));
				}
			}
		}

		return result;
	}

	/**
	 * Convert Map to Message
	 */
	public static Message convertMapToMessage(Map<?, ?> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}

		Object roleObj = map.get("role");
		Object textObj = map.get("text");
		Object contentObj = map.get("content");

		// Prioritize content, then text
		String content = null;
		if (contentObj != null) {
			content = contentObj.toString();
		}
		else if (textObj != null) {
			content = textObj.toString();
		}

		if (!StringUtils.hasLength(content)) {
			return null;
		}

		String role = roleObj != null ? roleObj.toString().toLowerCase() : "user";

		switch (role) {
		case "system":
			return new SystemMessage(content);
		case "assistant":
			return new AssistantMessage(content);
		case "user":
		default:
			return new UserMessage(content);
		}
	}

	/**
	 * Convert JsonNode to Message
	 */
	public static Message convertJsonNodeToMessage(JsonNode jsonNode) {
		if (jsonNode == null || !jsonNode.isObject()) {
			return null;
		}

		JsonNode roleNode = jsonNode.get("role");
		JsonNode textNode = jsonNode.get("text");
		JsonNode contentNode = jsonNode.get("content");

		// Prioritize content, then text
		String content = null;
		if (contentNode != null && contentNode.isTextual()) {
			content = contentNode.asText();
		}
		else if (textNode != null && textNode.isTextual()) {
			content = textNode.asText();
		}

		if (!StringUtils.hasLength(content)) {
			return null;
		}

		String role = roleNode != null && roleNode.isTextual() ? roleNode.asText().toLowerCase() : "user";

		return switch (role) {
			case "system" -> new SystemMessage(content);
			case "assistant" -> new AssistantMessage(content);
			case "user" -> new UserMessage(content);
			default -> new UserMessage(content);
		};
	}
}
